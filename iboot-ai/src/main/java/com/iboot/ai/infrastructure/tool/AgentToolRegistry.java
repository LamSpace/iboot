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

package com.iboot.ai.infrastructure.tool;

import com.iboot.ai.domain.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 智能体工具白名单注册表。
 *
 * <p>只登记容器中显式声明的 {@link AgentTool} Bean，模型永远无法调用未登记的工具。
 * 智能体的工具白名单在此进一步收窄可用集合。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools;

    /**
     * 创建注册表。
     *
     * @param toolBeans 容器中的工具 Bean
     * @throws IllegalStateException 工具名重复时抛出
     */
    public AgentToolRegistry(List<AgentTool> toolBeans) {
        Map<String, AgentTool> collected = new LinkedHashMap<>();
        for (AgentTool tool : toolBeans) {
            AgentTool previous = collected.put(tool.name(), tool);
            if (previous != null) {
                throw new IllegalStateException("工具名重复：" + tool.name());
            }
        }
        this.tools = Map.copyOf(collected);
    }

    /**
     * 按名称查找工具。
     *
     * @param name 工具名
     * @return 工具，未登记时为空
     */
    public Optional<AgentTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 按白名单筛选可用工具；未登记的名称被忽略。
     *
     * @param allowlist 智能体配置的工具白名单；为空表示无可用工具
     * @return 可用工具
     */
    public List<AgentTool> allowed(Collection<String> allowlist) {
        if (allowlist == null || allowlist.isEmpty()) {
            return List.of();
        }
        return allowlist.stream()
                .distinct()
                .map(tools::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 返回全部已登记的工具名。
     *
     * @return 工具名集合
     */
    public Set<String> names() {
        return tools.keySet();
    }
}
