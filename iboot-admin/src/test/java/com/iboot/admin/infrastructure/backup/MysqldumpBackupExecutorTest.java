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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MysqldumpBackupExecutor} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class MysqldumpBackupExecutorTest {

    private static final String PASSWORD = "s3cret-password";

    private BackupProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BackupProperties();
        properties.getDatabase().setPassword(PASSWORD);
    }

    /**
     * 验证导出成功时把标准输出写入目标流。
     */
    @Test
    void dumpWritesCommandOutput() throws IOException {
        CommandRunner runner = (command, environment, stdin, stdout) -> {
            stdout.write("CREATE TABLE t (id INT);".getBytes());
            return new CommandResult(0, "");
        };
        ByteArrayOutputStream target = new ByteArrayOutputStream();

        executor(runner).dump(target);

        assertThat(target.toString()).contains("CREATE TABLE t");
    }

    /**
     * 验证非零退出码转换为受控异常，且密码已脱敏。
     */
    @Test
    void dumpFailureIsSanitized() {
        CommandRunner runner = (command, environment, stdin, stdout) ->
                new CommandResult(2, "Access denied for user 'root' with password " + PASSWORD);

        assertThatThrownBy(() -> executor(runner).dump(OutputStream.nullOutputStream()))
                .isInstanceOf(CommandExecutionException.class)
                .hasMessageContaining("退出码: 2")
                .hasMessageContaining("******")
                .hasMessageNotContaining(PASSWORD);
    }

    /**
     * 验证中断时恢复中断标记并转换为 IO 异常。
     */
    @Test
    void interruptIsPropagated() throws IOException {
        CommandRunner runner = (command, environment, stdin, stdout) -> {
            throw new InterruptedException("interrupted");
        };

        try {
            assertThatThrownBy(() -> executor(runner).dump(OutputStream.nullOutputStream()))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("中断");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * 验证还原时把明文送入命令标准输入。
     */
    @Test
    void restoreFeedsStandardInput() throws IOException {
        ByteArrayOutputStream received = new ByteArrayOutputStream();
        CommandRunner runner = (command, environment, stdin, stdout) -> {
            stdin.transferTo(received);
            return new CommandResult(0, "");
        };
        byte[] sql = "INSERT INTO t VALUES (1);".getBytes();

        executor(runner).restore(new ByteArrayInputStream(sql), "iboot_restore_drill");

        assertThat(received.toByteArray()).isEqualTo(sql);
    }

    private MysqldumpBackupExecutor executor(final CommandRunner runner) {
        return new MysqldumpBackupExecutor(runner, new BackupCommandFactory(properties), properties);
    }
}
