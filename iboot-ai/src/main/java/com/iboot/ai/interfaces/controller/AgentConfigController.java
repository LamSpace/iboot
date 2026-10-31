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

import com.iboot.ai.application.service.agent.AgentConfigApplicationService;
import com.iboot.kernel.security.SecurityUtils;
import com.iboot.ai.interfaces.dto.AgentConfigRequest;
import com.iboot.ai.interfaces.dto.AgentConfigResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能体配置接口。
 *
 * <p>列表对具备使用权限的用户开放，只返回启用的智能体；配置维护要求 {@code ai:agent:manage}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai/agent/agents")
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentConfigController {

    private final AgentConfigApplicationService service;

    /**
     * 创建控制器。
     *
     * @param service 配置应用服务
     */
    public AgentConfigController(AgentConfigApplicationService service) {
        this.service = service;
    }

    /**
     * 查询可用智能体列表。
     *
     * @param all 是否包含停用项；仅管理权限可查看全部
     * @return 配置列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ai:agent:use')")
    public List<AgentConfigResponse> list(@RequestParam(defaultValue = "false") boolean all) {
        if (all && !SecurityUtils.getCurrentPermissions().contains("ai:agent:manage")) {
            throw new org.springframework.security.access.AccessDeniedException("无权查看全部智能体");
        }
        return service.list(!all).stream().map(AgentConfigResponse::from).toList();
    }

    /**
     * 查询单个智能体配置。
     *
     * @param id 智能体 ID
     * @return 配置
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:agent:manage')")
    public AgentConfigResponse get(@PathVariable long id) {
        return AgentConfigResponse.from(service.get(id));
    }

    /**
     * 新建智能体。
     *
     * @param request 配置请求
     * @return 新建后的配置
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ai:agent:manage')")
    public AgentConfigResponse create(@Valid @RequestBody AgentConfigRequest request) {
        return AgentConfigResponse.from(service.create(request, SecurityUtils.getCurrentUsername()));
    }

    /**
     * 更新智能体。
     *
     * @param id      智能体 ID
     * @param request 配置请求
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:agent:manage')")
    public AgentConfigResponse update(@PathVariable long id, @Valid @RequestBody AgentConfigRequest request) {
        return AgentConfigResponse.from(service.update(id, request));
    }

    /**
     * 删除智能体。
     *
     * @param id 智能体 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ai:agent:manage')")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
