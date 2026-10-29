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
import com.iboot.ai.application.service.graph.GraphRows;
import com.iboot.ai.application.service.graph.GraphScopeUnavailableException;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.domain.graph.GraphNodeMatch;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.UserRef;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link QueryOrgGraphTool} 的单元测试。
 *
 * <p>覆盖权限前置拒绝、模板分发、身份字段忽略、截断标记与失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class QueryOrgGraphToolTest {

    private static final QueryScope SCOPE = new QueryScope(false, List.of(2L));

    private final GraphQueryService queryService = mock(GraphQueryService.class);

    private final GraphScopeResolver scopeResolver = mock(GraphScopeResolver.class);

    private final QueryOrgGraphTool tool = new QueryOrgGraphTool(queryService, scopeResolver);

    @Test
    void deniesWithoutGraphPermission() {
        ToolOutcome outcome = tool.execute(context(Set.of("ai:chat")), Map.of("template", "colleagues"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.DENIED);
        verifyNoInteractions(queryService, scopeResolver);
    }

    @Test
    void delegatesColleaguesWithCallerIdentity() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.colleagues(null, 10L, SCOPE))
                .thenReturn(new GraphRows<>(List.of(new UserRef(11L, "bob", "鲍勃", 2L, "研发部")), false));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "colleagues", "callerId", 999));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.OK);
        assertThat(outcome.content()).contains("\"truncated\":false").contains("bob");
        verify(queryService).colleagues(null, 10L, SCOPE);
    }

    @Test
    void marksTruncatedResults() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.permHolders("user:list", SCOPE))
                .thenReturn(new GraphRows<>(List.of(), true));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "perm_holders", "permission", "user:list"));

        assertThat(outcome.content()).contains("\"truncated\":true");
    }

    @Test
    void rejectsUnknownTemplate() {
        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")), Map.of("template", "cypher"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("不支持的查询模板");
        verifyNoInteractions(queryService);
    }

    @Test
    void routesDeptPathToDedicatedTool() {
        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")), Map.of("template", "dept_path"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("query_dept_path");
        verifyNoInteractions(queryService);
    }

    @Test
    void delegatesSearchWithKeywordAndType() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.search("研发部", "dept", SCOPE)).thenReturn(new GraphRows<>(List.of(
                new GraphNodeMatch("dept", 2L, "研发部", "RD", "总经办", null)), false));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "search_nodes", "keyword", "研发部", "nodeType", "dept"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.OK);
        assertThat(outcome.content()).contains("研发部").contains("\"truncated\":false");
    }

    @Test
    void failsWhenSearchKeywordMissing() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.search(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("缺少查询关键词"));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "search_nodes"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("关键词");
    }

    @Test
    void failsOnUnsupportedNodeType() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.search("研发", "role", SCOPE))
                .thenThrow(new IllegalArgumentException("不支持的节点类型：role"));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "search_nodes", "keyword", "研发", "nodeType", "role"));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("节点类型");
    }

    @Test
    void passesSearchTruncationFlag() {
        when(scopeResolver.resolve(10L, "token")).thenReturn(SCOPE);
        when(queryService.search("研发", null, SCOPE)).thenReturn(new GraphRows<>(List.of(), true));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "search_nodes", "keyword", "研发"));

        assertThat(outcome.content()).contains("\"truncated\":true");
    }

    @Test
    void failsClosedWhenScopeUnavailable() {
        when(scopeResolver.resolve(10L, "token")).thenThrow(new GraphScopeUnavailableException("范围不可用"));

        ToolOutcome outcome = tool.execute(context(Set.of("ai:graph:use")),
                Map.of("template", "dept_members", "deptId", 2));

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.FAILED);
        assertThat(outcome.content()).contains("范围不可用");
        verify(queryService, never()).deptMembers(anyLong(), any());
    }

    private AgentContext context(Set<String> permissions) {
        return AgentContext.start(10L, "token", permissions, action -> { });
    }
}
