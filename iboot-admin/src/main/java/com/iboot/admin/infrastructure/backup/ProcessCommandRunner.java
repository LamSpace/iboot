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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@link ProcessBuilder} 的命令执行器。
 *
 * <p>使用虚拟线程并行消费 stdout、stderr 与写入 stdin，避免管道缓冲区写满导致死锁；
 * 标准错误最多保留 8 KB 用于失败诊断。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ProcessCommandRunner implements CommandRunner {

    /**
     * 标准错误最大保留字节数。
     */
    private static final int MAX_STDERR_BYTES = 8192;

    @Override
    public CommandResult run(final List<String> command, final Map<String, String> environment,
                             final InputStream stdin, final OutputStream stdout)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (environment != null && !environment.isEmpty()) {
            builder.environment().putAll(environment);
        }
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException("无法启动命令: " + command.get(0), e);
        }

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        Thread stderrThread = Thread.ofVirtual().start(() -> copyLimited(process.getErrorStream(), stderr));
        Thread stdinThread = null;
        if (stdin != null) {
            stdinThread = Thread.ofVirtual().start(() -> {
                try (OutputStream processIn = process.getOutputStream()) {
                    stdin.transferTo(processIn);
                } catch (IOException ignored) {
                    // 进程提前退出时输入管道会关闭，最终以退出码判断失败
                }
            });
        } else {
            process.getOutputStream().close();
        }

        try (InputStream processOut = process.getInputStream()) {
            processOut.transferTo(stdout == null ? OutputStream.nullOutputStream() : stdout);
        } catch (IOException e) {
            process.destroyForcibly();
            throw e;
        }

        int exitCode = process.waitFor();
        stderrThread.join();
        if (stdinThread != null) {
            stdinThread.join();
        }
        return new CommandResult(exitCode, stderr.toString(StandardCharsets.UTF_8));
    }

    private void copyLimited(final InputStream source, final ByteArrayOutputStream target) {
        byte[] buffer = new byte[1024];
        int total = 0;
        try (source) {
            int read;
            while ((read = source.read(buffer)) != -1) {
                if (total < MAX_STDERR_BYTES) {
                    int writable = Math.min(read, MAX_STDERR_BYTES - total);
                    target.write(buffer, 0, writable);
                    total += writable;
                }
            }
        } catch (IOException ignored) {
            // 进程结束时管道关闭属正常现象
        }
    }
}
