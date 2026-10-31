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

import com.iboot.admin.application.service.ApprovalApplicationService;
import com.iboot.admin.application.service.ApprovalDetail;
import com.iboot.admin.application.service.ApprovalItem;
import com.iboot.admin.application.service.BusinessMetricsService;
import com.iboot.kernel.web.BusinessException;
import com.iboot.kernel.web.PageResult;
import com.iboot.admin.common.util.MonitorUtils;
import com.iboot.kernel.security.JwtSecurityConfig;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;
import com.iboot.admin.infrastructure.config.ApiVersionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ApprovalController} 的接口测试。
 *
 * <p>使用 MockMvc 与 Mock 的服务层验证端点映射、权限与参数校验。安全上下文中的
 * principal 使用 JWT 过滤器约定的 Map 结构。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = ApprovalController.class)
@Import({ApiVersionConfig.class, MonitorUtils.class, com.iboot.kernel.web.GlobalExceptionHandler.class,
        ApprovalControllerTest.SecurityTestConfig.class})
class ApprovalControllerTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalApplicationService service;

    @MockitoBean
    private BusinessMetricsService businessMetricsService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtSecurityConfig jwtSecurityConfig;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证有权限时可以发起请假。
     */
    @Test
    void applyCreatesLeaveApplication() throws Exception {
        when(service.submit(anyLong(), any(), anyLong(), any(), any(), any(), any()))
                .thenReturn(application(LeaveStatus.PENDING));

        mockMvc.perform(post("/api/approval/leave")
                        .with(authentication(auth("approval:apply")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approverId":2,"leaveType":"ANNUAL","startTime":"2026-11-02T09:00:00",
                                 "endTime":"2026-11-03T18:00:00","reason":"回家"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(200))
                .andExpect(jsonPath("$.data.data.status").value("PENDING"));
    }

    /**
     * 验证缺少请求参数时返回校验错误。
     */
    @Test
    void applyValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/approval/leave")
                        .with(authentication(auth("approval:apply")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"ANNUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * 验证没有发起权限时返回 403。
     */
    @Test
    void applyRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/approval/leave")
                        .with(authentication(auth("approval:approve")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approverId":2,"leaveType":"ANNUAL","startTime":"2026-11-02T09:00:00",
                                 "endTime":"2026-11-03T18:00:00","reason":"回家"}
                                """))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证待办查询返回分页结构。
     */
    @Test
    void todoReturnsPage() throws Exception {
        ApprovalItem item = new ApprovalItem();
        item.setApplication(application(LeaveStatus.PENDING));
        item.setTask(task());
        item.setApplicantName("alice");
        item.setApproverName("bob");
        when(service.findTodo(anyLong(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(item), 1L, 1, 10));

        mockMvc.perform(get("/api/approval/todo")
                        .with(authentication(auth("approval:approve")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.total").value(1))
                .andExpect(jsonPath("$.data.data.data[0].applicantName").value("alice"))
                .andExpect(jsonPath("$.data.data.data[0].taskId").value("task-1"));
    }

    /**
     * 验证详情包含审批历史。
     */
    @Test
    void detailReturnsHistory() throws Exception {
        ApprovalDetail detail = new ApprovalDetail();
        detail.setApplication(application(LeaveStatus.APPROVED));
        detail.setApplicantName("alice");
        detail.setApproverName("bob");
        WorkflowHistoryItem submit = new WorkflowHistoryItem();
        submit.setActivityName("提交申请");
        submit.setActorId(1L);
        submit.setTime(LocalDateTime.now());
        WorkflowHistoryItem approve = new WorkflowHistoryItem();
        approve.setActivityName("审批通过");
        approve.setActorId(2L);
        approve.setApproved(true);
        approve.setComment("同意");
        approve.setTime(LocalDateTime.now());
        detail.setHistory(List.of(submit, approve));
        when(service.detail(anyLong(), anyLong(), any(Boolean.class))).thenReturn(detail);

        mockMvc.perform(get("/api/approval/10")
                        .with(authentication(auth("approval:apply")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.history[0].activityName").value("提交申请"))
                .andExpect(jsonPath("$.data.data.history[1].activityName").value("审批通过"))
                .andExpect(jsonPath("$.data.data.history[1].actorName").value("bob"));
    }

    /**
     * 验证没有审批权限时通过操作返回 403。
     */
    @Test
    void approveRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/approval/10/approve")
                        .with(authentication(auth("approval:apply")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证无权查看详情时返回业务码 404。
     */
    @Test
    void detailReturnsNotFoundForUnrelatedUser() throws Exception {
        when(service.detail(anyLong(), anyLong(), any(Boolean.class)))
                .thenThrow(new BusinessException(404, "申请不存在"));

        mockMvc.perform(get("/api/approval/10")
                        .with(authentication(auth("approval:apply")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /**
     * 验证重复审批返回业务码 409。
     */
    @Test
    void approveReturnsConflictWhenAlreadyProcessed() throws Exception {
        org.mockito.Mockito.doThrow(new BusinessException(409, "申请已处理"))
                .when(service).approve(anyLong(), anyLong(), any(), any());

        mockMvc.perform(post("/api/approval/10/approve")
                        .with(authentication(auth("approval:approve")))
                        .header(VERSION_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    private Authentication auth(final String... authorities) {
        Map<String, Object> principal = Map.of("userId", 1L, "username", "alice");
        List<SimpleGrantedAuthority> granted = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, granted);
    }

    private LeaveApplication application(LeaveStatus status) {
        LeaveApplication application = new LeaveApplication();
        application.setId(10L);
        application.setApplicantId(1L);
        application.setApproverId(2L);
        application.setLeaveType("ANNUAL");
        application.setStartTime(LocalDateTime.of(2026, 11, 2, 9, 0));
        application.setEndTime(LocalDateTime.of(2026, 11, 3, 18, 0));
        application.setReason("回家");
        application.setStatus(status);
        application.setCreateTime(LocalDateTime.now());
        return application;
    }

    private WorkflowTask task() {
        WorkflowTask task = new WorkflowTask();
        task.setTaskId("task-1");
        task.setProcessInstanceId("pid-10");
        task.setAssigneeId(2L);
        task.setTaskName("审批");
        task.setCreateTime(LocalDateTime.now());
        return task;
    }

    /**
     * 测试安全配置：放开过滤链，方法级权限仍由 {@code @PreAuthorize} 校验。
     */
    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfig {

        /**
         * 提供内存指标注册表，避免监控过滤器在测试中空指针。
         *
         * @return 指标注册表
         */
        @Bean
        io.micrometer.core.instrument.MeterRegistry meterRegistry() {
            return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        }

        /**
         * 提供 Jackson 2 的 ObjectMapper，供 JWT 过滤器使用。
         *
         * @return Jackson 2 ObjectMapper
         */
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }

        /**
         * 允许所有请求进入控制器。
         *
         * @param http 安全配置
         * @return 过滤链
         * @throws Exception 配置失败时抛出
         */
        @Bean
        SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }
}
