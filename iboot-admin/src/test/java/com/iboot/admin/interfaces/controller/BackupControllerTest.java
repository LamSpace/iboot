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

import com.iboot.admin.application.service.BusinessMetricsService;
import com.iboot.admin.application.service.backup.BackupApplicationService;
import com.iboot.admin.application.service.backup.BackupSummary;
import com.iboot.admin.application.service.backup.RestoreDrillService;
import com.iboot.admin.common.util.MonitorUtils;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupTriggerType;
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.infrastructure.config.ApiVersionConfig;
import com.iboot.admin.infrastructure.config.JwtSecurityConfig;
import com.iboot.admin.infrastructure.security.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BackupController} 的接口测试。
 *
 * <p>使用 MockMvc 验证端点映射、权限与响应结构。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = BackupController.class)
@Import({ApiVersionConfig.class, MonitorUtils.class, BackupControllerTest.SecurityTestConfig.class})
class BackupControllerTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BackupApplicationService backupApplicationService;

    @MockitoBean
    private RestoreDrillService restoreDrillService;

    @MockitoBean
    private BusinessMetricsService businessMetricsService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtSecurityConfig jwtSecurityConfig;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 验证有列表权限时返回分页结构。
     */
    @Test
    void listRecordsReturnsPage() throws Exception {
        when(backupApplicationService.findPage(any(), anyInt(), anyInt()))
                .thenReturn(List.of(successRecord()));
        when(backupApplicationService.count(any())).thenReturn(1L);

        mockMvc.perform(get("/api/monitor/backup/records")
                        .with(authentication(auth("backup:record:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.total").value(1))
                .andExpect(jsonPath("$.data.data.data[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.data.data[0].storageType").value("minio"));
    }

    /**
     * 验证没有列表权限时返回 403。
     */
    @Test
    void listRecordsRequiresPermission() throws Exception {
        mockMvc.perform(get("/api/monitor/backup/records")
                        .with(authentication(auth("backup:record:create")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证手动备份返回运行中记录。
     */
    @Test
    void createBackupReturnsRunningRecord() throws Exception {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "alice",
                LocalDateTime.now());
        record.setId(7L);
        when(backupApplicationService.triggerManual("alice")).thenReturn(record);

        mockMvc.perform(post("/api/monitor/backup/records")
                        .with(authentication(auth("backup:record:create")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.id").value(7))
                .andExpect(jsonPath("$.data.data.status").value("RUNNING"));
    }

    /**
     * 验证没有备份权限时返回 403。
     */
    @Test
    void createBackupRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/monitor/backup/records")
                        .with(authentication(auth("backup:record:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证恢复演练返回运行中记录。
     */
    @Test
    void restoreReturnsRunningRecord() throws Exception {
        RestoreRecord record = RestoreRecord.start(7L, "iboot_restore_drill", "alice", LocalDateTime.now());
        record.setId(3L);
        when(restoreDrillService.trigger(eq(7L), eq("alice"))).thenReturn(record);

        mockMvc.perform(post("/api/monitor/backup/records/7/restore")
                        .with(authentication(auth("backup:record:restore")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.id").value(3))
                .andExpect(jsonPath("$.data.data.targetSchema").value("iboot_restore_drill"));
    }

    /**
     * 验证没有演练权限时返回 403。
     */
    @Test
    void restoreRequiresPermission() throws Exception {
        mockMvc.perform(post("/api/monitor/backup/records/7/restore")
                        .with(authentication(auth("backup:record:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    /**
     * 验证概览返回功能开关与统计。
     */
    @Test
    void summaryReturnsStatistics() throws Exception {
        when(backupApplicationService.summary())
                .thenReturn(new BackupSummary(true, 2L, 2048L, LocalDateTime.of(2026, 10, 24, 2, 5)));

        mockMvc.perform(get("/api/monitor/backup/summary")
                        .with(authentication(auth("backup:record:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.enabled").value(true))
                .andExpect(jsonPath("$.data.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.data.totalSize").value(2048));
    }

    private BackupRecord successRecord() {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.SCHEDULED, "iboot_admin", "system",
                LocalDateTime.now());
        record.setId(1L);
        record.markSuccess("minio", "backup/iboot_admin/2026/10/24/B1.sql.enc", 100L, 80L, "abc",
                Map.of("sys_user", 3L), LocalDateTime.now());
        return record;
    }

    private Authentication auth(final String... authorities) {
        Map<String, Object> principal = Map.of("userId", 1L, "username", "alice");
        List<SimpleGrantedAuthority> granted = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, granted);
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
