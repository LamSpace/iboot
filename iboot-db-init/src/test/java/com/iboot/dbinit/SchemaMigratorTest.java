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

package com.iboot.dbinit;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 迁移执行器测试。
 *
 * <p>在空库上执行 baseline，验证全部表与种子数据被创建；再次执行时不再应用任何迁移。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class SchemaMigratorTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_test")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Test
    void createsAllTablesAndIsIdempotent() throws SQLException {
        MysqlDataSource dataSource = dataSource();

        MigrateResult first = migrateWithRetry(dataSource);
        assertThat(first.migrationsExecuted).isEqualTo(12);
        assertThat(countTables(dataSource)).isEqualTo(81);

        MigrateResult second = migrateWithRetry(dataSource);
        assertThat(second.migrationsExecuted).isZero();
        assertThat(countTables(dataSource)).isEqualTo(81);

        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_user")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu")).isGreaterThan(100);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.file.silo.endpoint'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key LIKE 'sys.file.silo.%'")).isEqualTo(5);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.file.storageType' AND config_value = 'silo'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.silo.console.url'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_name = 'Silo服务端点'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_name LIKE '%Silo%'")).isEqualTo(6);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_name LIKE '%MinIO%' OR remark LIKE '%MinIO%'")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 706 AND path = '/monitor/silo-monitor' "
                + "AND permission = 'monitor:silo'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 7061 AND permission = 'silo:query'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 712 AND parent_id = 9700 AND path = '/observability/tempo-monitor' "
                + "AND permission = 'monitor:tempo'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 7121 AND parent_id = 712 AND permission = 'tempo:query'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_role_menu "
                + "WHERE role_id = 1 AND menu_id IN (712, 7121)")).isEqualTo(2);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_role_menu "
                + "WHERE role_id = 2 AND menu_id IN (712, 7121)")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 9600 AND parent_id = 0 AND path = '/governance' "
                + "AND menu_type = 'M'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 9601 AND parent_id = 9600 AND path = '/governance/nacos' "
                + "AND permission = 'governance:nacos'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu "
                + "WHERE id = 9602 AND parent_id = 9600 AND path = '/governance/sentinel' "
                + "AND permission = 'governance:sentinel'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_role_menu "
                + "WHERE role_id = 1 AND menu_id IN (9600, 9601, 9602)")).isEqualTo(3);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.nacos.console.url' "
                + "AND config_value = '/nacos-console/'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.nacos.console.embed' AND config_value = 'true'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.sentinel.dashboard.url' "
                + "AND config_value = 'http://localhost:8858'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.sentinel.dashboard.embed' AND config_value = 'true'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'sys_config' "
                + "AND column_name = 'config_group'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_dict_type "
                + "WHERE dict_type = 'sys_config_group' AND deleted = 0")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_dict_data "
                + "WHERE dict_type = 'sys_config_group' AND deleted = 0")).isEqualTo(6);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'basic'")).isEqualTo(8);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'message'")).isEqualTo(4);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'storage'")).isEqualTo(13);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'monitor'")).isEqualTo(13);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'console'")).isEqualTo(15);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE deleted = 0 AND config_group = 'other'")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.nacos.console.url' AND config_group = 'console'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.sentinel.dashboard.url' AND config_group = 'console'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.file.storageType' AND config_group = 'storage'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'monitor.cpu.warn.threshold' AND config_group = 'monitor'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.grafana.monitor.url' AND config_group = 'console'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM biz_notification_outbox")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'biz_notification_outbox' "
                + "AND index_name IN ('uk_event_id', 'idx_status_retry')")).isEqualTo(3);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_user_leave_quota")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_leave_quota_log")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM undo_log")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_leave_quota_transfer")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu WHERE id = 9105 "
                + "AND permission = 'approval:quota:transfer'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_role_menu "
                + "WHERE role_id = 1 AND menu_id = 9105")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'sys_leave_quota_transfer' "
                + "AND index_name IN ('uk_transfer_id', 'uk_xid')")).isEqualTo(2);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'sys_user_leave_quota' "
                + "AND index_name = 'uk_user_year'")).isEqualTo(2);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'sys_leave_quota_log' "
                + "AND index_name = 'uk_request_id'")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'undo_log' "
                + "AND index_name = 'ux_undo_log'")).isEqualTo(2);
    }

    @Test
    void upgradesExistingLocalStorageTypeToSilo() throws SQLException {
        String schema = "iboot_upgrade_" + System.nanoTime();
        Flyway.configure()
                .dataSource(dataSource())
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion("7"))
                .load()
                .migrate();

        MysqlDataSource scoped = dataSource(schema);
        assertThat(count(scoped, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.file.storageType' AND config_value = 'local'")).isEqualTo(1);

        SchemaMigrator.migrate(scoped, schema);

        assertThat(count(scoped, "SELECT COUNT(*) FROM sys_config "
                + "WHERE config_key = 'sys.file.storageType' AND config_value = 'silo'")).isEqualTo(1);
    }

    private MigrateResult migrateWithRetry(MysqlDataSource dataSource) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                return SchemaMigrator.migrate(dataSource);
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

    private MysqlDataSource dataSource() {
        return dataSource("iboot_test");
    }

    private MysqlDataSource dataSource(String database) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUser("root");
        dataSource.setPassword("root");
        return dataSource;
    }

    private int countTables(MysqlDataSource dataSource) throws SQLException {
        return count(dataSource,
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'iboot_test'");
    }

    private int count(MysqlDataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
