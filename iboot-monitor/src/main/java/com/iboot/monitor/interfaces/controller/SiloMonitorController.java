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

package com.iboot.monitor.interfaces.controller;

import com.iboot.monitor.application.service.SiloAdminService;
import com.iboot.kernel.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Silo 监控控制器。
 *
 * <p>提供 Silo 集群状态、存储桶列表和存储使用情况查询接口，
 * 需要 {@code silo:query} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see SiloAdminService
 */
@Tag(name = "Silo监控", description = "Silo对象存储监控信息接口")
@RestController
@RequestMapping("/api/monitor/silo")
public class SiloMonitorController {

    private final SiloAdminService siloAdminService;

    @SuppressWarnings("all")
    public SiloMonitorController(final SiloAdminService siloAdminService) {
        this.siloAdminService = siloAdminService;
    }

    /**
     * 查询 Silo 监控信息。
     *
     * @return 集群状态、存储桶列表和存储使用情况
     */
    @Operation(summary = "获取Silo监控信息", description = "获取Silo集群状态、存储桶列表、存储使用情况等")
    @GetMapping(version = "1")
    @PreAuthorize("hasAuthority('silo:query')")
    public Result<Map<String, Object>> getSiloStatus() {
        Map<String, Object> status = siloAdminService.getSiloStatus();
        return Result.success(status);
    }

    /**
     * 检查 Silo 服务是否在线。
     *
     * @return 在线返回 {@code true}，否则返回 {@code false}
     */
    @Operation(summary = "检查Silo服务状态", description = "检查Silo服务是否在线")
    @GetMapping(version = "1", value = "/ping")
    @PreAuthorize("hasAuthority('silo:query')")
    public Result<Boolean> pingSilo() {
        boolean online = siloAdminService.pingSilo();
        return Result.success(online);
    }

}
