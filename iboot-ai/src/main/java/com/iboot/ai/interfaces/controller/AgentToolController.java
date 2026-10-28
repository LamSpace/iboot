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

import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import com.iboot.ai.interfaces.dto.AgentToolResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 可用工具清单接口。
 *
 * <p>供配置页展示可勾选的工具；只暴露名称、说明、只读属性与所需权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai/agent/tools")
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentToolController {

    private final AgentToolRegistry registry;

    /**
     * 创建控制器。
     *
     * @param registry 工具注册表
     */
    public AgentToolController(AgentToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * 查询已注册工具。
     *
     * @return 工具清单
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ai:agent:manage')")
    public List<AgentToolResponse> list() {
        return registry.names().stream()
                .map(registry::find)
                .flatMap(java.util.Optional::stream)
                .map(AgentToolResponse::from)
                .toList();
    }
}
