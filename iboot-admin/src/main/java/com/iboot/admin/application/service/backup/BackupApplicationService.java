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
import com.iboot.admin.domain.backup.repository.BackupRecordRepository;
import com.iboot.admin.domain.backup.service.BackupExecutor;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupErrorSanitizer;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import com.iboot.admin.infrastructure.backup.CountingOutputStream;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 备份应用服务。
 *
 * <p>负责备份触发的并发闸门、运行中记录重置、导出→校验和→加密→临时文件→存储的流水线，
 * 以及成功后元数据与逐表行数快照的落库。执行过程异步，接口立即返回记录标识；
 * 任何失败都会把记录置为失败并保存脱敏错误，不留下可演练的半成品。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupExecutor
 * @see BackupCipher
 */
@Service
public class BackupApplicationService {

    private static final Logger log = LoggerFactory.getLogger(BackupApplicationService.class);

    private static final DateTimeFormatter BACKUP_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter PATH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final int MAX_PAGE_SIZE = 100;

    private final BackupProperties properties;

    private final BackupRecordRepository backupRepository;

    private final BackupExecutor backupExecutor;

    private final BackupCipher backupCipher;

    private final FileStorageStrategy fileStorageStrategy;

    private final AsyncTaskExecutor taskExecutor;

    private final Clock clock;

    private final ReentrantLock runningLock = new ReentrantLock();

