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

import com.iboot.admin.application.service.MinioAdminService;
import com.iboot.kernel.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MinIO 监控控制器。
 *
 * <p>提供 MinIO 集群状态、存储桶列表和存储使用情况查询接口，
 * 需要 {@code minio:query} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MinioAdminService
 */
@Tag(name = "MinIO监控", description = "MinIO对象存储监控信息接口")
@RestController
@RequestMapping("/api/monitor/minio")
public class MinioMonitorController {

    private final MinioAdminService minioAdminService;

    @SuppressWarnings("all")
    public MinioMonitorController(final MinioAdminService minioAdminService) {
        this.minioAdminService = minioAdminService;
    }

    /**
     * 查询 MinIO 监控信息。
     *
     * @return 集群状态、存储桶列表和存储使用情况
     */
    @Operation(summary = "获取MinIO监控信息", description = "获取MinIO集群状态、存储桶列表、存储使用情况等")
    @GetMapping(version = "1")
    @PreAuthorize("hasAuthority('minio:query')")
    public Result<Map<String, Object>> getMinioStatus() {
        Map<String, Object> status = minioAdminService.getMinioStatus();
        return Result.success(status);
    }

    /**
     * 检查 MinIO 服务是否在线。
     *
     * @return 在线返回 {@code true}，否则返回 {@code false}
     */
    @Operation(summary = "检查MinIO服务状态", description = "检查MinIO服务是否在线")
    @GetMapping(version = "1", value = "/ping")
    @PreAuthorize("hasAuthority('minio:query')")
    public Result<Boolean> pingMinio() {
        boolean online = minioAdminService.pingMinio();
        return Result.success(online);
    }

}
