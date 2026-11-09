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

package com.iboot.system.interfaces.controller;

import com.iboot.system.application.service.LeaveQuotaApplicationService;
import com.iboot.system.domain.system.model.LeaveQuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 请假额度内部接口测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class InternalLeaveQuotaControllerTest {

    private static final String TOKEN = "internal-secret";

    private LeaveQuotaApplicationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(LeaveQuotaApplicationService.class);
        when(service.deduct(anyString(), anyLong(), anyInt()))
                .thenReturn(new LeaveQuota(1L, 2026, 10, 1, 0));
        when(service.view(anyLong(), anyInt()))
                .thenReturn(new LeaveQuota(1L, 2026, 10, 0, 0));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalLeaveQuotaController(service, TOKEN, true))
                .build();
    }

    /**
     * 验证缺少内部令牌时拒绝扣减。
     */
    @Test
    void deductWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/internal/leave/quota/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deductBody()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 验证参与方未启用分布式事务时拒绝带 XID 的请求。
     */
    @Test
    void deductWithXidWhenSeataDisabledReturns503() throws Exception {
        MockMvc disabled = MockMvcBuilders
                .standaloneSetup(new InternalLeaveQuotaController(service, TOKEN, false))
                .build();

        disabled.perform(post("/internal/leave/quota/deduct")
                        .header("X-Internal-Token", TOKEN)
                        .header("TX_XID", "127.0.0.1:8091:1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deductBody()))
                .andExpect(status().isServiceUnavailable());
    }

    /**
     * 验证扣减成功返回剩余次数。
     */
    @Test
    void deductReturnsQuota() throws Exception {
        mockMvc.perform(post("/internal/leave/quota/deduct")
                        .header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deductBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.remainingCount").value(9));
    }

    /**
     * 验证查询返回额度视图。
     */
    @Test
    void getReturnsQuota() throws Exception {
        mockMvc.perform(get("/internal/leave/quota")
                        .header("X-Internal-Token", TOKEN)
                        .param("userId", "1")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andExpect(jsonPath("$.data.remainingCount").value(10));
    }

    private String deductBody() {
        return "{\"requestId\":\"leave-1\",\"userId\":1,\"year\":2026}";
    }
}
