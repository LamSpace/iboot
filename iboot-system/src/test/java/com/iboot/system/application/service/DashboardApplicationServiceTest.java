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

package com.iboot.system.application.service;

import com.iboot.system.infrastructure.persistence.mapper.LoginLogMapper;
import com.iboot.system.infrastructure.persistence.mapper.OperateLogMapper;
import com.iboot.system.infrastructure.persistence.po.OperateLogPO;
import com.iboot.system.interfaces.dto.response.DashboardSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * 仪表盘最近操作日志的认证方式透传测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock
    private UserApplicationService userApplicationService;

    @Mock
    private RoleApplicationService roleApplicationService;

    @Mock
    private DeptApplicationService deptApplicationService;

    @Mock
    private PostApplicationService postApplicationService;

    @Mock
    private LoginLogMapper loginLogMapper;

    @Mock
    private OperateLogMapper operateLogMapper;

    /**
     * 最近操作日志应保留认证方式。
     */
    @Test
    void recentOperateLogsKeepAuthType() {
        OperateLogPO po = new OperateLogPO();
        po.setId(1L);
        po.setTitle("定时任务");
        po.setAuthType("PAT");
        po.setOperateTime(LocalDateTime.now());
        when(loginLogMapper.count()).thenReturn(0L);
        when(operateLogMapper.count()).thenReturn(0L);
        when(loginLogMapper.selectPage(anyInt(), anyInt())).thenReturn(List.of());
        when(operateLogMapper.selectPage(anyInt(), anyInt())).thenReturn(List.of(po));
        DashboardApplicationService service = new DashboardApplicationService(
                userApplicationService, roleApplicationService, deptApplicationService,
                postApplicationService, loginLogMapper, operateLogMapper);

        DashboardSummaryResponse summary = service.getDashboardSummary();

        assertThat(summary.getRecentOperateLogs()).hasSize(1);
        assertThat(summary.getRecentOperateLogs().get(0).getAuthType()).isEqualTo("PAT");
    }
}
