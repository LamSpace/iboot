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

package com.iboot.admin.interfaces.dto.response;

import com.iboot.admin.domain.backup.model.BackupRecord;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 备份记录响应。
 *
 * <p>只暴露备份状态、产物位置与行数快照；不包含数据库密码或主密钥。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupRecordResponse {

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
     * 逐表行数快照。
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
     * 耗时。
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
     * 从领域模型创建响应。
     *
     * @param record 备份记录
     * @return 响应对象
     */
    public static BackupRecordResponse from(final BackupRecord record) {
        BackupRecordResponse response = new BackupRecordResponse();
        response.id = record.getId();
        response.backupNo = record.getBackupNo();
        response.triggerType = record.getTriggerType() == null ? null : record.getTriggerType().name();
        response.status = record.getStatus() == null ? null : record.getStatus().name();
        response.databaseName = record.getDatabaseName();
        response.storageType = record.getStorageType();
        response.storagePath = record.getStoragePath();
        response.ciphertextSize = record.getCiphertextSize();
        response.plaintextSize = record.getPlaintextSize();
        response.checksumSha256 = record.getChecksumSha256();
        response.encryption = record.getEncryption();
        response.dumpTool = record.getDumpTool();
        response.tableCount = record.getTableCount();
        response.tableStats = record.getTableStats();
        response.startedAt = record.getStartedAt();
        response.finishedAt = record.getFinishedAt();
        response.durationMs = record.getDurationMs();
        response.errorMessage = record.getErrorMessage();
        response.createdBy = record.getCreatedBy();
        response.createTime = record.getCreateTime();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getBackupNo() {
        return backupNo;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getStatus() {
        return status;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Long getCiphertextSize() {
        return ciphertextSize;
    }

    public Long getPlaintextSize() {
        return plaintextSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getEncryption() {
        return encryption;
    }

    public String getDumpTool() {
        return dumpTool;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public Map<String, Long> getTableStats() {
        return tableStats;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}
