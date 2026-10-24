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
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.TableCountMismatch;
import com.iboot.admin.domain.backup.repository.BackupRecordRepository;
import com.iboot.admin.domain.backup.repository.RestoreRecordRepository;
import com.iboot.admin.domain.backup.service.BackupExecutor;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupErrorSanitizer;
import com.iboot.admin.infrastructure.backup.BackupIntegrityException;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 恢复演练应用服务。
 *
 * <p>只处理状态成功的备份。演练先把对象解密到临时文件并校验明文 SHA-256，
 * 校验通过后才删除并重建固定演练库、执行还原并逐表比对行数。目标库由服务端固定，
 * 任何指向业务库或其他库名的请求都会被 {@link RestoreTargetGuard} 拒绝。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupExecutor
 * @see RestoreTargetGuard
 */
@Service
public class RestoreDrillService {

    private static final Logger log = LoggerFactory.getLogger(RestoreDrillService.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final BackupProperties properties;

    private final BackupRecordRepository backupRepository;

    private final RestoreRecordRepository restoreRepository;

    private final BackupExecutor backupExecutor;

    private final BackupCipher backupCipher;

    private final FileStorageStrategy fileStorageStrategy;

    private final AsyncTaskExecutor taskExecutor;

    private final Clock clock;

    private final ReentrantLock runningLock = new ReentrantLock();

    @SuppressWarnings("all")
    public RestoreDrillService(final BackupProperties properties,
                               final BackupRecordRepository backupRepository,
                               final RestoreRecordRepository restoreRepository,
                               final BackupExecutor backupExecutor,
                               final BackupCipher backupCipher,
                               final FileStorageStrategy fileStorageStrategy,
                               @Qualifier("taskExecutor") final AsyncTaskExecutor taskExecutor,
                               final Clock clock) {
        this.properties = properties;
        this.backupRepository = backupRepository;
        this.restoreRepository = restoreRepository;
        this.backupExecutor = backupExecutor;
        this.backupCipher = backupCipher;
        this.fileStorageStrategy = fileStorageStrategy;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    /**
     * 对指定备份发起恢复演练。
     *
     * @param backupId 备份记录 ID
     * @param operator 触发人
     * @return 运行中的演练记录
     * @throws BusinessException 功能未启用、备份不可用或已有演练正在执行时抛出
     */
    public RestoreRecord trigger(final Long backupId, final String operator) {
        if (!properties.isEnabled()) {
            throw new BusinessException("备份功能未启用");
        }
        BackupRecord backup = backupRepository.findById(backupId)
                .orElseThrow(() -> new BusinessException("备份记录不存在: " + backupId));
        if (!backup.isRestorable()) {
            throw new BusinessException("只有成功的备份可以执行恢复演练");
        }
        String schema = properties.getRestore().getSchema();
        RestoreTargetGuard.requireValid(schema, properties.getRestore().getPrefix(),
                properties.getDatabase().getName());
        if (!runningLock.tryLock()) {
            throw new BusinessException("已有恢复演练正在执行");
        }
        boolean submitted = false;
        try {
            if (restoreRepository.findRunning().isPresent()) {
                throw new BusinessException("已有恢复演练正在执行");
            }
            RestoreRecord record = RestoreRecord.start(backupId, schema, operator, LocalDateTime.now(clock));
            restoreRepository.insert(record);
            Runnable task = () -> {
                try {
                    execute(record.getId());
                } catch (Throwable t) {
                    log.error("恢复演练异步执行异常: id={}", record.getId(), t);
                } finally {
                    runningLock.unlock();
                }
            };
            try {
                taskExecutor.execute(task);
            } catch (RuntimeException e) {
                runningLock.unlock();
                submitted = true;
                throw e;
            }
            submitted = true;
            return record;
        } finally {
            if (!submitted) {
                runningLock.unlock();
            }
        }
    }

    /**
     * 执行指定演练记录。
     *
     * @param id 演练记录 ID
     */
    public void execute(final Long id) {
        Optional<RestoreRecord> found = restoreRepository.findById(id);
        if (found.isEmpty()) {
            log.warn("演练记录不存在，忽略执行: {}", id);
            return;
        }
        RestoreRecord record = found.get();
        if (!record.isRunning()) {
            log.warn("演练记录不处于运行中，忽略执行: {}", id);
            return;
        }
        Path tempFile = null;
        try {
            BackupRecord backup = backupRepository.findById(record.getBackupId())
                    .filter(BackupRecord::isRestorable)
                    .orElseThrow(() -> new BusinessException("备份记录不可用"));
            tempFile = Files.createTempFile("iboot-restore-", ".sql");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream source = fileStorageStrategy.getFileStream(backup.getStoragePath()).getInputStream();
                 OutputStream target = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                DigestOutputStream digestOut = new DigestOutputStream(target, digest);
                backupCipher.decrypt(source, digestOut);
                digestOut.flush();
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            if (!checksum.equalsIgnoreCase(backup.getChecksumSha256())) {
                throw new BackupIntegrityException("备份明文校验和不一致");
            }

            String schema = record.getTargetSchema();
            RestoreTargetGuard.requireValid(schema, properties.getRestore().getPrefix(),
                    properties.getDatabase().getName());
            backupExecutor.recreateSchema(schema);
            try (InputStream sql = new BufferedInputStream(Files.newInputStream(tempFile))) {
                backupExecutor.restore(sql, schema);
            }
            Map<String, Long> restoredStats = backupExecutor.tableRowCounts(schema);
            List<TableCountMismatch> mismatches = compare(backup.getTableStats(), restoredStats);
            LocalDateTime now = LocalDateTime.now(clock);
            if (mismatches.isEmpty()) {
                record.markSuccess(restoredStats, now);
                log.info("恢复演练成功: backupId={}, schema={}", backup.getId(), schema);
            } else {
                record.markMismatch(restoredStats, mismatches, now);
                log.warn("恢复演练行数不一致: backupId={}, 差异表 {} 张", backup.getId(), mismatches.size());
            }
            restoreRepository.update(record);
        } catch (Exception e) {
            log.error("恢复演练失败: id={}", id, e);
            markFailed(record, e);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    /**
     * 分页查询演练记录。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 演练记录列表
     */
    public List<RestoreRecord> findPage(final Long backupId, final Integer pageNum, final Integer pageSize) {
        int safePage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        return restoreRepository.findPage(backupId, (safePage - 1) * safeSize, safeSize);
    }

    /**
     * 统计演练记录数。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @return 记录数
     */
    public long count(final Long backupId) {
        return restoreRepository.count(backupId);
    }

    /**
     * 按 ID 查询演练记录。
     *
     * @param id 记录 ID
     * @return 演练记录
     * @throws BusinessException 记录不存在时抛出
     */
    public RestoreRecord getById(final Long id) {
        return restoreRepository.findById(id)
                .orElseThrow(() -> new BusinessException("演练记录不存在: " + id));
    }

    private List<TableCountMismatch> compare(final Map<String, Long> expected, final Map<String, Long> actual) {
        Set<String> tables = new TreeSet<>();
        if (expected != null) {
            tables.addAll(expected.keySet());
        }
        if (actual != null) {
            tables.addAll(actual.keySet());
        }
        List<TableCountMismatch> mismatches = new ArrayList<>();
        for (String table : tables) {
            long expectedCount = expected == null ? 0L : expected.getOrDefault(table, 0L);
            long actualCount = actual == null ? 0L : actual.getOrDefault(table, 0L);
            if (expectedCount != actualCount) {
                mismatches.add(new TableCountMismatch(table, expectedCount, actualCount));
            }
        }
        return mismatches;
    }

    private void markFailed(final RestoreRecord record, final Throwable error) {
        try {
            record.markFailed(BackupErrorSanitizer.sanitize(error, properties.getDatabase().getPassword(),
                    properties.getEncryptionKey()), LocalDateTime.now(clock));
            restoreRepository.update(record);
        } catch (Exception updateError) {
            log.error("更新演练失败状态失败: id={}", record.getId(), updateError);
        }
    }

    private void deleteQuietly(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除演练临时文件失败: {}", path, e);
        }
    }
}
