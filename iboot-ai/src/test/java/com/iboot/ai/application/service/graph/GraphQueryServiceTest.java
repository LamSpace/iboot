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

package com.iboot.ai.application.service.graph;

import com.iboot.ai.domain.graph.GraphNodeMatch;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.UserRef;
import com.iboot.ai.infrastructure.config.GraphProperties;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GraphQueryService} 的单元测试。
 *
 * <p>覆盖参数校验、身份默认值与「查询他人权限需要 user:list」的本地拒绝。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphQueryServiceTest {

    private final Neo4jGraphRepository repository = mock(Neo4jGraphRepository.class);

    private final GraphQueryService service = new GraphQueryService(repository, new GraphProperties(), metrics());

    private final QueryScope scope = new QueryScope(false, List.of(2L));

    @Test
    void colleaguesDefaultsToCaller() {
        when(repository.colleagues(10L, scope, 51)).thenReturn(List.of());

        service.colleagues(null, 10L, scope);

        verify(repository).colleagues(10L, scope, 51);
    }

    @Test
    void rejectsMissingDepartment() {
        assertThatThrownBy(() -> service.deptMembers(null, scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("部门");
    }

    @Test
    void rejectsMissingPermission() {
        assertThatThrownBy(() -> service.permHolders(" ", scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权限");
    }

    @Test
    void rejectsIncompletePath() {
        assertThatThrownBy(() -> service.deptPath(1L, null, scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("部门");
    }

    @Test
    void rejectsOtherUsersPermissionsWithoutUserList() {
        assertThatThrownBy(() -> service.userPermissions(11L, 10L, scope, Set.of("ai:graph:use")))
                .isInstanceOf(GraphAccessDeniedException.class);

        verify(repository, never()).userPermissions(anyLong(), any(), anyBoolean(), anyInt());
    }

    @Test
    void allowsOtherUsersPermissionsWithUserList() {
        when(repository.userPermissions(11L, scope, false, 51)).thenReturn(List.of());

        service.userPermissions(11L, 10L, scope, Set.of("user:list"));

        verify(repository).userPermissions(11L, scope, false, 51);
    }

    @Test
    void allowsOwnPermissionsWithoutUserList() {
        when(repository.userPermissions(10L, scope, true, 51)).thenReturn(List.of());

        service.userPermissions(null, 10L, scope, Set.of("ai:graph:use"));

        verify(repository).userPermissions(10L, scope, true, 51);
    }

    @Test
    void delegatesColleaguesWithLimitFromProperties() {
        GraphProperties properties = new GraphProperties();
        properties.getQuery().setMaxRows(20);
        GraphQueryService limited = new GraphQueryService(repository, properties, metrics());
        when(repository.colleagues(10L, scope, 21)).thenReturn(List.<UserRef>of());

        limited.colleagues(10L, 10L, scope);

        verify(repository).colleagues(10L, scope, 21);
    }

    @Test
    void truncatesResultsAboveLimit() {
        GraphProperties properties = new GraphProperties();
        properties.getQuery().setMaxRows(2);
        GraphQueryService limited = new GraphQueryService(repository, properties, metrics());
        when(repository.colleagues(10L, scope, 3)).thenReturn(List.of(
                new UserRef(1L, "a", "a", 2L, "研发部"),
                new UserRef(2L, "b", "b", 2L, "研发部"),
                new UserRef(3L, "c", "c", 2L, "研发部")));

        GraphRows<UserRef> rows = limited.colleagues(10L, 10L, scope);

        assertThat(rows.rows()).hasSize(2);
        assertThat(rows.truncated()).isTrue();
    }

    private static AiMetrics metrics() {
        return new AiMetrics(new SimpleMeterRegistry(), "test-model");
    }

    @Test
    void delegatePathsReturnRepositoryResult() {
        when(repository.permHolders(anyString(), any(), anyInt())).thenReturn(List.of());

        assertThat(service.permHolders("user:list", scope).rows()).isEmpty();
    }

    @Test
    void rejectsBlankSearchKeyword() {
        assertThatThrownBy(() -> service.search("  ", "dept", scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("关键词");

        verify(repository, never()).searchDepartments(anyString(), any(), anyInt());
    }

    @Test
    void rejectsUnknownNodeType() {
        assertThatThrownBy(() -> service.search("研发", "role", scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("节点类型");

        verify(repository, never()).searchDepartments(anyString(), any(), anyInt());
    }

    @Test
    void mergesAllNodeTypesWhenTypeOmitted() {
        when(repository.searchDepartments("研发", scope, 51)).thenReturn(List.of(
                new GraphNodeMatch("dept", 2L, "研发部", "RD", "总经办", null)));
        when(repository.searchUsers("研发", scope, 51)).thenReturn(List.of());
        when(repository.searchMenus("研发", scope, 51)).thenReturn(List.of(
                new GraphNodeMatch("menu", 100L, "研发管理", "C", null, "dev:list")));

        GraphRows<GraphNodeMatch> rows = service.search("研发", null, scope);

        assertThat(rows.rows()).extracting(GraphNodeMatch::type).containsExactly("dept", "menu");
        assertThat(rows.truncated()).isFalse();
    }

    @Test
    void delegatesOnlyTheRequestedNodeType() {
        when(repository.searchUsers("研发", scope, 51)).thenReturn(List.of());

        service.search("研发", "user", scope);

        verify(repository).searchUsers("研发", scope, 51);
        verify(repository, never()).searchDepartments(anyString(), any(), anyInt());
        verify(repository, never()).searchMenus(anyString(), any(), anyInt());
    }

    @Test
    void truncatesMergedResultsAboveLimit() {
        GraphProperties properties = new GraphProperties();
        properties.getQuery().setMaxRows(1);
        GraphQueryService limited = new GraphQueryService(repository, properties, metrics());
        when(repository.searchDepartments("研发", scope, 2)).thenReturn(List.of(
                new GraphNodeMatch("dept", 2L, "研发", "RD-ABBR", "总经办", null),
                new GraphNodeMatch("dept", 3L, "研发部", "RD", "总经办", null)));
        when(repository.searchUsers("研发", scope, 2)).thenReturn(List.of());
        when(repository.searchMenus("研发", scope, 2)).thenReturn(List.of());

        GraphRows<GraphNodeMatch> rows = limited.search("研发", "all", scope);

        assertThat(rows.rows()).hasSize(1);
        assertThat(rows.truncated()).isTrue();
    }
}
