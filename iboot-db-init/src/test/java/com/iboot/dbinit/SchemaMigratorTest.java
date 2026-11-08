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
        assertThat(first.migrationsExecuted).isEqualTo(4);
        assertThat(countTables(dataSource)).isEqualTo(77);

        MigrateResult second = migrateWithRetry(dataSource);
        assertThat(second.migrationsExecuted).isZero();
        assertThat(countTables(dataSource)).isEqualTo(77);

        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_user")).isEqualTo(1);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM sys_menu")).isGreaterThan(100);
        assertThat(count(dataSource, "SELECT COUNT(*) FROM biz_notification_outbox")).isZero();
        assertThat(count(dataSource, "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = 'iboot_test' AND table_name = 'biz_notification_outbox' "
                + "AND index_name IN ('uk_event_id', 'idx_status_retry')")).isEqualTo(3);
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
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
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
