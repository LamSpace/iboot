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

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 备份命令组装器。
 *
 * <p>以参数列表方式拼装 {@code mysqldump}/{@code mysql} 命令，不经过 shell；
 * 密码只通过 {@code MYSQL_PWD} 进程环境传递。导出使用一致性快照且不写库名，
 * 使同一份 dump 能够被还原到演练库。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class BackupCommandFactory {

    /**
     * 合法的数据库标识符。
     */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

    /**
     * 备份配置。
     */
    private final BackupProperties properties;

    @SuppressWarnings("all")
    public BackupCommandFactory(final BackupProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断数据库标识符是否合法。
     *
     * @param value 标识符
     * @return 仅含字母、数字与下划线时返回 {@code true}
     */
    public static boolean isValidIdentifier(final String value) {
        return value != null && IDENTIFIER.matcher(value).matches();
    }

    /**
     * 组装导出命令。
     *
     * @return 命令参数列表
     * @throws IllegalArgumentException 数据库名不合法时抛出
     */
    public List<String> dumpCommand() {
        BackupProperties.Database database = properties.getDatabase();
        requireIdentifier(database.getName(), "备份数据库名");
        List<String> command = new ArrayList<>(properties.getCommand().getDumpPrefix());
        command.add("--single-transaction");
        command.add("--quick");
        command.add("--default-character-set=utf8mb4");
        command.add("--set-gtid-purged=OFF");
        command.add("--no-tablespaces");
        command.add("-u" + database.getUsername());
        command.add("-h" + commandHost());
        command.add("-P" + commandPort());
        command.add(database.getName());
        return List.copyOf(command);
    }

    /**
     * 组装还原命令。
     *
     * @param targetSchema 目标库名
     * @return 命令参数列表
     * @throws IllegalArgumentException 目标库名不合法时抛出
     */
    public List<String> restoreCommand(final String targetSchema) {
        requireIdentifier(targetSchema, "恢复目标库名");
        BackupProperties.Database database = properties.getDatabase();
        List<String> command = new ArrayList<>(properties.getCommand().getRestorePrefix());
        command.add("--default-character-set=utf8mb4");
        command.add("-u" + database.getUsername());
        command.add("-h" + commandHost());
        command.add("-P" + commandPort());
        command.add("--database=" + targetSchema);
        return List.copyOf(command);
    }

    /**
     * 组装命令执行环境。
     *
     * @return 包含 {@code MYSQL_PWD} 的环境变量
     */
    public Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        String password = properties.getDatabase().getPassword();
        if (password != null) {
            environment.put("MYSQL_PWD", password);
        }
        return Map.copyOf(environment);
    }

    private String commandHost() {
        String host = properties.getCommand().getHost();
        return host == null || host.isBlank() ? properties.getDatabase().getHost() : host;
    }

    private int commandPort() {
        Integer port = properties.getCommand().getPort();
        return port == null ? properties.getDatabase().getPort() : port;
    }

    private void requireIdentifier(final String value, final String label) {
        if (!isValidIdentifier(value)) {
            throw new IllegalArgumentException(label + "不合法: " + value);
        }
    }
}
