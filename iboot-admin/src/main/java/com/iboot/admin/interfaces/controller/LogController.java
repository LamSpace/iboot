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

import com.iboot.admin.application.service.LoginLogApplicationService;
import com.iboot.admin.application.service.OperateLogApplicationService;
import com.iboot.admin.application.service.RunLogApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.LoginLog;
import com.iboot.admin.domain.system.model.OperateLog;
import com.iboot.admin.interfaces.dto.export.LoginLogExportVO;
import com.iboot.admin.interfaces.dto.export.OperateLogExportVO;
import com.iboot.admin.interfaces.dto.response.LoginLogResponse;
import com.iboot.admin.interfaces.dto.response.OperateLogResponse;
import com.iboot.admin.interfaces.dto.response.RunLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志管理控制器
 *
 * @author iBoot
 */
@Tag(name = "日志管理", description = "日志管理相关接口")
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final LoginLogApplicationService loginLogApplicationService;

    private final OperateLogApplicationService operateLogApplicationService;

    private final RunLogApplicationService runLogApplicationService;

    @SuppressWarnings("all")
    public LogController(final LoginLogApplicationService loginLogApplicationService,
                         final OperateLogApplicationService operateLogApplicationService,
                         final RunLogApplicationService runLogApplicationService) {
        this.loginLogApplicationService = loginLogApplicationService;
        this.operateLogApplicationService = operateLogApplicationService;
        this.runLogApplicationService = runLogApplicationService;
    }

    // ==================== 登录日志接口 ====================
    @Operation(summary = "查询登录日志列表")
    @GetMapping(version = "1", value = "/login/list")
    @PreAuthorize("hasAuthority('loginlog:list')")
    public Result<PageResult<LoginLogResponse>> loginLogList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             @RequestParam(required = false) String username,
                                                             @RequestParam(required = false) String ipAddress,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false)
                                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                             LocalDateTime beginTime,
                                                             @RequestParam(required = false)
                                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                             LocalDateTime endTime) {
        List<LoginLog> logs;
        long total;
        if (username != null || ipAddress != null || status != null || beginTime != null || endTime != null) {
            logs = loginLogApplicationService.getLoginLogPageByCondition(username, ipAddress, status, beginTime,
                    endTime, pageNum, pageSize);
            total = loginLogApplicationService.countLoginLogsByCondition(username, ipAddress, status, beginTime,
                    endTime);
        } else {
            logs = loginLogApplicationService.getLoginLogPage(pageNum, pageSize);
            total = loginLogApplicationService.countLoginLogs();
        }
        List<LoginLogResponse> responses = logs.stream()
                .map(this::convertLoginLogToResponse)
                .collect(Collectors.toList());
        PageResult<LoginLogResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "清空登录日志")
    @DeleteMapping(version = "1", value = "/login/clean")
    @PreAuthorize("hasAuthority('loginlog:remove')")
    @Log(title = "登录日志", businessType = BusinessTypeEnum.CLEAN)
    public Result<Void> cleanLoginLog() {
        loginLogApplicationService.cleanLoginLogs(null);
        return Result.success();
    }

    @Operation(summary = "导出登录日志")
    @GetMapping(version = "1", value = "/login/export")
    @PreAuthorize("hasAuthority('loginlog:export')")
    @Log(title = "登录日志", businessType = BusinessTypeEnum.EXPORT)
    public void exportLoginLog(HttpServletResponse response, @RequestParam(required = false) String username,
                               @RequestParam(required = false) String ipAddress,
                               @RequestParam(required = false) Integer status,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime beginTime,
                               @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime endTime)
            throws IOException {
        List<LoginLog> logs = loginLogApplicationService.getAllLoginLogsByCondition(username, ipAddress, status,
                beginTime, endTime);
        List<LoginLogExportVO> exportList = logs.stream()
                .map(this::convertLoginLogToExportVO)
                .collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, LoginLogExportVO.class, "登录日志", "登录日志数据");
    }

    // ==================== 操作日志接口 ====================
    @Operation(summary = "查询操作日志列表")
    @GetMapping(version = "1", value = "/operate/list")
    @PreAuthorize("hasAuthority('operatelog:list')")
    public Result<PageResult<OperateLogResponse>> operateLogList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                                 @RequestParam(required = false) String title,
                                                                 @RequestParam(required = false) String operatorName,
                                                                 @RequestParam(required = false) Integer businessType,
                                                                 @RequestParam(required = false) Integer status,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                 LocalDateTime beginTime,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                                 LocalDateTime endTime) {
        List<OperateLog> logs;
        long total;
        if (title != null || operatorName != null || businessType != null || status != null || beginTime != null
                || endTime != null) {
            logs = operateLogApplicationService.getOperateLogPageByCondition(title, operatorName, businessType, status,
                    beginTime, endTime, pageNum, pageSize);
            total = operateLogApplicationService.countOperateLogsByCondition(title, operatorName, businessType, status,
                    beginTime, endTime);
        } else {
            logs = operateLogApplicationService.getOperateLogPage(pageNum, pageSize);
            total = operateLogApplicationService.countOperateLogs();
        }
        List<OperateLogResponse> responses = logs.stream()
                .map(this::convertOperateLogToResponse)
                .collect(Collectors.toList());
        PageResult<OperateLogResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "查询操作日志详情")
    @GetMapping(version = "1", value = "/operate/{id}")
    @PreAuthorize("hasAuthority('operatelog:query')")
    public Result<OperateLogResponse> operateLogDetail(@PathVariable Long id) {
        OperateLog log = operateLogApplicationService.getOperateLogById(id)
                .orElseThrow(() -> new RuntimeException("操作日志不存在"));
        return Result.success(convertOperateLogToResponse(log));
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping(version = "1", value = "/operate/clean")
    @PreAuthorize("hasAuthority('operatelog:remove')")
    @Log(title = "操作日志", businessType = BusinessTypeEnum.CLEAN)
    public Result<Void> cleanOperateLog() {
        operateLogApplicationService.cleanOperateLogs(null);
        return Result.success();
    }

    @Operation(summary = "导出操作日志")
    @GetMapping(version = "1", value = "/operate/export")
    @PreAuthorize("hasAuthority('operatelog:export')")
    @Log(title = "操作日志", businessType = BusinessTypeEnum.EXPORT)
    public void exportOperateLog(HttpServletResponse response, @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String operatorName,
                                 @RequestParam(required = false) Integer businessType,
                                 @RequestParam(required = false) Integer status,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                 LocalDateTime beginTime,
                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                 LocalDateTime endTime)
            throws IOException {
        List<OperateLog> logs = operateLogApplicationService.getAllOperateLogsByCondition(title, operatorName,
                businessType, status, beginTime, endTime);
        List<OperateLogExportVO> exportList = logs.stream()
                .map(this::convertOperateLogToExportVO)
                .collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, OperateLogExportVO.class, "操作日志", "操作日志数据");
    }

    // ==================== 运行日志接口 ====================
    @Operation(summary = "查询运行日志列表")
    @GetMapping(version = "1", value = "/run/list")
    @PreAuthorize("hasAuthority('auditlog:list')")
    public Result<PageResult<RunLogResponse>> runLogList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize,
                                                         @RequestParam(required = false) String level,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String logger,
                                                         @RequestParam(required = false) String thread,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                         LocalDateTime beginTime,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                         LocalDateTime endTime) {
        List<RunLogResponse> logs = runLogApplicationService.searchRunLogs(level, keyword, logger, thread, beginTime,
                endTime, pageNum, pageSize);
        long total = runLogApplicationService.countRunLogs(level, keyword, logger, thread, beginTime, endTime);
        PageResult<RunLogResponse> pageResult = new PageResult<>(logs, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "查询运行日志详情")
    @GetMapping(version = "1", value = "/run/{id}")
    @PreAuthorize("hasAuthority('auditlog:query')")
    public Result<RunLogResponse> runLogDetail(@PathVariable String id) {
        RunLogResponse log = runLogApplicationService.getRunLogById(id)
                .orElseThrow(() -> new RuntimeException("运行日志不存在"));
        return Result.success(log);
    }

    // ==================== 转换方法 ====================
    private LoginLogResponse convertLoginLogToResponse(LoginLog log) {
        return LoginLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .ipAddress(log.getIpAddress())
                .loginLocation(log.getLoginLocation())
                .browser(log.getBrowser())
                .os(log.getOs())
                .status(log.getStatus())
                .msg(log.getMessage())
                .loginTime(log.getLoginTime())
                .build();
    }

    private OperateLogResponse convertOperateLogToResponse(OperateLog log) {
        return OperateLogResponse.builder()
                .id(log.getId())
                .title(log.getTitle())
                .businessType(log.getBusinessType())
                .method(log.getMethod())
                .requestMethod(log.getRequestMethod())
                .operatorName(log.getOperatorName())
                .deptName(log.getDeptName())
                .operUrl(log.getUrl())
                .operIp(log.getIp())
                .operLocation(log.getLocation())
                .operParam(log.getParam())
                .jsonResult(log.getResult())
                .status(log.getStatus())
                .errorMsg(log.getErrorMsg())
                .costTime(log.getCostTime())
                .operTime(log.getOperateTime())
                .build();
    }

    private LoginLogExportVO convertLoginLogToExportVO(LoginLog log) {
        return LoginLogExportVO.builder()
                .id(log.getId())
                .username(log.getUsername())
                .ipAddress(log.getIpAddress())
                .loginLocation(log.getLoginLocation())
                .browser(log.getBrowser())
                .os(log.getOs())
                .status(log.getStatus())
                .msg(log.getMessage())
                .loginTime(log.getLoginTime())
                .build();
    }

    private OperateLogExportVO convertOperateLogToExportVO(OperateLog log) {
        return OperateLogExportVO.builder()
                .id(log.getId())
                .title(log.getTitle())
                .businessType(log.getBusinessType())
                .requestMethod(log.getRequestMethod())
                .operatorName(log.getOperatorName())
                .deptName(log.getDeptName())
                .operUrl(log.getUrl())
                .operIp(log.getIp())
                .operLocation(log.getLocation())
                .status(log.getStatus())
                .costTime(log.getCostTime())
                .operTime(log.getOperateTime())
                .build();
    }

}
