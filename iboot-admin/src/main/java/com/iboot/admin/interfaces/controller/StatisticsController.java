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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.StatisticsApplicationService;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.interfaces.dto.response.StatisticsResponse.ReportResponse;
import com.iboot.admin.interfaces.dto.response.StatisticsResponse.UsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 统计分析控制器
 *
 * @author iBoot
 */
@Tag(name = "统计分析", description = "统计分析相关接口")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsApplicationService statisticsApplicationService;

    @SuppressWarnings("all")
    public StatisticsController(final StatisticsApplicationService statisticsApplicationService) {
        this.statisticsApplicationService = statisticsApplicationService;
    }

    @Operation(summary = "获取数据统计报表")
    @GetMapping("/report")
    public Result<ReportResponse> getReport(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(
                    pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(
                    pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 默认最近7天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        ReportResponse data = statisticsApplicationService.getReportStatistics(startTime, endTime);
        return Result.success(data);
    }

    @Operation(summary = "获取系统使用分析")
    @GetMapping("/usage")
    public Result<UsageResponse> getUsageAnalysis(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(
                    pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(
                    pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 默认最近30天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(29);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
        UsageResponse data = statisticsApplicationService.getUsageAnalysis(startTime, endTime);
        return Result.success(data);
    }

}
