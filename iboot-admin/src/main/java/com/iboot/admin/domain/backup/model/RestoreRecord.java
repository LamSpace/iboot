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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 恢复演练记录。
 *
 * <p>记录一次演练的目标库、还原后的逐表行数与差异明细。运行中记录只允许迁移到
 * 成功、不一致或失败；不一致状态代表还原完成但行数快照不匹配，必须如实保留差异，
 * 不得当作成功处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class RestoreRecord {

    /**
     * 演练记录 ID。
     */
    private Long id;

    /**
     * 关联的备份记录 ID。
     */
    private Long backupId;

    /**
     * 演练库名。
     */
    private String targetSchema;

    /**
     * 当前状态。
     */
    private RestoreStatus status;

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
     * 创建一条运行中的演练记录。
     *
     * @param backupId     备份记录 ID
     * @param targetSchema 演练库名
     * @param createdBy    触发人
     * @param now          开始时间
     * @return 运行中的演练记录
     */
    public static RestoreRecord start(final Long backupId, final String targetSchema, final String createdBy,
                                      final LocalDateTime now) {
        RestoreRecord record = new RestoreRecord();
        record.backupId = backupId;
        record.targetSchema = targetSchema;
        record.status = RestoreStatus.RUNNING;
        record.startedAt = now;
        record.createdBy = createdBy;
        record.createTime = now;
        record.updateTime = now;
        return record;
    }

    /**
     * 标记演练成功。
     *
     * @param tableStats 还原后的逐表行数
     * @param now        结束时间
     * @throws IllegalStateException 记录不处于运行中时抛出
     */
    public void markSuccess(final Map<String, Long> tableStats, final LocalDateTime now) {
        requireRunning();
        this.status = RestoreStatus.SUCCESS;
        this.tableStats = tableStats == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableStats);
        this.tableCount = this.tableStats.size();
        this.mismatches = new ArrayList<>();
        finish(now);
    }

    /**
     * 标记演练完成但行数不一致。
     *
     * @param tableStats 还原后的逐表行数
     * @param mismatches 差异明细
     * @param now        结束时间
     * @throws IllegalStateException 记录不处于运行中时抛出
     */
    public void markMismatch(final Map<String, Long> tableStats, final List<TableCountMismatch> mismatches,
                             final LocalDateTime now) {
        requireRunning();
        this.status = RestoreStatus.MISMATCH;
        this.tableStats = tableStats == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableStats);
        this.tableCount = this.tableStats.size();
        this.mismatches = mismatches == null ? new ArrayList<>() : new ArrayList<>(mismatches);
        finish(now);
    }

    /**
     * 标记演练失败。
     *
     * @param error 脱敏后的错误信息
     * @param now   结束时间
     * @throws IllegalStateException 记录不处于运行中时抛出
     */
    public void markFailed(final String error, final LocalDateTime now) {
        requireRunning();
        this.status = RestoreStatus.FAILED;
        this.errorMessage = error;
        finish(now);
    }

    /**
     * 判断记录是否处于运行中。
     *
     * @return 运行中返回 {@code true}
     */
    public boolean isRunning() {
        return status == RestoreStatus.RUNNING;
    }

    private void finish(final LocalDateTime now) {
        this.finishedAt = now;
        this.durationMs = Duration.between(startedAt, now).toMillis();
        this.updateTime = now;
    }

    private void requireRunning() {
        if (status != RestoreStatus.RUNNING) {
            throw new IllegalStateException("只有运行中的演练记录可以变更状态，当前状态: " + status);
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
     * 获取备份记录 ID。
     *
     * @return 备份记录 ID
     */
    public Long getBackupId() {
        return backupId;
    }

    /**
     * 设置备份记录 ID。
     *
     * @param backupId 备份记录 ID
     */
    public void setBackupId(final Long backupId) {
        this.backupId = backupId;
    }

    /**
     * 获取演练库名。
     *
     * @return 演练库名
     */
    public String getTargetSchema() {
        return targetSchema;
    }

    /**
     * 设置演练库名。
     *
     * @param targetSchema 演练库名
     */
    public void setTargetSchema(final String targetSchema) {
        this.targetSchema = targetSchema;
    }

    /**
     * 获取状态。
     *
     * @return 状态
     */
    public RestoreStatus getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态
     */
    public void setStatus(final RestoreStatus status) {
        this.status = status;
    }

    /**
     * 获取比对表数量。
     *
     * @return 表数量
     */
    public Integer getTableCount() {
        return tableCount;
    }

    /**
     * 设置比对表数量。
     *
     * @param tableCount 表数量
     */
    public void setTableCount(final Integer tableCount) {
        this.tableCount = tableCount;
    }

    /**
     * 获取还原后的逐表行数。
     *
     * @return 表名到行数的映射
     */
    public Map<String, Long> getTableStats() {
        return tableStats;
    }

    /**
     * 设置还原后的逐表行数。
     *
     * @param tableStats 表名到行数的映射
     */
    public void setTableStats(final Map<String, Long> tableStats) {
        this.tableStats = tableStats == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tableStats);
    }

    /**
     * 获取行数差异明细。
     *
     * @return 差异列表
     */
    public List<TableCountMismatch> getMismatches() {
        return mismatches;
    }

    /**
     * 设置行数差异明细。
     *
     * @param mismatches 差异列表
     */
    public void setMismatches(final List<TableCountMismatch> mismatches) {
        this.mismatches = mismatches == null ? new ArrayList<>() : new ArrayList<>(mismatches);
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
