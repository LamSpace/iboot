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

import com.iboot.admin.application.service.ServerMonitorApplicationService;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.interfaces.dto.response.ServerInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务监控检查控制器 提供服务器系统信息、服务健康状态等监控接口 监控阈值通过参数配置动态调整，检查项通过字典管理灵活定义
 *
 * @author iBoot
 */
@Tag(name = "服务监控检查", description = "服务器监控信息及健康检查接口")
@RestController
@RequestMapping("/api/monitor/server")
public class ServerMonitorController {

    private final ServerMonitorApplicationService serverMonitorApplicationService;

    @SuppressWarnings("all")
    public ServerMonitorController(final ServerMonitorApplicationService serverMonitorApplicationService) {
        this.serverMonitorApplicationService = serverMonitorApplicationService;
    }

    @Operation(summary = "获取服务器监控信息", description = "获取CPU、内存、JVM、磁盘、操作系统信息及服务健康检查结果")
    @GetMapping
    @PreAuthorize("hasAuthority('server:list')")
    public Result<ServerInfoResponse> getServerInfo() {
        ServerInfoResponse serverInfo = serverMonitorApplicationService.getServerInfo();
        return Result.success(serverInfo);
    }

}
