/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.system;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 知识图谱权限迁移测试。
 *
 * <p>验证 V27 在空库上创建 {@code ai:graph:use} 权限点并授权超级管理员，
 * 且同一 SQL 重复执行保持幂等。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class GraphMenuMigrationTest {

    private static final int MYSQL_PORT = 3306;

    private static final long GRAPH_MENU_ID = 9806L;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_graph_menu")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_graph_menu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        migrateWithRetry(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void createsPermissionPointAndGrantsSuperAdmin() {
        assertThat(countMenus()).isEqualTo(1);
        assertThat(countGrants()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT permission FROM sys_menu WHERE id = ?", String.class, GRAPH_MENU_ID))
                .isEqualTo("ai:graph:use");
    }

    @Test
    void reexecutingMigrationIsIdempotent() throws Exception {
        executeMigrationAgain();

        assertThat(countMenus()).isEqualTo(1);
        assertThat(countGrants()).isEqualTo(1);
    }

    private int countMenus() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE id = ?", Integer.class, GRAPH_MENU_ID);
    }

    private int countGrants() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id = ?",
                Integer.class, GRAPH_MENU_ID);
    }

    private void executeMigrationAgain() throws Exception {
        String sql;
        try (InputStream input = new ClassPathResource("db/migration/V27__add_graph_permission.sql")
                .getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String withoutComments = Arrays.stream(sql.split("\n"))
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        for (String statement : withoutComments.split(";")) {
            if (!statement.isBlank()) {
                jdbcTemplate.execute(statement);
            }
        }
    }

    private void migrateWithRetry(DriverManagerDataSource dataSource) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
                return;
            } catch (RuntimeException e) {
                last = e;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }
}
