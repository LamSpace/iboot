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

package com.iboot.admin.domain.backup.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据库备份记录。
 *
 * <p>记录一次备份从开始到结束的完整状态：运行中记录只允许迁移到成功或失败；
 * 成功记录保存加密产物位置、明文校验和以及备份时刻的逐表行数快照，供恢复演练比对。
 * 失败记录不得保留可用的存储路径。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupRecord {

    /**
     * 备份记录 ID。
     */
    private Long id;

    /**
     * 备份编号，用于对象键命名。
     */
    private String backupNo;

    /**
     * 触发方式。
     */
    private BackupTriggerType triggerType;

    /**
     * 当前状态。
     */
    private BackupStatus status;

    /**
     * 源数据库名。
     */
    private String databaseName;

    /**
     * 存储类型。
     */
    private String storageType;

    /**
     * 加密备份对象路径。
     */
    private String storagePath;

    /**
     * 密文大小（字节）。
     */
    private Long ciphertextSize;

    /**
     * 明文大小（字节）。
     */
    private Long plaintextSize;

    /**
     * 明文 SHA-256。
     */
    private String checksumSha256;

    /**
     * 加密算法标识。
     */
    private String encryption;

    /**
     * 导出工具标识。
     */
    private String dumpTool;

    /**
     * 表数量。
     */
    private Integer tableCount;

    /**
     * 备份时刻的逐表行数快照。
     */
    private Map<String, Long> tableStats = new LinkedHashMap<>();

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime finishedAt;

    /**
     * 耗时（毫秒）。
     */
    private Long durationMs;

    /**
     * 脱敏后的错误信息。
     */
    private String errorMessage;

    /**
     * 触发人。
     */
    private String createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 创建一条运行中的备份记录。
     *
     * @param backupNo     备份编号
     * @param triggerType  触发方式
     * @param databaseName 源数据库名
     * @param createdBy    触发人
     * @param now          开始时间
     * @return 运行中的备份记录
     */
    public static BackupRecord start(final String backupNo, final BackupTriggerType triggerType,
                                     final String databaseName, final String createdBy, final LocalDateTime now) {
        BackupRecord record = new BackupRecord();
        record.backupNo = backupNo;
        record.triggerType = triggerType;
        record.status = BackupStatus.RUNNING;
        record.databaseName = databaseName;
        record.encryption = "AES-256-GCM";
        record.dumpTool = "mysqldump";
        record.startedAt = now;
        record.createdBy = createdBy;
        record.createTime = now;
        record.updateTime = now;
        return record;
    }

    /**
     * 标记备份成功。
     *
     * @param storageType     存储类型
     * @param storagePath     加密对象路径
     * @param ciphertextSize  密文大小
     * @param plaintextSize   明文大小
     * @param checksumSha256  明文校验和
     * @param tableStats      逐表行数快照
     * @param now             结束时间
     * @throws IllegalStateException 记录不处于运行中时抛出
     */
    public void markSuccess(final String storageType, final String storagePath, final long ciphertextSize,
                            final long plaintextSize, final String checksumSha256,
                            final Map<String, Long> tableStats, final LocalDateTime now) {
        requireRunning();
        this.status = BackupStatus.SUCCESS;
        this.storageType = storageType;
        this.storagePath = storagePath;
        this.ciphertextSize = ciphertextSize;
        this.plaintextSize = plaintextSize;
        this.checksumSha256 = checksumSha256;
        this.tableStats = tableStats == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableStats);
        this.tableCount = this.tableStats.size();
        this.finishedAt = now;
        this.durationMs = Duration.between(startedAt, now).toMillis();
        this.errorMessage = null;
        this.updateTime = now;
    }

    /**
     * 标记备份失败。
     *
     * @param error 脱敏后的错误信息
     * @param now   结束时间
     * @throws IllegalStateException 记录不处于运行中时抛出
     */
    public void markFailed(final String error, final LocalDateTime now) {
        requireRunning();
        this.status = BackupStatus.FAILED;
        this.errorMessage = error;
        this.finishedAt = now;
        this.durationMs = Duration.between(startedAt, now).toMillis();
        this.updateTime = now;
    }

    /**
     * 判断记录是否处于运行中。
     *
     * @return 运行中返回 {@code true}
     */
    public boolean isRunning() {
        return status == BackupStatus.RUNNING;
    }

    /**
     * 判断记录是否可用于恢复演练。
     *
     * @return 状态成功且存在存储路径时返回 {@code true}
     */
    public boolean isRestorable() {
        return status == BackupStatus.SUCCESS && storagePath != null && !storagePath.isBlank();
    }

    private void requireRunning() {
        if (status != BackupStatus.RUNNING) {
            throw new IllegalStateException("只有运行中的备份记录可以变更状态，当前状态: " + status);
        }
    }

    /**
     * 获取记录 ID。
     *
     * @return 记录 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置记录 ID。
     *
     * @param id 记录 ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 获取备份编号。
     *
     * @return 备份编号
     */
    public String getBackupNo() {
        return backupNo;
    }

    /**
     * 设置备份编号。
     *
     * @param backupNo 备份编号
     */
    public void setBackupNo(final String backupNo) {
        this.backupNo = backupNo;
    }

    /**
     * 获取触发方式。
     *
     * @return 触发方式
     */
    public BackupTriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * 设置触发方式。
     *
     * @param triggerType 触发方式
     */
    public void setTriggerType(final BackupTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    /**
     * 获取状态。
     *
     * @return 状态
     */
    public BackupStatus getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态
     */
    public void setStatus(final BackupStatus status) {
        this.status = status;
    }

    /**
     * 获取源数据库名。
     *
     * @return 数据库名
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * 设置源数据库名。
     *
     * @param databaseName 数据库名
     */
    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * 获取存储类型。
     *
     * @return 存储类型
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * 设置存储类型。
     *
     * @param storageType 存储类型
     */
    public void setStorageType(final String storageType) {
        this.storageType = storageType;
    }

    /**
     * 获取存储路径。
     *
     * @return 对象路径
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 设置存储路径。
     *
     * @param storagePath 对象路径
     */
    public void setStoragePath(final String storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * 获取密文大小。
     *
     * @return 字节数
     */
    public Long getCiphertextSize() {
        return ciphertextSize;
    }

    /**
     * 设置密文大小。
     *
     * @param ciphertextSize 字节数
     */
    public void setCiphertextSize(final Long ciphertextSize) {
        this.ciphertextSize = ciphertextSize;
    }

    /**
     * 获取明文大小。
     *
     * @return 字节数
     */
    public Long getPlaintextSize() {
        return plaintextSize;
    }

    /**
     * 设置明文大小。
     *
     * @param plaintextSize 字节数
     */
    public void setPlaintextSize(final Long plaintextSize) {
        this.plaintextSize = plaintextSize;
    }

    /**
     * 获取明文校验和。
     *
     * @return SHA-256
     */
    public String getChecksumSha256() {
        return checksumSha256;
    }

    /**
     * 设置明文校验和。
     *
     * @param checksumSha256 SHA-256
     */
    public void setChecksumSha256(final String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    /**
     * 获取加密算法。
     *
     * @return 算法标识
     */
    public String getEncryption() {
        return encryption;
    }

    /**
     * 设置加密算法。
     *
     * @param encryption 算法标识
     */
    public void setEncryption(final String encryption) {
        this.encryption = encryption;
    }

    /**
     * 获取导出工具。
     *
     * @return 工具标识
     */
    public String getDumpTool() {
        return dumpTool;
    }

    /**
     * 设置导出工具。
     *
     * @param dumpTool 工具标识
     */
    public void setDumpTool(final String dumpTool) {
        this.dumpTool = dumpTool;
    }

    /**
     * 获取表数量。
     *
     * @return 表数量
     */
    public Integer getTableCount() {
        return tableCount;
    }

    /**
     * 设置表数量。
     *
     * @param tableCount 表数量
     */
    public void setTableCount(final Integer tableCount) {
        this.tableCount = tableCount;
    }

    /**
     * 获取逐表行数快照。
     *
     * @return 表名到行数的映射
     */
    public Map<String, Long> getTableStats() {
        return tableStats;
    }

    /**
     * 设置逐表行数快照。
     *
     * @param tableStats 表名到行数的映射
     */
    public void setTableStats(final Map<String, Long> tableStats) {
        this.tableStats = tableStats == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableStats);
    }

    /**
     * 获取开始时间。
     *
     * @return 开始时间
     */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /**
     * 设置开始时间。
     *
     * @param startedAt 开始时间
     */
    public void setStartedAt(final LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     * 获取结束时间。
     *
     * @return 结束时间
     */
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    /**
     * 设置结束时间。
     *
     * @param finishedAt 结束时间
     */
    public void setFinishedAt(final LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    /**
     * 获取耗时。
     *
     * @return 毫秒数
     */
    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * 设置耗时。
     *
     * @param durationMs 毫秒数
     */
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 获取错误信息。
     *
     * @return 脱敏后的错误
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 设置错误信息。
     *
     * @param errorMessage 脱敏后的错误
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 获取触发人。
     *
     * @return 触发人
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置触发人。
     *
     * @param createdBy 触发人
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
