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

import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.TableCountMismatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 恢复演练记录响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class RestoreRecordResponse {

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
     * 还原后的逐表行数。
     */
    private Map<String, Long> tableStats = new LinkedHashMap<>();

    /**
     * 行数差异明细。
     */
    private List<TableCountMismatch> mismatches = new ArrayList<>();

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
     * @param record 演练记录
     * @return 响应对象
     */
    public static RestoreRecordResponse from(final RestoreRecord record) {
        RestoreRecordResponse response = new RestoreRecordResponse();
        response.id = record.getId();
        response.backupId = record.getBackupId();
        response.targetSchema = record.getTargetSchema();
        response.status = record.getStatus() == null ? null : record.getStatus().name();
        response.tableCount = record.getTableCount();
        response.tableStats = record.getTableStats();
        response.mismatches = record.getMismatches();
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

    public Long getBackupId() {
        return backupId;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public String getStatus() {
        return status;
    }

    public Integer getTableCount() {
        return tableCount;
    }

    public Map<String, Long> getTableStats() {
        return tableStats;
    }

    public List<TableCountMismatch> getMismatches() {
        return mismatches;
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
