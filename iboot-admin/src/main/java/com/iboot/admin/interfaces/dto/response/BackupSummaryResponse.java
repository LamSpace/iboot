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

import com.iboot.admin.application.service.backup.BackupSummary;

import java.time.LocalDateTime;

/**
 * 备份概览响应。
 *
 * <p>用于管理端展示功能开关、备份总数、存储占用与最近成功时间。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupSummaryResponse {

    /**
     * 功能是否启用。
     */
    private boolean enabled;

    /**
     * 备份记录总数。
     */
    private long totalCount;

    /**
     * 成功备份的密文总大小。
     */
    private long totalSize;

    /**
     * 最近一次成功备份的结束时间。
     */
    private LocalDateTime lastSuccessAt;

    /**
     * 从应用层概览创建响应。
     *
     * @param summary 备份概览
     * @return 响应对象
     */
    public static BackupSummaryResponse from(final BackupSummary summary) {
        BackupSummaryResponse response = new BackupSummaryResponse();
        response.enabled = summary.enabled();
        response.totalCount = summary.totalCount();
        response.totalSize = summary.totalSize();
        response.lastSuccessAt = summary.lastSuccessAt();
        return response;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public LocalDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }
}
