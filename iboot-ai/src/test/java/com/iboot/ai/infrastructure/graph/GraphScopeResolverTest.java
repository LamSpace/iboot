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

package com.iboot.ai.infrastructure.graph;

import com.iboot.ai.application.service.graph.GraphScopeUnavailableException;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.infrastructure.security.PermissionScopeUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link GraphScopeResolver} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphScopeResolverTest {

    private final PermissionScopeClient scopeClient = mock(PermissionScopeClient.class);

    private final GraphScopeResolver resolver = new GraphScopeResolver(scopeClient);

    @Test
    void mapsPermissionScopeToQueryScope() {
        when(scopeClient.fetchScope(10L, "token")).thenReturn(new PermissionScope(10L, "alice", false, 4, 2L,
                List.of(2L, 3L), List.of(2L, 3L), List.of("ai:graph:use"), List.of()));

        QueryScope scope = resolver.resolve(10L, "token");

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.visibleDeptIds()).containsExactly(2L, 3L);
    }

    @Test
    void superAdminIsUnrestricted() {
        when(scopeClient.fetchScope(1L, "token")).thenReturn(new PermissionScope(1L, "root", true, 1, 1L,
                List.of(), List.of(), List.of(), List.of()));

        assertThat(resolver.resolve(1L, "token").unrestricted()).isTrue();
    }

    @Test
    void failsClosedWhenScopeUnavailable() {
        when(scopeClient.fetchScope(10L, "token"))
                .thenThrow(new PermissionScopeUnavailableException("down"));

        assertThatThrownBy(() -> resolver.resolve(10L, "token"))
                .isInstanceOf(GraphScopeUnavailableException.class);
    }
}
