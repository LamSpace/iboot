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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupTriggerType;
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.RestoreStatus;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import com.iboot.admin.infrastructure.storage.InMemoryFileStorageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RestoreDrillService} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RestoreDrillServiceTest {

    private static final String KEY = Base64.getEncoder().encodeToString(filledKey());

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T18:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private static final String STORAGE_PATH = "backup/iboot_admin/2026/10/24/B1.sql.enc";

    private byte[] plaintext;

    private FakeBackupRecordRepository backupRepository;

    private FakeRestoreRecordRepository restoreRepository;

    private FakeBackupExecutor executor;

    private InMemoryFileStorageStrategy storage;

    private BackupCipher cipher;

    private BackupProperties properties;

    private QueuedTaskExecutor taskExecutor;

    private RestoreDrillService service;

    private Long backupId;

    @BeforeEach
    void setUp() throws Exception {
        plaintext = "CREATE TABLE demo (id BIGINT);\nINSERT INTO demo VALUES (1);\n"
                .getBytes(StandardCharsets.UTF_8);
        properties = new BackupProperties();
        properties.setEnabled(true);
        properties.setEncryptionKey(KEY);
        properties.getDatabase().setName("iboot_admin");
        properties.getDatabase().setPassword("s3cret");
        backupRepository = new FakeBackupRecordRepository();
        restoreRepository = new FakeRestoreRecordRepository();
        executor = new FakeBackupExecutor("iboot_admin", "", Map.of("demo", 2L));
        storage = new InMemoryFileStorageStrategy();
        cipher = BackupCipher.fromBase64(KEY);
        taskExecutor = new QueuedTaskExecutor();

        byte[] encrypted = encrypt(plaintext);
        storage.save(new StorageObject(new ByteArrayInputStream(encrypted), (long) encrypted.length,
                "application/octet-stream", "B1.sql.enc"), STORAGE_PATH);
        BackupRecord backup = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin",
                LocalDateTime.now(CLOCK));
        backup.markSuccess("memory", STORAGE_PATH, encrypted.length, plaintext.length, checksum(plaintext),
                Map.of("demo", 2L), LocalDateTime.now(CLOCK).plusSeconds(1));
        backupRepository.insert(backup);
        backupId = backup.getId();

        service = new RestoreDrillService(properties, backupRepository, restoreRepository, executor, cipher,
                storage, taskExecutor, CLOCK);
    }

    /**
     * 验证成功恢复：重建演练库、还原明文并逐表行数一致。
     */
    @Test
    void drillSucceedsWhenCountsMatch() {
        RestoreRecord triggered = service.trigger(backupId, "admin");
        taskExecutor.runAll();

        RestoreRecord saved = restoreRepository.findById(triggered.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(RestoreStatus.SUCCESS);
        assertThat(saved.getTargetSchema()).isEqualTo("iboot_restore_drill");
        assertThat(saved.getTableStats()).containsEntry("demo", 2L);
        assertThat(saved.getMismatches()).isEmpty();
        assertThat(executor.recreatedSchemas()).containsExactly("iboot_restore_drill");
        assertThat(executor.restoredContent()).isEqualTo(plaintext);
    }

    /**
     * 验证行数快照不一致时状态为 MISMATCH 并保留差异。
     */
    @Test
    void drillReportsMismatch() {
        executor.restoredStats(Map.of("demo", 1L));

        RestoreRecord triggered = service.trigger(backupId, "admin");
        taskExecutor.runAll();

        RestoreRecord saved = restoreRepository.findById(triggered.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(RestoreStatus.MISMATCH);
        assertThat(saved.getMismatches()).hasSize(1);
        assertThat(saved.getMismatches().get(0).tableName()).isEqualTo("demo");
        assertThat(saved.getMismatches().get(0).expected()).isEqualTo(2L);
        assertThat(saved.getMismatches().get(0).actual()).isEqualTo(1L);
    }

    /**
     * 验证密文损坏时在重建演练库之前失败。
     */
    @Test
    void corruptCiphertextFailsBeforeDdl() throws Exception {
        byte[] tampered = encrypt(plaintext);
        tampered[tampered.length - 1] ^= 0x01;
        storage.delete(STORAGE_PATH);
        storage.save(new StorageObject(new ByteArrayInputStream(tampered), (long) tampered.length,
                "application/octet-stream", "B1.sql.enc"), STORAGE_PATH);

        RestoreRecord triggered = service.trigger(backupId, "admin");
        taskExecutor.runAll();

        assertThat(restoreRepository.findById(triggered.getId()).orElseThrow().getStatus())
                .isEqualTo(RestoreStatus.FAILED);
        assertThat(executor.recreatedSchemas()).isEmpty();
    }

    /**
     * 验证非成功备份不可演练。
     */
    @Test
    void rejectsNonSuccessfulBackup() {
        BackupRecord failed = BackupRecord.start("B2", BackupTriggerType.MANUAL, "iboot_admin", "admin",
                LocalDateTime.now(CLOCK));
        failed.markFailed("导出失败", LocalDateTime.now(CLOCK).plusSeconds(1));
        backupRepository.insert(failed);

        assertThatThrownBy(() -> service.trigger(failed.getId(), "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有成功的备份");
    }

    /**
     * 验证功能关闭时拒绝演练。
     */
    @Test
    void rejectsDrillWhenDisabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.trigger(backupId, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未启用");
    }

    /**
     * 验证同时只允许一个演练。
     */
    @Test
    void rejectsConcurrentDrill() {
        service.trigger(backupId, "admin");

        assertThatThrownBy(() -> service.trigger(backupId, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有恢复演练正在执行");

        taskExecutor.runAll();
    }

    private byte[] encrypt(final byte[] content) throws Exception {
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        try (var output = cipher.encrypting(encrypted)) {
            output.write(content);
        }
        return encrypted.toByteArray();
    }

    private String checksum(final byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static byte[] filledKey() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 5);
        return key;
    }

    /**
     * 可手动驱动的异步执行器。
     */
    private static final class QueuedTaskExecutor implements AsyncTaskExecutor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(final Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            List<Runnable> pending = new ArrayList<>(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
    }
}
