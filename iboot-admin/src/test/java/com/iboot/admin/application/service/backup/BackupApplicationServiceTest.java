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

package com.iboot.admin.application.service.backup;

import com.iboot.kernel.web.BusinessException;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupStatus;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import com.iboot.admin.infrastructure.storage.InMemoryFileStorageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BackupApplicationService} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupApplicationServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(filledKey());

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T18:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private static final String DUMP = "CREATE TABLE demo (id BIGINT);\nINSERT INTO demo VALUES (1);\n";

    private FakeBackupRecordRepository repository;

    private FakeBackupExecutor executor;

    private InMemoryFileStorageStrategy storage;

    private BackupCipher cipher;

    private BackupProperties properties;

    private QueuedTaskExecutor taskExecutor;

    private BackupApplicationService service;

    @BeforeEach
    void setUp() {
        properties = new BackupProperties();
        properties.setEnabled(true);
        properties.setEncryptionKey(KEY);
        properties.getDatabase().setName("iboot_admin");
        properties.getDatabase().setPassword("s3cret");
        repository = new FakeBackupRecordRepository();
        executor = new FakeBackupExecutor("iboot_admin", DUMP, Map.of("demo", 2L));
        storage = new InMemoryFileStorageStrategy();
        cipher = BackupCipher.fromBase64(KEY);
        taskExecutor = new QueuedTaskExecutor();
        service = new BackupApplicationService(properties, repository, executor, cipher, storage,
                taskExecutor, CLOCK);
    }

    /**
     * 验证手动备份完成加密、存储与元数据登记。
     */
    @Test
    void manualBackupStoresEncryptedArtifact() throws Exception {
        BackupRecord triggered = service.triggerManual("admin");
        taskExecutor.runAll();

        BackupRecord saved = repository.findById(triggered.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(saved.getStorageType()).isEqualTo("memory");
        assertThat(saved.getStoragePath())
                .startsWith("backup/iboot_admin/2026/10/24/")
                .endsWith(".sql.enc");
        assertThat(saved.getCiphertextSize()).isPositive();
        assertThat(saved.getPlaintextSize()).isEqualTo(DUMP.getBytes().length);
        assertThat(saved.getChecksumSha256()).hasSize(64);
        assertThat(saved.getTableStats()).containsEntry("demo", 2L);
        assertThat(storage.exists(saved.getStoragePath())).isTrue();

        ByteArrayOutputStream plaintext = new ByteArrayOutputStream();
        cipher.decrypt(storage.getFileStream(saved.getStoragePath()).getInputStream(), plaintext);
        assertThat(plaintext.toString()).isEqualTo(DUMP);
    }

    /**
     * 验证功能关闭时拒绝手动触发。
     */
    @Test
    void rejectsTriggerWhenDisabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.triggerManual("admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未启用");
    }

    /**
     * 验证运行中不允许再次触发，完成后可继续触发。
     */
    @Test
    void rejectsConcurrentTrigger() {
        service.triggerManual("admin");

        assertThatThrownBy(() -> service.triggerManual("admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有备份正在执行");

        taskExecutor.runAll();
        service.triggerManual("admin");
        taskExecutor.runAll();

        assertThat(repository.countAll()).isEqualTo(2);
    }

    /**
     * 验证备份任务在独立线程执行后，锁仍能被释放、可以再次触发。
     *
     * <p>回归用例：原先使用 {@code ReentrantLock}，加锁在请求线程、解锁在执行器线程，
     * 跨线程解锁抛 {@code IllegalMonitorStateException}，导致每个进程只能备份一次。
     */
    @Test
    void allowsTriggerAgainAfterTaskRunsOnAnotherThread() {
        service.triggerManual("admin");
        taskExecutor.runAllOnNewThread();

        service.triggerManual("admin");
        taskExecutor.runAllOnNewThread();

        assertThat(repository.countAll()).isEqualTo(2);
    }

    /**
     * 验证导出失败时记录为失败、错误脱敏且不产生产物。
     */
    @Test
    void marksFailedAndSanitizesWhenDumpFails() {
        executor.failDump();

        BackupRecord triggered = service.triggerManual("admin");
        taskExecutor.runAll();

        BackupRecord saved = repository.findById(triggered.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(saved.getErrorMessage()).contains("******").doesNotContain("s3cret");
        assertThat(saved.getStoragePath()).isNull();
        assertThat(saved.getCiphertextSize()).isNull();
    }

    /**
     * 验证存储失败时记录为失败。
     */
    @Test
    void marksFailedWhenStorageFails() {
        FileStorageStrategy failing = new FailingFileStorageStrategy();
        service = new BackupApplicationService(properties, repository, executor, cipher, failing,
                taskExecutor, CLOCK);

        BackupRecord triggered = service.triggerManual("admin");
        taskExecutor.runAll();

        assertThat(repository.findById(triggered.getId()).orElseThrow().getStatus())
                .isEqualTo(BackupStatus.FAILED);
    }

    /**
     * 验证触发前会重置超时的运行中记录。
     */
    @Test
    void resetsStaleRunningBeforeTrigger() {
        BackupRecord stale = BackupRecord.start("OLD", com.iboot.admin.domain.backup.model.BackupTriggerType.MANUAL,
                "iboot_admin", "admin", LocalDateTime.now(CLOCK).minusHours(2));
        repository.insert(stale);

        service.triggerManual("admin");
        taskExecutor.runAll();

        assertThat(repository.resetCalls()).isEqualTo(1);
        assertThat(repository.findById(stale.getId()).orElseThrow().getStatus())
                .isEqualTo(BackupStatus.FAILED);
    }

    /**
     * 验证成功记录更新失败不会让异步任务抛出，记录保持运行中等待超时重置。
     */
    @Test
    void updateFailureDoesNotPropagate() {
        repository.failNextUpdate();

        BackupRecord triggered = service.triggerManual("admin");
        taskExecutor.runAll();

        assertThat(repository.findById(triggered.getId()).orElseThrow().getStatus())
                .isEqualTo(BackupStatus.RUNNING);
    }

    /**
     * 验证分页与概览统计。
     */
    @Test
    void returnsPageAndSummary() {
        service.triggerManual("admin");
        taskExecutor.runAll();

        assertThat(service.findPage(null, 1, 10)).hasSize(1);
        assertThat(service.count(null)).isEqualTo(1);
        BackupSummary summary = service.summary();
        assertThat(summary.enabled()).isTrue();
        assertThat(summary.totalCount()).isEqualTo(1);
        assertThat(summary.totalSize()).isPositive();
        assertThat(summary.lastSuccessAt()).isNotNull();
    }

    private static byte[] filledKey() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 5);
        return key;
    }

    /**
     * 可手动驱动的异步执行器，让测试精确控制任务执行时机。
     */
    private static final class QueuedTaskExecutor implements AsyncTaskExecutor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(final Runnable task) {
            tasks.add(task);
        }

        /**
         * 在新线程中执行全部任务，模拟真实的异步执行器。
         */
        void runAllOnNewThread() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            for (Runnable task : pending) {
                Thread thread = new Thread(task, "backup-task");
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }

        void runAll() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
    }

    /**
     * 保存总是失败的存储策略。
     */
    private static final class FailingFileStorageStrategy implements FileStorageStrategy {

        @Override
        public String save(final StorageObject source, final String relativePath) throws IOException {
            throw new IOException("模拟存储失败");
        }

        @Override
        public String getFileUrl(final String filePath) {
            return filePath;
        }

        @Override
        public StorageObject getFileStream(final String filePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(final String filePath) {
            return false;
        }

        @Override
        public boolean exists(final String filePath) {
            return false;
        }

        @Override
        public String getStorageType() {
            return "failing";
        }

        @Override
        public boolean isObjectStorage() {
            return false;
        }
    }
}
