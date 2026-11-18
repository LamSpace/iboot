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

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.AtomicFiles;
import com.iboot.cli.config.CliPaths;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 按 context 隔离的令牌存储。
 *
 * <p>凭据写入 {@code credentials.json}，权限为 {@code 0600}；写入使用同目录临时文件
 * 加原子替换。刷新操作通过 {@link #withLock(Supplier)} 串行化，避免多个 CLI 进程
 * 同时轮换 refresh token 导致互相作废。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CredentialStore {

    private static final TypeReference<Map<String, TokenSet>> TOKEN_MAP =
            new TypeReference<>() {
            };

    private final Path credentialsFile;

    private final Path lockFile;

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 同一 JVM 内的进程级互斥；跨进程由文件锁保证。
     */
    private final java.util.concurrent.locks.ReentrantLock processLock =
            new java.util.concurrent.locks.ReentrantLock();

    /**
     * 标记当前线程是否已持有锁，使 {@link #save(String, TokenSet)} 等嵌套调用可重入。
     */
    private final ThreadLocal<Boolean> lockHeld = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 使用默认凭据路径创建存储。
     */
    public CredentialStore() {
        this(CliPaths.credentialsFile(), CliPaths.credentialsLockFile());
    }

    /**
     * 使用指定路径创建存储，便于测试。
     *
     * @param credentialsFile 凭据文件
     * @param lockFile 文件锁路径
     */
    public CredentialStore(Path credentialsFile, Path lockFile) {
        this.credentialsFile = credentialsFile;
        this.lockFile = lockFile;
    }

    /**
     * 读取指定 context 的令牌。
     *
     * @param context context 名称
     * @return 令牌；不存在时返回 {@code null}
     */
    public TokenSet load(String context) {
        return readAll().get(context);
    }

    /**
     * 保存指定 context 的令牌。
     *
     * @param context context 名称
     * @param tokens 令牌
     */
    public void save(String context, TokenSet tokens) {
        withLock(() -> {
            Map<String, TokenSet> all = readAll();
            all.put(context, tokens);
            writeAll(all);
            return null;
        });
    }

    /**
     * 删除指定 context 的令牌。
     *
     * @param context context 名称
     */
    public void delete(String context) {
        withLock(() -> {
            Map<String, TokenSet> all = readAll();
            if (all.remove(context) != null) {
                writeAll(all);
            }
            return null;
        });
    }

    /**
     * 在凭据文件锁内执行操作。
     *
     * <p>锁内读取的是磁盘上的最新内容，调用方不要复用锁外读取的令牌对象来构造新集合。
     *
     * @param action 操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T withLock(Supplier<T> action) {
        if (Boolean.TRUE.equals(lockHeld.get())) {
            return action.get();
        }
        processLock.lock();
        try {
            lockHeld.set(Boolean.TRUE);
            Path parent = lockFile.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return action.get();
            }
        } catch (IOException e) {
            throw new CliException(ExitCode.BUSINESS, "无法锁定凭据文件：" + lockFile, e);
        } finally {
            lockHeld.set(Boolean.FALSE);
            processLock.unlock();
        }
    }

    private Map<String, TokenSet> readAll() {
        if (!Files.exists(credentialsFile)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, TokenSet> values = mapper.readValue(Files.readString(credentialsFile), TOKEN_MAP);
            return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
        } catch (IOException | RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "无法读取凭据文件：" + credentialsFile, e);
        }
    }

    private void writeAll(Map<String, TokenSet> values) {
        try {
            AtomicFiles.writeSecret(credentialsFile,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(values));
        } catch (IOException | RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "无法写入凭据文件：" + credentialsFile, e);
        }
    }
}
