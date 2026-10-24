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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BackupCommandFactory} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupCommandFactoryTest {

    private static final String PASSWORD = "s3cret-password";

    private BackupCommandFactory factory;

    @BeforeEach
    void setUp() {
        BackupProperties properties = new BackupProperties();
        properties.getDatabase().setHost("localhost");
        properties.getDatabase().setPort(3306);
        properties.getDatabase().setName("iboot_admin");
        properties.getDatabase().setUsername("root");
        properties.getDatabase().setPassword(PASSWORD);
        properties.getCommand().setDumpPrefix(
                List.of("docker", "exec", "-i", "-e", "MYSQL_PWD", "iboot-mysql", "mysqldump"));
        properties.getCommand().setRestorePrefix(
                List.of("docker", "exec", "-i", "-e", "MYSQL_PWD", "iboot-mysql", "mysql"));
        factory = new BackupCommandFactory(properties);
    }

    /**
     * 验证导出命令使用一致性快照、UTF-8 与无库名模式，且密码不在参数中。
     */
    @Test
    void buildsDumpCommandWithoutPasswordOrDatabaseSelector() {
        List<String> command = factory.dumpCommand();

        assertThat(command).startsWith("docker", "exec", "-i", "-e", "MYSQL_PWD", "iboot-mysql", "mysqldump");
        assertThat(command).contains("--single-transaction", "--quick", "--default-character-set=utf8mb4",
                "--set-gtid-purged=OFF", "--no-tablespaces", "-uroot", "-hlocalhost", "-P3306", "iboot_admin");
        assertThat(command).doesNotContain("--databases", "-p" + PASSWORD, PASSWORD);
        assertThat(factory.environment()).containsEntry("MYSQL_PWD", PASSWORD);
    }

    /**
     * 验证还原命令显式指定目标库。
     */
    @Test
    void buildsRestoreCommandWithTargetSchema() {
        List<String> command = factory.restoreCommand("iboot_restore_drill");

        assertThat(command).endsWith("--database=iboot_restore_drill");
        assertThat(command).doesNotContain("iboot_admin", PASSWORD);
    }

    /**
     * 验证非法标识符被拒绝。
     */
    @Test
    void rejectsIllegalIdentifiers() {
        assertThatThrownBy(() -> factory.restoreCommand("iboot_restore_drill; DROP DATABASE iboot_admin"))
                .isInstanceOf(IllegalArgumentException.class);

        BackupProperties properties = new BackupProperties();
        properties.getDatabase().setName("iboot-admin");
        assertThatThrownBy(() -> new BackupCommandFactory(properties).dumpCommand())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
