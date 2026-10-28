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

package com.iboot.ai.interfaces.controller;

import com.iboot.ai.application.service.graph.GraphDisabledException;
import com.iboot.ai.application.service.graph.GraphSyncService;
import com.iboot.ai.domain.graph.GraphStatus;
import com.iboot.ai.domain.graph.GraphSyncSummary;
import com.iboot.ai.infrastructure.config.GraphProperties;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.ai.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识图谱运维接口。
 *
 * <p>提供同步触发与状态查询。控制器常驻注册，能力关闭时返回可辨识的 {@code graph_disabled}，
 * 便于运维区分「未启用」与「路径写错」。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai/graph")
@Tag(name = "AI 知识图谱", description = "组织架构与 RBAC 图谱的同步与状态")
public class GraphController {

    private final GraphProperties properties;

    private final ObjectProvider<GraphSyncService> syncService;

    private final ObjectProvider<Neo4jGraphRepository> repository;

    /**
     * 创建控制器。
     *
     * @param properties  知识图谱配置
     * @param syncService 同步服务，能力关闭时不存在
     * @param repository  图仓储，能力关闭时不存在
     */
    public GraphController(GraphProperties properties, ObjectProvider<GraphSyncService> syncService,
                           ObjectProvider<Neo4jGraphRepository> repository) {
        this.properties = properties;
        this.syncService = syncService;
        this.repository = repository;
    }

    /**
     * 以调用者身份同步组织图谱。
     *
     * @return 同步摘要
     * @throws GraphDisabledException 能力未启用时抛出
     */
    @Operation(summary = "同步组织图谱", description = "使用调用者登录态读取 admin 组织数据并整体替换图内容")
    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('ai:graph:use')")
    public GraphSyncSummary sync() {
        assertEnabled();
        return syncService.getObject().sync(SecurityUtils.getCurrentBearerToken());
    }

    /**
     * 查询图谱同步状态。
     *
     * @return 当前状态
     * @throws GraphDisabledException 能力未启用时抛出
     */
    @Operation(summary = "查询图谱状态", description = "返回上次同步时间与各类节点数量")
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ai:graph:use')")
    public GraphStatus status() {
        assertEnabled();
        return repository.getObject().loadStatus();
    }

    private void assertEnabled() {
        if (!properties.isEnabled()) {
            throw new GraphDisabledException("知识图谱能力未启用");
        }
    }
}
