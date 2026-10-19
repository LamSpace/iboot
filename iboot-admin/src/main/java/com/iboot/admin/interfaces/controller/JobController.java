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

import com.iboot.admin.application.service.JobApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.job.model.Job;
import com.iboot.admin.domain.job.model.JobLog;
import com.iboot.admin.infrastructure.job.ScheduleUtils;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.export.JobExportVO;
import com.iboot.admin.interfaces.dto.request.JobRequest;
import com.iboot.admin.interfaces.dto.response.JobLogResponse;
import com.iboot.admin.interfaces.dto.response.JobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务控制器。
 *
 * <p>提供定时任务的分页查询、详情、增删改、状态切换、立即执行、cron 校验、导出，
 * 以及任务执行日志的查询、详情和清理接口，对应 {@code job:*} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see JobApplicationService
 */
@Tag(name = "定时任务管理", description = "定时任务管理相关接口")
@RestController
@RequestMapping("/api/job")
public class JobController {

    private final JobApplicationService jobApplicationService;

    @SuppressWarnings("all")
    public JobController(final JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    // ==================== 任务管理接口 ====================
    /**
     * 分页查询定时任务。
     *
     * <p>任一带条件参数非空时走条件查询，否则查询全部任务。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param jobName 任务名称关键字，可为 {@code null}
     * @param jobGroup 任务组，可为 {@code null}
     * @param status 状态：0-暂停，1-正常；可为 {@code null}
     * @return 定时任务分页结果
     */
    @Operation(summary = "查询定时任务列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('job:list')")
    public Result<PageResult<JobResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String jobName,
                                                @RequestParam(required = false) String jobGroup,
                                                @RequestParam(required = false) Integer status) {
        List<Job> jobs;
        long total;
        if (jobName != null || jobGroup != null || status != null) {
            jobs = jobApplicationService.getJobPageByCondition(jobName, jobGroup, status, pageNum, pageSize);
            total = jobApplicationService.countJobsByCondition(jobName, jobGroup, status);
        } else {
            jobs = jobApplicationService.getJobPage(pageNum, pageSize);
            total = jobApplicationService.countJobs();
        }
        List<JobResponse> responses = jobs.stream().map(this::convertJobToResponse).collect(Collectors.toList());
        PageResult<JobResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 获取定时任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     * @throws BusinessException 任务不存在时抛出
     */
    @Operation(summary = "获取定时任务详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('job:query')")
    public Result<JobResponse> getInfo(@PathVariable Long id) {
        Job job = jobApplicationService.getJobById(id).orElseThrow(() -> new BusinessException("任务不存在"));
        return Result.success(convertJobToResponse(job));
    }

    /**
     * 新增定时任务。
     *
     * @param request 任务创建请求
     * @return 创建后的任务详情
     */
    @Operation(summary = "新增定时任务")
    @PostMapping
    @PreAuthorize("hasAuthority('job:add')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.INSERT)
    public Result<JobResponse> add(@Valid @RequestBody JobRequest request) {
        Job job = convertRequestToJob(request);
        job.setCreateBy(SecurityUtils.getCurrentUsername());
        Job savedJob = jobApplicationService.createJob(job);
        return Result.success(convertJobToResponse(savedJob));
    }

    /**
     * 修改定时任务。
     *
     * @param request 任务更新请求，{@code id} 不能为空
     * @return 更新后的任务详情
     * @throws BusinessException 任务 ID 为空时抛出
     */
    @Operation(summary = "修改定时任务")
    @PutMapping
    @PreAuthorize("hasAuthority('job:edit')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.UPDATE)
    public Result<JobResponse> edit(@Valid @RequestBody JobRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("任务ID不能为空");
        }
        Job job = convertRequestToJob(request);
        job.setUpdateBy(SecurityUtils.getCurrentUsername());
        Job updatedJob = jobApplicationService.updateJob(job);
        return Result.success(convertJobToResponse(updatedJob));
    }

    /**
     * 删除定时任务。
     *
     * @param id 任务 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除定时任务")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('job:remove')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> remove(@PathVariable Long id) {
        jobApplicationService.deleteJob(id);
        return Result.success();
    }

    /**
     * 修改任务状态。
     *
     * @param id 任务 ID
     * @param status 目标状态：0-暂停，1-正常
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改任务状态")
    @PutMapping(version = "1", value = "/{id}/status")
    @PreAuthorize("hasAuthority('job:changeStatus')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        jobApplicationService.changeJobStatus(id, status);
        return Result.success();
    }

    /**
     * 立即执行一次任务。
     *
     * @param id 任务 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "立即执行一次任务")
    @PostMapping(version = "1", value = "/{id}/run")
    @PreAuthorize("hasAuthority('job:run')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.OTHER)
    public Result<Void> run(@PathVariable Long id) {
        jobApplicationService.runJob(id);
        return Result.success();
    }

    /**
     * 导出定时任务列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param jobName 任务名称关键字，可为 {@code null}
     * @param jobGroup 任务组，可为 {@code null}
     * @param status 状态：0-暂停，1-正常；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出定时任务列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('job:export')")
    @Log(title = "定时任务", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String jobName,
                       @RequestParam(required = false) String jobGroup, @RequestParam(required = false) Integer status)
            throws IOException {
        List<Job> jobs;
        if (jobName != null || jobGroup != null || status != null) {
            jobs = jobApplicationService.getAllJobsByCondition(jobName, jobGroup, status);
        } else {
            jobs = jobApplicationService.getAllJobs();
        }
        List<JobExportVO> exportList = jobs.stream().map(this::convertJobToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, JobExportVO.class, "定时任务", "任务数据");
    }

    /**
     * 校验 cron 表达式。
     *
     * @param cronExpression cron 表达式
     * @return 表达式合法返回 {@code true}，否则返回 {@code false}
     */
    @Operation(summary = "校验cron表达式")
    @GetMapping(version = "1", value = "/checkCron")
    public Result<Boolean> checkCronExpression(@RequestParam String cronExpression) {
        return Result.success(ScheduleUtils.isValidCronExpression(cronExpression));
    }

    // ==================== 任务日志接口 ====================
    /**
     * 分页查询任务执行日志。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param jobId 任务 ID，可为 {@code null}
     * @param jobName 任务名称关键字，可为 {@code null}
     * @param jobGroup 任务组，可为 {@code null}
     * @param status 执行状态：0-失败，1-成功；可为 {@code null}
     * @param beginTime 开始时间，可为 {@code null}
     * @param endTime 结束时间，可为 {@code null}
     * @return 任务日志分页结果
     */
    @Operation(summary = "查询任务执行日志列表")
    @GetMapping(version = "1", value = "/log/list")
    @PreAuthorize("hasAuthority('job:log')")
    public Result<PageResult<JobLogResponse>> logList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) Long jobId,
                                                      @RequestParam(required = false) String jobName,
                                                      @RequestParam(required = false) String jobGroup,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                      LocalDateTime beginTime,
                                                      @RequestParam(required = false)
                                                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                                      LocalDateTime endTime) {
        List<JobLog> logs;
        long total;
        if (jobId != null || jobName != null || jobGroup != null || status != null || beginTime != null
                || endTime != null) {
            logs = jobApplicationService.getJobLogPageByCondition(jobId, jobName, jobGroup, status, beginTime, endTime,
                    pageNum, pageSize);
            total = jobApplicationService.countJobLogsByCondition(jobId, jobName, jobGroup, status, beginTime, endTime);
        } else {
            logs = jobApplicationService.getJobLogPage(pageNum, pageSize);
            total = jobApplicationService.countJobLogs();
        }
        List<JobLogResponse> responses = logs.stream().map(this::convertJobLogToResponse).collect(Collectors.toList());
        PageResult<JobLogResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 获取任务日志详情。
     *
     * @param id 日志 ID
     * @return 任务日志详情
     * @throws BusinessException 日志不存在时抛出
     */
    @Operation(summary = "获取任务日志详情")
    @GetMapping(version = "1", value = "/log/{id}")
    @PreAuthorize("hasAuthority('job:log')")
    public Result<JobLogResponse> getLogInfo(@PathVariable Long id) {
        JobLog log = jobApplicationService.getJobLogById(id).orElseThrow(() -> new BusinessException("任务日志不存在"));
        return Result.success(convertJobLogToResponse(log));
    }

    /**
     * 清理任务执行日志。
     *
     * <p>指定 {@code jobId} 时只清理该任务的日志；否则清理 {@code days} 天前的日志；
     * 两者都为空时默认清理 30 天前的日志。
     *
     * @param jobId 任务 ID，可为 {@code null}
     * @param days 保留天数，可为 {@code null}
     * @return 实际清理的日志数量
     */
    @Operation(summary = "清理任务执行日志")
    @DeleteMapping(version = "1", value = "/log/clean")
    @PreAuthorize("hasAuthority('job:log')")
    @Log(title = "定时任务日志", businessType = BusinessTypeEnum.CLEAN)
    public Result<Integer> cleanLog(@RequestParam(required = false) Long jobId,
                                    @RequestParam(required = false) Integer days) {
        int count;
        if (jobId != null) {
            count = jobApplicationService.cleanJobLogsByJobId(jobId);
        } else if (days != null) {
            LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
            count = jobApplicationService.cleanJobLogs(beforeDate);
        } else {
            // 默认清理30天前的日志
            LocalDateTime beforeDate = LocalDateTime.now().minusDays(30);
            count = jobApplicationService.cleanJobLogs(beforeDate);
        }
        return Result.success(count);
    }

    // ==================== 转换方法 ====================
    private Job convertRequestToJob(JobRequest request) {
        return Job.builder()
                .id(request.getId())
                .jobName(request.getJobName())
                .jobGroup(request.getJobGroup())
                .invokeTarget(request.getInvokeTarget())
                .cronExpression(request.getCronExpression())
                .misfirePolicy(request.getMisfirePolicy())
                .concurrent(request.getConcurrent())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }

    private JobResponse convertJobToResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .jobGroup(job.getJobGroup())
                .invokeTarget(job.getInvokeTarget())
                .cronExpression(job.getCronExpression())
                .misfirePolicy(job.getMisfirePolicy())
                .concurrent(job.getConcurrent())
                .status(job.getStatus())
                .remark(job.getRemark())
                .createBy(job.getCreateBy())
                .createTime(job.getCreateTime())
                .updateBy(job.getUpdateBy())
                .updateTime(job.getUpdateTime())
                .build();
    }

    private JobLogResponse convertJobLogToResponse(JobLog log) {
        return JobLogResponse.builder()
                .id(log.getId())
                .jobId(log.getJobId())
                .jobName(log.getJobName())
                .jobGroup(log.getJobGroup())
                .invokeTarget(log.getInvokeTarget())
                .jobMessage(log.getJobMessage())
                .status(log.getStatus())
                .exceptionInfo(log.getExceptionInfo())
                .startTime(log.getStartTime())
                .endTime(log.getEndTime())
                .costTime(log.getCostTime())
                .createTime(log.getCreateTime())
                .build();
    }

    private JobExportVO convertJobToExportVO(Job job) {
        return JobExportVO.builder()
                .id(job.getId())
                .jobName(job.getJobName())
                .jobGroup(job.getJobGroup())
                .invokeTarget(job.getInvokeTarget())
                .cronExpression(job.getCronExpression())
                .misfirePolicy(job.getMisfirePolicy())
                .concurrent(job.getConcurrent())
                .status(job.getStatus())
                .remark(job.getRemark())
                .createTime(job.getCreateTime())
                .build();
    }

}
