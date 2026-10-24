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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 备份记录持久化对象。
 *
 * <p>与 {@code db_backup_record} 一一对应；枚举与行数快照以字符串/JSON 存储，
 * 由仓储实现负责与领域模型转换。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupRecordPO {

    /**
     * 记录 ID。
     */
    private Long id;

    /**
     * 备份编号。
     */
    private String backupNo;

    /**
     * 触发方式。
     */
    private String triggerType;

    /**
     * 状态。
     */
    private String status;

    /**
     * 源数据库名。
     */
    private String databaseName;

    /**
     * 存储类型。
     */
    private String storageType;

    /**
     * 存储路径。
     */
    private String storagePath;

    /**
     * 密文大小。
     */
    private Long ciphertextSize;

    /**
     * 明文大小。
     */
    private Long plaintextSize;

    /**
     * 明文校验和。
     */
    private String checksumSha256;

    /**
     * 加密算法。
     */
    private String encryption;

    /**
     * 导出工具。
     */
    private String dumpTool;

    /**
     * 表数量。
     */
    private Integer tableCount;

    /**
     * 逐表行数快照 JSON。
     */
    private String tableStatsJson;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime finishedAt;

    /**
     * 耗时。
     */
    private Long durationMs;

    /**
     * 错误信息。
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

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getBackupNo() {
        return backupNo;
    }

    public void setBackupNo(final String backupNo) {
        this.backupNo = backupNo;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(final String triggerType) {
        this.triggerType = triggerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(final String storageType) {
        this.storageType = storageType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(final String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getCiphertextSize() {
        return ciphertextSize;
    }

    public void setCiphertextSize(final Long ciphertextSize) {
        this.ciphertextSize = ciphertextSize;
    }

    public Long getPlaintextSize() {
        return plaintextSize;
    }

    public void setPlaintextSize(final Long plaintextSize) {
        this.plaintextSize = plaintextSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(final String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(final String encryption) {
        this.encryption = encryption;
    }

    public String getDumpTool() {
        return dumpTool;
    }

    public void setDumpTool(final String dumpTool) {
        this.dumpTool = dumpTool;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(final Integer tableCount) {
        this.tableCount = tableCount;
    }

    public String getTableStatsJson() {
        return tableStatsJson;
    }

    public void setTableStatsJson(final String tableStatsJson) {
        this.tableStatsJson = tableStatsJson;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(final LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(final LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
