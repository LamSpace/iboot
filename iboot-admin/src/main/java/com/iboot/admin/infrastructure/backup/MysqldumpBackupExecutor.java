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

import com.iboot.admin.domain.backup.service.BackupExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@code mysqldump}/{@code mysql} 的备份执行器。
 *
 * <p>命令通过 {@link CommandRunner} 执行，默认前缀为容器内的客户端命令；
 * 库级 DDL 与行数统计通过 JDBC 完成。所有数据库标识符先做白名单校验，
 * 再以反引号引用，避免拼接注入。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupExecutor
 */
@Component
public class MysqldumpBackupExecutor implements BackupExecutor {

    /**
     * 命令执行器。
     */
    private final CommandRunner commandRunner;

    /**
     * 命令组装器。
     */
    private final BackupCommandFactory commandFactory;

    /**
     * 备份配置。
     */
    private final BackupProperties properties;

    @SuppressWarnings("all")
    public MysqldumpBackupExecutor(final CommandRunner commandRunner, final BackupCommandFactory commandFactory,
                                   final BackupProperties properties) {
        this.commandRunner = commandRunner;
        this.commandFactory = commandFactory;
        this.properties = properties;
    }

    @Override
    public void dump(final OutputStream target) throws IOException {
        CommandResult result;
        try {
            result = commandRunner.run(commandFactory.dumpCommand(), commandFactory.environment(), null, target);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("导出命令被中断", e);
        }
        if (result.exitCode() != 0) {
            throw new CommandExecutionException("导出", result.exitCode(), sanitize(result.stderr()));
        }
    }

    @Override
    public void restore(final InputStream source, final String targetSchema) throws IOException {
        CommandResult result;
        try {
            result = commandRunner.run(commandFactory.restoreCommand(targetSchema), commandFactory.environment(),
                    source, OutputStream.nullOutputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("还原命令被中断", e);
        }
        if (result.exitCode() != 0) {
            throw new CommandExecutionException("还原", result.exitCode(), sanitize(result.stderr()));
        }
    }

    @Override
    public void recreateSchema(final String schema) throws IOException {
        requireIdentifier(schema);
        try (Connection connection = openServerConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS " + quote(schema));
            statement.executeUpdate("CREATE DATABASE " + quote(schema)
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new IOException("重建演练库失败: " + schema, e);
        }
    }

    @Override
    public Map<String, Long> tableRowCounts(final String schema) throws IOException {
        requireIdentifier(schema);
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Connection connection = openServerConnection()) {
            List<String> tables = listBaseTables(connection, schema);
            for (String table : tables) {
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(
                             "SELECT COUNT(*) FROM " + quote(schema) + "." + quote(table))) {
                    resultSet.next();
                    counts.put(table, resultSet.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IOException("统计表行数失败: " + schema, e);
        }
        return counts;
    }

    private List<String> listBaseTables(final Connection connection, final String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name")) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1));
                }
            }
        }
        return tables;
    }

    private Connection openServerConnection() throws SQLException {
        BackupProperties.Database database = properties.getDatabase();
        String url = "jdbc:mysql://" + database.getHost() + ":" + database.getPort()
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
        return DriverManager.getConnection(url, database.getUsername(), database.getPassword());
    }

    private void requireIdentifier(final String schema) {
        if (!BackupCommandFactory.isValidIdentifier(schema)) {
            throw new IllegalArgumentException("数据库标识符不合法: " + schema);
        }
    }

    private String quote(final String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String sanitize(final String stderr) {
        return BackupErrorSanitizer.sanitize(stderr, properties.getDatabase().getPassword());
    }
}
