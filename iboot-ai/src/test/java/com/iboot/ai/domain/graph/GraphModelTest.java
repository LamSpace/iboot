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

package com.iboot.ai.domain.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 图领域模型的单元测试。
 *
 * <p>覆盖集合字段的空值规范化与模板名的解析约束。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphModelTest {

    @Test
    void normalizesNullCollections() {
        GraphSnapshot snapshot = new GraphSnapshot("sync-1", null, null, null, null);
        assertThat(snapshot.departments()).isEmpty();
        assertThat(snapshot.users()).isEmpty();
        assertThat(snapshot.roles()).isEmpty();
        assertThat(snapshot.menus()).isEmpty();

        assertThat(new UserNode(1L, "u", "n", 2L, 1, null).roleIds()).isEmpty();
        RoleNode role = new RoleNode(1L, "code", "name", 1, 1, null, null);
        assertThat(role.menuIds()).isEmpty();
        assertThat(role.deptIds()).isEmpty();
        assertThat(new QueryScope(false, null).visibleDeptIds()).isEmpty();
        assertThat(new DeptPath(null, 0).departmentNames()).isEmpty();
    }

    @Test
    void parsesKnownTemplateNames() {
        assertThat(GraphQueryTemplate.fromWire("colleagues")).isEqualTo(GraphQueryTemplate.COLLEAGUES);
        assertThat(GraphQueryTemplate.fromWire("dept_path")).isEqualTo(GraphQueryTemplate.DEPT_PATH);
        assertThat(GraphQueryTemplate.fromWire("search_nodes")).isEqualTo(GraphQueryTemplate.SEARCH_NODES);
    }

    @Test
    void rejectsUnknownTemplateNames() {
        assertThatThrownBy(() -> GraphQueryTemplate.fromWire("cypher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cypher");
        assertThatThrownBy(() -> GraphQueryTemplate.fromWire(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factoriesCarryOnlyTheirOwnParameters() {
        GraphQueryRequest request = GraphQueryRequest.permHolders("user:list");

        assertThat(request.template()).isEqualTo(GraphQueryTemplate.PERM_HOLDERS);
        assertThat(request.permission()).isEqualTo("user:list");
        assertThat(request.userId()).isNull();
        assertThat(request.deptId()).isNull();
    }
}
