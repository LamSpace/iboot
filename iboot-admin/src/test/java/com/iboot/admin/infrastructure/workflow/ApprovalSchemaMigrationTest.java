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

package com.iboot.admin.infrastructure.workflow;

import org.flowable.engine.ProcessEngine;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批流数据库迁移与引擎启动的集成测试。
 *
 * <p>在空库上执行 Flyway V1–V17，随后以 {@code database-schema-update=false} 启动
 * Flowable 引擎，验证建表脚本完整、业务表与菜单种子存在，且引擎不会再次执行 DDL。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = ApprovalSchemaMigrationTest.MigrationConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "flowable.database-schema-update=false",
                "flowable.async-executor-activate=false",
                "flowable.idm.enabled=false",
                "flowable.eventregistry.enabled=false",
                "flowable.check-process-definitions=false",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,"
                        + "de.codecentric.boot.admin.server.config.AdminServerNotifierAutoConfiguration"
        })
@Testcontainers
class ApprovalSchemaMigrationTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_approval")
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 验证迁移后 Flowable 表、业务表与菜单数据完整。
     */
    @Test
    void migrationCreatesWorkflowSchema() {
        assertThat(countTables("ACT\\_%")).isEqualTo(30);
        assertThat(countTables("FLW\\_RU\\_BATCH%")).isEqualTo(2);
        assertThat(countTables("biz_leave_application")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history", Integer.class)).isGreaterThanOrEqualTo(17);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_menu WHERE id BETWEEN 9100 AND 9104", Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 9100 AND 9104",
                Integer.class)).isEqualTo(5);
    }

    /**
     * 验证引擎在关闭自动建表后仍能启动，且没有新增表。
     */
    @Test
    void engineStartsWithoutCreatingTables() {
        assertThat(context.getBeansOfType(ProcessEngine.class)).hasSize(1);
        assertThat(countTables("ACT\\_%")).isEqualTo(30);
        assertThat(countTables("FLW\\_RU\\_BATCH%")).isEqualTo(2);
    }

    private int countTables(String pattern) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name LIKE ?",
                Integer.class, pattern);
        return count == null ? 0 : count;
    }

    /**
     * 迁移测试配置：数据源创建时先执行 Flyway，确保引擎启动前表结构已就绪。
     */
    @Configuration
    @EnableAutoConfiguration
    static class MigrationConfig {

        /**
         * 执行 Flyway 迁移并返回数据源。
         *
         * @return 数据源
         */
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                    + "/iboot_approval?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            return dataSource;
        }
    }
}
