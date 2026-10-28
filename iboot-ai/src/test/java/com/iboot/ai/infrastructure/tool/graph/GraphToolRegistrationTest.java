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

package com.iboot.ai.infrastructure.tool.graph;

import com.iboot.ai.application.service.graph.GraphQueryService;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 图工具注册条件测试。
 *
 * <p>验证关闭态下两个工具不进入容器，开启态下均被注册且可通过白名单取用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphToolRegistrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(GraphQueryService.class, () -> mock(GraphQueryService.class))
            .withBean(GraphScopeResolver.class, () -> mock(GraphScopeResolver.class))
            .withUserConfiguration(ToolScanConfig.class);

    @Test
    void registersNothingWhenDisabled() {
        runner.run(context -> assertThat(context.getBeansOfType(AgentTool.class)).isEmpty());
    }

    @Test
    void registersBothToolsWhenEnabled() {
        runner.withPropertyValues("iboot.ai.graph.enabled=true").run(context -> {
            List<AgentTool> tools = List.copyOf(context.getBeansOfType(AgentTool.class).values());
            AgentToolRegistry registry = new AgentToolRegistry(tools);

            assertThat(registry.names()).containsExactlyInAnyOrder(
                    QueryOrgGraphTool.NAME, QueryDeptPathTool.NAME);
            assertThat(registry.allowed(List.of(QueryOrgGraphTool.NAME))).hasSize(1);
        });
    }

    /**
     * 只扫描图工具包，避免加载整个应用上下文。
     */
    @Configuration
    @ComponentScan(basePackageClasses = QueryOrgGraphTool.class)
    static class ToolScanConfig {
    }
}
