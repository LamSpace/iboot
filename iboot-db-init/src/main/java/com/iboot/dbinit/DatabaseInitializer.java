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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/**
 * 数据库初始化进程：建库、执行全部迁移，成功后退出。
 *
 * <p>作为一次性执行者存在，不承担任何 Web 或常驻职责。连接信息与服务共用同一组环境变量，
 * 数据库名默认为 {@code iboot}，可由 {@code IBOOT_DB_NAME} 覆盖。
 *
 * <p>退出码：全部成功为 0；连接失败、建库失败或任一迁移失败为 1。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

    private static final String CONNECTION_PARAMS =
            "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true";

    private DatabaseInitializer() {
    }

    /**
     * 程序入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        String host = env("MYSQL_HOST", "localhost");
        String port = env("MYSQL_PORT", "3306");
        String username = env("MYSQL_USERNAME", "root");
        String password = env("MYSQL_PASSWORD", "root");
        String database = env("IBOOT_DB_NAME", "iboot");

        if (!IDENTIFIER.matcher(database).matches()) {
            log.error("数据库名不合法：{}", database);
            System.exit(1);
        }

        try {
            createDatabaseIfAbsent(host, port, username, password, database);
            MigrateResult result = SchemaMigrator.migrate(dataSource(host, port, username, password, database));
            log.info("数据库 {} 初始化完成：本次应用 {} 个迁移，当前版本 {}",
                    database, result.migrationsExecuted, result.targetSchemaVersion);
        } catch (RuntimeException | SQLException e) {
            log.error("数据库初始化失败", e);
            System.exit(1);
        }
    }

    private static void createDatabaseIfAbsent(String host, String port, String username,
                                               String password, String database) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + CONNECTION_PARAMS;
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + database
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            log.info("数据库 {} 已就绪", database);
        }
    }

    private static MysqlDataSource dataSource(String host, String port, String username,
                                              String password, String database) throws SQLException {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL("jdbc:mysql://" + host + ":" + port + "/" + database + CONNECTION_PARAMS);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
