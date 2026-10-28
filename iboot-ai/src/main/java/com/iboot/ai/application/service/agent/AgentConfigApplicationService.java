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

package com.iboot.ai.application.service.agent;

import com.iboot.ai.domain.agent.AgentSummary;
import com.iboot.ai.infrastructure.persistence.AgentRepository;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import com.iboot.ai.interfaces.dto.AgentConfigRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 智能体配置应用服务。
 *
 * <p>维护智能体定义与工具白名单；白名单只允许引用已注册工具，避免配置出模型无法使用或越权的工具。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentConfigApplicationService {

    private final AgentRepository repository;

    private final AgentToolRegistry registry;

    /**
     * 创建服务。
     *
     * @param repository 仓储
     * @param registry   工具注册表
     */
    public AgentConfigApplicationService(AgentRepository repository, AgentToolRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    /**
     * 查询智能体列表。
     *
     * @param onlyEnabled 是否只返回启用的智能体
     * @return 配置视图
     */
    public List<AgentSummary> list(boolean onlyEnabled) {
        return repository.listAgents(onlyEnabled);
    }

    /**
     * 查询单个智能体。
     *
     * @param id 智能体 ID
     * @return 配置视图
     * @throws AgentNotFoundException 不存在时抛出
     */
    public AgentSummary get(long id) {
        return repository.findAgentSummary(id)
                .orElseThrow(() -> new AgentNotFoundException("智能体不存在"));
    }

    /**
     * 新建智能体。
     *
     * @param request   配置请求
     * @param createdBy 创建人
     * @return 新建后的配置
     * @throws IllegalArgumentException 工具未注册时抛出
     */
    public AgentSummary create(AgentConfigRequest request, String createdBy) {
        validateTools(request.toolAllowlist());
        long id = repository.insertAgent(request.name(), request.description(), request.systemPrompt(),
                request.model(), request.toolAllowlist(), request.enabled(), createdBy);
        return get(id);
    }

    /**
     * 更新智能体。
     *
     * @param id      智能体 ID
     * @param request 配置请求
     * @return 更新后的配置
     * @throws AgentNotFoundException   不存在时抛出
     * @throws IllegalArgumentException  工具未注册时抛出
     */
    public AgentSummary update(long id, AgentConfigRequest request) {
        validateTools(request.toolAllowlist());
        int affected = repository.updateAgent(id, request.name(), request.description(), request.systemPrompt(),
                request.model(), request.toolAllowlist(), request.enabled());
        if (affected == 0) {
            throw new AgentNotFoundException("智能体不存在");
        }
        return get(id);
    }

    /**
     * 删除智能体。
     *
     * @param id 智能体 ID
     * @throws AgentNotFoundException 不存在时抛出
     */
    public void delete(long id) {
        if (repository.deleteAgent(id) == 0) {
            throw new AgentNotFoundException("智能体不存在");
        }
    }

    private void validateTools(List<String> allowlist) {
        List<String> unknown = allowlist.stream()
                .filter(Objects::nonNull)
                .filter(name -> registry.find(name).isEmpty())
                .toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("未注册的工具：" + String.join(", ", unknown));
        }
    }
}