    @SuppressWarnings("all")
    public BackupApplicationService(final BackupProperties properties,
                                    final BackupRecordRepository backupRepository,
                                    final BackupExecutor backupExecutor,
                                    final BackupCipher backupCipher,
                                    final FileStorageStrategy fileStorageStrategy,
                                    @Qualifier("taskExecutor") final AsyncTaskExecutor taskExecutor,
                                    final Clock clock) {
        this.properties = properties;
        this.backupRepository = backupRepository;
        this.backupExecutor = backupExecutor;
        this.backupCipher = backupCipher;
        this.fileStorageStrategy = fileStorageStrategy;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    /**
     * 手动触发一次备份。
     *
     * @param operator 触发人
     * @return 运行中的备份记录
     * @throws BusinessException 功能未启用或已有备份正在执行时抛出
     */
    public BackupRecord triggerManual(final String operator) {
        return start(BackupTriggerType.MANUAL, operator);
    }

    /**
     * 定时触发一次备份。
     *
     * <p>功能关闭或已有备份运行时只记录日志，不向 Quartz 抛出异常，避免任务日志被噪音填满。
     */
    public void triggerScheduled() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            start(BackupTriggerType.SCHEDULED, "system");
        } catch (BusinessException e) {
            log.warn("定时备份未启动: {}", e.getMessage());
        }
    }

    /**
     * 执行指定备份记录。
     *
     * <p>异步入口，也可由测试直接调用。记录不处于运行中时直接返回；执行失败时更新为失败状态。
     *
     * @param id 备份记录 ID
     */
    public void execute(final Long id) {
        Optional<BackupRecord> found = backupRepository.findById(id);
        if (found.isEmpty()) {
            log.warn("备份记录不存在，忽略执行: {}", id);
            return;
        }
        BackupRecord record = found.get();
        if (!record.isRunning()) {
            log.warn("备份记录不处于运行中，忽略执行: {}", id);
            return;
        }
        Path tempFile = null;
        try {
            var tableStats = backupExecutor.tableRowCounts(properties.getDatabase().getName());
            tempFile = Files.createTempFile("iboot-backup-", ".sql.enc");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long plaintextSize = 0L;
            try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(tempFile))) {
                try (OutputStream encrypting = backupCipher.encrypting(fileOut)) {
                    CountingOutputStream counter = new CountingOutputStream(encrypting);
                    DigestOutputStream digestOut = new DigestOutputStream(counter, digest);
                    backupExecutor.dump(digestOut);
                    digestOut.flush();
                    plaintextSize = counter.getCount();
                }
            }
            long ciphertextSize = Files.size(tempFile);
            String storagePath = storagePath(record);
            String fileName = record.getBackupNo() + ".sql.enc";
            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(tempFile))) {
                fileStorageStrategy.save(new StorageObject(inputStream, ciphertextSize,
                        "application/octet-stream", fileName), storagePath);
            }
            String checksum = HexFormat.of().formatHex(digest.digest());
            record.markSuccess(fileStorageStrategy.getStorageType(), storagePath, ciphertextSize, plaintextSize,
                    checksum, tableStats, LocalDateTime.now(clock));
            backupRepository.update(record);
            log.info("备份完成: id={}, path={}, size={}", record.getId(), storagePath, ciphertextSize);
        } catch (Exception e) {
            log.error("备份执行失败: id={}", id, e);
            markFailed(record, e);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    /**
     * 分页查询备份记录。
     *
     * @param status   状态筛选，可为 {@code null}
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 备份记录列表
     */
    public List<BackupRecord> findPage(final String status, final Integer pageNum, final Integer pageSize) {
        int safePage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        return backupRepository.findPage(status, (safePage - 1) * safeSize, safeSize);
    }

    /**
     * 统计备份记录数。
     *
     * @param status 状态筛选，可为 {@code null}
     * @return 记录数
     */
    public long count(final String status) {
        return backupRepository.count(status);
    }

    /**
     * 按 ID 查询备份记录。
     *
     * @param id 记录 ID
     * @return 备份记录
     * @throws BusinessException 记录不存在时抛出
     */
    public BackupRecord getById(final Long id) {
        return backupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("备份记录不存在: " + id));
    }

    /**
     * 获取备份概览。
     *
     * @return 功能开关、记录总数、总占用与最近成功时间
     */
    public BackupSummary summary() {
        return new BackupSummary(properties.isEnabled(), backupRepository.countAll(),
                backupRepository.sumCiphertextSize(), backupRepository.lastSuccessAt());
    }

    private BackupRecord start(final BackupTriggerType triggerType, final String operator) {
        if (!properties.isEnabled()) {
            throw new BusinessException("备份功能未启用");
        }
        if (!runningLock.tryLock()) {
            throw new BusinessException("已有备份正在执行");
        }
        boolean submitted = false;
        try {
            LocalDateTime now = LocalDateTime.now(clock);
            int reset = backupRepository.resetStaleRunning(now.minusMinutes(properties.getStaleRunningMinutes()), now);
            if (reset > 0) {
                log.warn("重置超时备份记录 {} 条", reset);
            }
            if (backupRepository.findRunning().isPresent()) {
                throw new BusinessException("已有备份正在执行");
            }
            BackupRecord record = BackupRecord.start(newBackupNo(now), triggerType,
                    properties.getDatabase().getName(), operator, now);
            backupRepository.insert(record);
            Runnable task = () -> {
                try {
                    execute(record.getId());
                } catch (Throwable t) {
                    log.error("备份异步执行异常: id={}", record.getId(), t);
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

    private void markFailed(final BackupRecord record, final Throwable error) {
        try {
            record.markFailed(BackupErrorSanitizer.sanitize(error, properties.getDatabase().getPassword(),
                    properties.getEncryptionKey()), LocalDateTime.now(clock));
            backupRepository.update(record);
        } catch (Exception updateError) {
            log.error("更新备份失败状态失败: id={}", record.getId(), updateError);
        }
    }

    private String newBackupNo(final LocalDateTime now) {
        return "B" + now.format(BACKUP_NO_FORMAT) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String storagePath(final BackupRecord record) {
        LocalDate today = LocalDate.now(clock);
        return "backup/" + record.getDatabaseName() + "/" + today.format(PATH_DATE_FORMAT)
                + "/" + record.getBackupNo() + ".sql.enc";
    }

    private void deleteQuietly(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除备份临时文件失败: {}", path, e);
        }
    }
}
