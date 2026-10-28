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
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.domain.graph.DeptPath;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link QueryDeptPathTool} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class QueryDeptPathToolTest {

    private static final QueryScope SCOPE = new QueryScope(true, List.of());

    private final GraphQueryService queryService = mock(GraphQueryService.class);

    private final GraphScopeResolver scopeResolver = mock(GraphScopeResolver.class);

    private final QueryDeptPathTool tool = new QueryDeptPathTool(queryService, scopeResolver);

    @Test
    void deniesWithoutGraphPermission() {
        ToolOutcome outcome = tool.execute(context(Set.of()), Map.of("fromDeptId", 1, "toDeptId", 2));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.DENIED);
        verifyNoInteractions(queryService, scopeResolver);
    }

    @Test
    void returnsPath() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.deptPath(3L, 4L, SCOPE))
                .thenReturn(Optional.of(new DeptPath(List.of("市场部", "总经办", "研发部", "后端组"), 3)));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("fromDeptId", 3, "toDeptId", 4));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.OK);
        assertThat(outcome.content()).contains("市场部").contains("\"hops\":3");
        verify(queryService).deptPath(3L, 4L, SCOPE);
    }

    @Test
    void returnsEmptyWhenNoPath() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.deptPath(3L, 4L, SCOPE)).thenReturn(Optional.empty());

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("fromDeptId", 3, "toDeptId", 4));

        assertThat(outcome.content()).isEqualTo("{\"path\":null}");
    }

    @Test
    void failsOnMissingParameters() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.deptPath(3L, null, SCOPE)).thenThrow(new IllegalArgumentException("缺少部门标识"));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")), Map.of("fromDeptId", 3));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("缺少部门标识");
    }

    private AgentContext context(Set<String> permissions) {
        return AgentContext.start(10L, "token", permissions, action -> { });
    }
}
