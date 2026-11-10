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

import com.iboot.system.application.service.tcc.LeaveQuotaTransferTccAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 额度划转内部接口测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class InternalLeaveQuotaTransferControllerTest {

    private static final String TOKEN = "internal-secret";

    private LeaveQuotaTransferTccAction action;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        action = mock(LeaveQuotaTransferTccAction.class);
        when(action.tryTransfer(anyString(), anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(true);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InternalLeaveQuotaTransferController(action, TOKEN, true))
                .build();
    }

    /**
     * 验证缺少内部令牌时拒绝划转。
     */
    @Test
    void tryWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/internal/leave/quota/transfer/try")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 验证未启用分布式事务时拒绝请求。
     */
    @Test
    void tryWhenSeataDisabledReturns503() throws Exception {
        MockMvc disabled = MockMvcBuilders
                .standaloneSetup(new InternalLeaveQuotaTransferController(action, TOKEN, false))
                .build();

        disabled.perform(post("/internal/leave/quota/transfer/try")
                        .header("X-Internal-Token", TOKEN)
                        .header("TX_XID", "127.0.0.1:8091:1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isServiceUnavailable());
    }

    /**
     * 验证缺少全局事务标识时拒绝请求。
     */
    @Test
    void tryWithoutXidReturns503() throws Exception {
        mockMvc.perform(post("/internal/leave/quota/transfer/try")
                        .header("X-Internal-Token", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isServiceUnavailable());
    }

    /**
     * 验证成功时返回划转标识与 TRYING 状态。
     */
    @Test
    void tryReturnsTransferId() throws Exception {
        mockMvc.perform(post("/internal/leave/quota/transfer/try")
                        .header("X-Internal-Token", TOKEN)
                        .header("TX_XID", "127.0.0.1:8091:1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.transferId").value("t-1"))
                .andExpect(jsonPath("$.data.status").value("TRYING"));
        verify(action).tryTransfer("t-1", 1L, 2L, 2026, 1);
    }

    private String body() {
        return "{\"transferId\":\"t-1\",\"fromUserId\":1,\"toUserId\":2,\"year\":2026,\"count\":1}";
    }
}
