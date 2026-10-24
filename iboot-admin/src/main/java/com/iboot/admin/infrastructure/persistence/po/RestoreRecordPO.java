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
 * 恢复演练记录持久化对象。
 *
 * <p>与 {@code db_restore_record} 一一对应；逐表行数与差异明细以 JSON 存储。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class RestoreRecordPO {

    /**
     * 记录 ID。
     */
    private Long id;

    /**
     * 备份记录 ID。
     */
    private Long backupId;

    /**
     * 演练库名。
     */
    private String targetSchema;

    /**
     * 状态。
     */
    private String status;

    /**
     * 比对表数量。
     */
    private Integer tableCount;

    /**
     * 还原后逐表行数 JSON。
     */
    private String tableStatsJson;

    /**
     * 差异明细 JSON。
     */
    private String mismatchJson;

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

    public Long getBackupId() {
        return backupId;
    }

    public void setBackupId(final Long backupId) {
        this.backupId = backupId;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public void setTargetSchema(final String targetSchema) {
        this.targetSchema = targetSchema;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
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

    public String getMismatchJson() {
        return mismatchJson;
    }

    public void setMismatchJson(final String mismatchJson) {
        this.mismatchJson = mismatchJson;
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
