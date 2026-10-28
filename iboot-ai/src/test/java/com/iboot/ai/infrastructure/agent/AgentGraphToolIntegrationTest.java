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

package com.iboot.ai.infrastructure.agent;

import com.iboot.ai.application.service.QuotaService;
import com.iboot.ai.application.service.agent.AgentRunEvent;
import com.iboot.ai.application.service.agent.AgentRunService;
import com.iboot.ai.application.service.graph.GraphQueryService;
import com.iboot.ai.application.service.graph.GraphRows;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.UserRef;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.persistence.AgentRepository;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import com.iboot.ai.infrastructure.tool.graph.QueryOrgGraphTool;
import com.iboot.ai.support.FakeOpenAiServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 图工具在智能体链路中的端到端测试。
 *
 * <p>用 {@link FakeOpenAiServer} 让模型发起一次 {@code query_org_graph} 调用，验证只读工具
 * 不进入待确认、结果进入事件流、工具调用指标被记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentGraphToolIntegrationTest {

    private static final long AGENT_ID = 1L;

    private static final long USER_ID = 10L;

    private static final long RUN_ID = 99L;

    private static final String CONVERSATION = "c-graph";

    private static final String TOKEN = "tk-1";

    private static final QueryScope SCOPE = new QueryScope(false, List.of(2L));

    private FakeOpenAiServer fake;

    private GraphQueryService queryService;

    private GraphScopeResolver scopeResolver;

    private AgentRepository repository;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        fake = FakeOpenAiServer.start();
        fake.enableToolCall(QueryOrgGraphTool.NAME, "{\"template\":\"colleagues\"}");
        queryService = mock(GraphQueryService.class);
        scopeResolver = mock(GraphScopeResolver.class);
        repository = mock(AgentRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        when(scopeResolver.resolve(USER_ID, TOKEN)).thenReturn(SCOPE);
        when(queryService.colleagues(null, USER_ID, SCOPE)).thenReturn(new GraphRows<>(
                List.of(new UserRef(11L, "bob", "鲍勃", 2L, "研发部")), false));
        when(repository.findEnabledAgent(AGENT_ID)).thenReturn(Optional.of(new AgentDefinition(AGENT_ID,
                "graph-helper", "组织助手", "你是组织助手，可以查询同部门同事。", null,
                List.of(QueryOrgGraphTool.NAME))));
        when(repository.insertRun(AGENT_ID, USER_ID, CONVERSATION)).thenReturn(RUN_ID);
    }

    @AfterEach
    void tearDown() {
        fake.stop();
    }

    @Test
    void runsReadOnlyGraphToolWithoutApproval() {
        AgentScopeRuntime runtime = runtime();
        AgentToolRegistry registry = registry();
        AgentRunService service = new AgentRunService(runtime, repository, registry,
                mock(QuotaService.class), new AiMetrics(meterRegistry, "qwen3.8-flash"), properties());

        List<AgentRunEvent> events = service.stream(context(), AGENT_ID, CONVERSATION, "谁和我同部门？")
                .collectList().block(Duration.ofSeconds(60));

        assertThat(events).isNotNull().isNotEmpty();
        assertThat(events).extracting(AgentRunEvent::name)
                .contains("tool_result")
                .doesNotContain("pending_approval");
        assertThat(events.getLast().name()).isEqualTo("done");
        verify(queryService).colleagues(null, USER_ID, SCOPE);
        Counter counter = meterRegistry.find("iboot.ai.agent.tool.calls")
                .tag("tool", QueryOrgGraphTool.NAME).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isPositive();
    }

    private AgentScopeRuntime runtime() {
        return new AgentScopeRuntime(registry(), properties(), fake.baseUrl(), "test-key", "qwen3.8-flash");
    }

    private AgentToolRegistry registry() {
        return new AgentToolRegistry(List.of(new QueryOrgGraphTool(queryService, scopeResolver)));
    }

    private AiProperties properties() {
        AiProperties properties = new AiProperties();
        properties.getAgent().setEnabled(true);
        properties.getAgent().setMaxSteps(4);
        return properties;
    }

    private AgentContext context() {
        return new AgentContext(USER_ID, TOKEN, Set.of("ai:graph:use"), null, null, action -> { });
    }
}
