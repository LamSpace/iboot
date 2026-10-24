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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.backup.BackupApplicationService;
import com.iboot.admin.application.service.backup.RestoreDrillService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.response.BackupRecordResponse;
import com.iboot.admin.interfaces.dto.response.BackupSummaryResponse;
import com.iboot.admin.interfaces.dto.response.RestoreRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据备份管理控制器。
 *
 * <p>提供备份记录查询、手动备份、恢复演练与概览。恢复目标由服务端固定为演练库，
 * 接口不接受客户端传入目标库名；写操作记录操作日志。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupApplicationService
 * @see RestoreDrillService
 */
@Tag(name = "数据备份", description = "数据库逻辑备份与恢复演练")
@RestController
@RequestMapping("/api/monitor/backup")
public class BackupController {

    private final BackupApplicationService backupApplicationService;

    private final RestoreDrillService restoreDrillService;

    @SuppressWarnings("all")
    public BackupController(final BackupApplicationService backupApplicationService,
                            final RestoreDrillService restoreDrillService) {
        this.backupApplicationService = backupApplicationService;
        this.restoreDrillService = restoreDrillService;
    }

    /**
     * 分页查询备份记录。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param status   状态筛选，可为 {@code null}
     * @return 备份记录分页结果
     */
    @Operation(summary = "查询备份记录")
    @GetMapping(version = "1", value = "/records")
    @PreAuthorize("hasAuthority('backup:record:list')")
    public Result<PageResult<BackupRecordResponse>> listRecords(
            @RequestParam(defaultValue = "1") final Integer pageNum,
            @RequestParam(defaultValue = "10") final Integer pageSize,
            @RequestParam(required = false) final String status) {
        List<BackupRecord> records = backupApplicationService.findPage(status, pageNum, pageSize);
        long total = backupApplicationService.count(status);
        List<BackupRecordResponse> rows = records.stream().map(BackupRecordResponse::from).toList();
        return Result.success(new PageResult<>(rows, total, pageNum, pageSize));
    }

    /**
     * 查询单条备份记录。
     *
     * @param id 备份记录 ID
     * @return 备份记录
     */
    @Operation(summary = "查询备份记录详情")
    @GetMapping(version = "1", value = "/records/{id}")
    @PreAuthorize("hasAuthority('backup:record:list')")
    public Result<BackupRecordResponse> getRecord(@PathVariable final Long id) {
        return Result.success(BackupRecordResponse.from(backupApplicationService.getById(id)));
    }

    /**
     * 手动触发一次备份。
     *
     * @return 运行中的备份记录
     */
    @Operation(summary = "立即备份")
    @PostMapping(version = "1", value = "/records")
    @PreAuthorize("hasAuthority('backup:record:create')")
    @Log(title = "数据备份", businessType = BusinessTypeEnum.OTHER)
    public Result<BackupRecordResponse> createBackup() {
        BackupRecord record = backupApplicationService.triggerManual(SecurityUtils.getCurrentUsername());
        return Result.success(BackupRecordResponse.from(record));
    }

    /**
     * 分页查询恢复演练记录。
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @return 演练记录分页结果
     */
    @Operation(summary = "查询恢复演练记录")
    @GetMapping(version = "1", value = "/restores")
    @PreAuthorize("hasAuthority('backup:record:list')")
    public Result<PageResult<RestoreRecordResponse>> listRestores(
            @RequestParam(defaultValue = "1") final Integer pageNum,
            @RequestParam(defaultValue = "10") final Integer pageSize,
            @RequestParam(required = false) final Long backupId) {
        List<RestoreRecord> records = restoreDrillService.findPage(backupId, pageNum, pageSize);
        long total = restoreDrillService.count(backupId);
        List<RestoreRecordResponse> rows = records.stream().map(RestoreRecordResponse::from).toList();
        return Result.success(new PageResult<>(rows, total, pageNum, pageSize));
    }

    /**
     * 查询单条演练记录。
     *
     * @param id 演练记录 ID
     * @return 演练记录
     */
    @Operation(summary = "查询恢复演练详情")
    @GetMapping(version = "1", value = "/restores/{id}")
    @PreAuthorize("hasAuthority('backup:record:list')")
    public Result<RestoreRecordResponse> getRestore(@PathVariable final Long id) {
        return Result.success(RestoreRecordResponse.from(restoreDrillService.getById(id)));
    }

    /**
     * 对指定备份发起恢复演练。
     *
     * @param id 备份记录 ID
     * @return 运行中的演练记录
     */
    @Operation(summary = "恢复演练")
    @PostMapping(version = "1", value = "/records/{id}/restore")
    @PreAuthorize("hasAuthority('backup:record:restore')")
    @Log(title = "数据备份", businessType = BusinessTypeEnum.OTHER)
    public Result<RestoreRecordResponse> restore(@PathVariable final Long id) {
        RestoreRecord record = restoreDrillService.trigger(id, SecurityUtils.getCurrentUsername());
        return Result.success(RestoreRecordResponse.from(record));
    }

    /**
     * 查询备份概览。
     *
     * @return 功能开关、总数、总占用与最近成功时间
     */
    @Operation(summary = "查询备份概览")
    @GetMapping(version = "1", value = "/summary")
    @PreAuthorize("hasAuthority('backup:record:list')")
    public Result<BackupSummaryResponse> summary() {
        return Result.success(BackupSummaryResponse.from(backupApplicationService.summary()));
    }
}
