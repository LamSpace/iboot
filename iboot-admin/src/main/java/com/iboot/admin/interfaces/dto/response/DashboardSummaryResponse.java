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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard 汇总数据响应 DTO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard 汇总数据响应")
public class DashboardSummaryResponse {

    @Schema(description = "用户总数")
    private long userCount;

    @Schema(description = "角色总数")
    private long roleCount;

    @Schema(description = "部门总数")
    private long deptCount;

    @Schema(description = "岗位总数")
    private long postCount;

    @Schema(description = "登录日志总数")
    private long loginLogCount;

    @Schema(description = "操作日志总数")
    private long operateLogCount;

    @Schema(description = "最近登录日志")
    private List<LoginLogResponse> recentLoginLogs;

    @Schema(description = "最近操作日志")
    private List<OperateLogResponse> recentOperateLogs;
}
