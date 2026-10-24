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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.backup;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据备份迁移脚本的集成测试。
 *
 * <p>在真实 MySQL 8.0.45 上分别验证全新库迁移与 V20 到 V21 的增量迁移，
 * 断言备份表、定时任务种子与管理端菜单权限均已就绪。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers
class BackupMigrationTest {

    private static final int MYSQL_PORT = 3306;

    private static final String FRESH_DATABASE = "iboot_backup_fresh";

    private static final String UPGRADE_DATABASE = "iboot_backup_upgrade";

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", FRESH_DATABASE)
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    /**
     * 验证全新库执行 V1–V21 后备份相关表和种子数据完整。
     */
    @Test
    void freshMigrationCreatesBackupSchema() throws SQLException {
        migrate(FRESH_DATABASE, null);

        assertThat(tableExists(FRESH_DATABASE, "db_backup_record")).isTrue();
        assertThat(tableExists(FRESH_DATABASE, "db_restore_record")).isTrue();
        assertThat(count(FRESH_DATABASE, "SELECT COUNT(*) FROM flyway_schema_history")).isGreaterThanOrEqualTo(21);
        assertThat(count(FRESH_DATABASE,
                "SELECT COUNT(*) FROM sys_job WHERE invoke_target = 'backupTask.run' AND deleted = 0")).isEqualTo(1);
        assertThat(count(FRESH_DATABASE,
                "SELECT COUNT(*) FROM sys_menu WHERE id BETWEEN 9300 AND 9302")).isEqualTo(3);
        assertThat(count(FRESH_DATABASE,
                "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 9300 AND 9302"))
                .isEqualTo(3);
    }

    /**
     * 验证已有 V20 库可增量升级到 V21，且升级前不存在备份表。
     */
    @Test
    void upgradeFromV20AddsBackupSchema() throws SQLException {
        execute(FRESH_DATABASE, "CREATE DATABASE IF NOT EXISTS " + UPGRADE_DATABASE
                + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        migrate(UPGRADE_DATABASE, "20");
        assertThat(tableExists(UPGRADE_DATABASE, "db_backup_record")).isFalse();

        migrate(UPGRADE_DATABASE, "21");
        assertThat(tableExists(UPGRADE_DATABASE, "db_backup_record")).isTrue();
        assertThat(tableExists(UPGRADE_DATABASE, "db_restore_record")).isTrue();
    }

    private void migrate(final String database, final String target) {
        var configuration = Flyway.configure()
                .dataSource(url(database), "root", "root")
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(MigrationVersion.fromVersion(target));
        }
        configuration.load().migrate();
    }

    private boolean tableExists(final String database, final String table) throws SQLException {
        return count(database, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '"
                + database + "' AND table_name = '" + table + "'") > 0;
    }

    private long count(final String database, final String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url(database), "root", "root");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private void execute(final String database, final String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url(database), "root", "root");
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String url(final String database) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
