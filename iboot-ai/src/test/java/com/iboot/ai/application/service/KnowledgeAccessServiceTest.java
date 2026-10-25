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

package com.iboot.ai.application.service;

import com.iboot.ai.domain.knowledge.DocumentScopeFilter;
import com.iboot.ai.domain.knowledge.KnowledgeBaseSummary;
import com.iboot.ai.infrastructure.security.PermissionScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KnowledgeAccessService} 的单元测试。
 *
 * <p>覆盖五级数据权限与五级知识库可见范围的组合，确认知识库列表与文档过滤条件都不越权。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class KnowledgeAccessServiceTest {

    private final KnowledgeAccessService service = new KnowledgeAccessService();

    /**
     * 全部数据权限可看到公开、本部门、下级与自定义共享的知识库，但看不到他人的仅本人知识库。
     */
    @Test
    void allScopeCombinesKbVisibility() {
        PermissionScope scope = scope(1, 2L, 100L, List.of(), List.of(2L, 3L), false);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 1, 999L, 9L),
                kb(2L, 3, 999L, 2L, 2L),
                kb(3L, 4, 999L, 3L, 3L),
                kb(4L, 5, 999L, 2L),
                kb(5L, 2, 999L, 2L, 3L),
                kb(6L, 3, 999L, 9L, 9L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 2L, 3L, 5L);
        assertThat(service.documentScopeFilter(scope).mode()).isEqualTo(DocumentScopeFilter.Mode.ALL);
    }

    /**
     * 仅本人数据权限只能看到自己的仅本人知识库与共享给本部门的知识库。
     */
    @Test
    void selfScopeOnlySeesOwnKb() {
        PermissionScope scope = scope(5, 2L, 100L, List.of(), List.of(2L), false);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 1, 999L, 9L),
                kb(2L, 5, 100L, 2L),
                kb(3L, 5, 101L, 2L),
                kb(4L, 3, 999L, 2L, 2L),
                kb(5L, 3, 999L, 3L, 3L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 2L, 4L);
        DocumentScopeFilter filter = service.documentScopeFilter(scope);
        assertThat(filter.mode()).isEqualTo(DocumentScopeFilter.Mode.SELF);
        assertThat(filter.userId()).isEqualTo(100L);
    }

    /**
     * 本部门数据权限只能看到本部门共享的知识库。
     */
    @Test
    void deptScopeSeesOwnDeptKb() {
        PermissionScope scope = scope(3, 2L, 100L, List.of(2L), List.of(2L), false);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 3, 999L, 2L, 2L),
                kb(2L, 3, 999L, 3L, 3L),
                kb(3L, 4, 999L, 2L, 2L),
                kb(4L, 1, 999L, 9L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 3L, 4L);
        assertThat(service.documentScopeFilter(scope).mode()).isEqualTo(DocumentScopeFilter.Mode.DEPT);
        assertThat(service.documentScopeFilter(scope).deptId()).isEqualTo(2L);
    }

    /**
     * 自定义数据权限按授权部门集合过滤知识库。
     */
    @Test
    void customScopeUsesCustomDepts() {
        PermissionScope scope = scope(2, 3L, 100L, List.of(2L, 4L), List.of(2L, 4L), false);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 2, 999L, 9L, 4L),
                kb(2L, 2, 999L, 9L, 5L),
                kb(3L, 3, 999L, 2L, 2L),
                kb(4L, 1, 999L, 9L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 3L, 4L);
        DocumentScopeFilter filter = service.documentScopeFilter(scope);
        assertThat(filter.mode()).isEqualTo(DocumentScopeFilter.Mode.DEPTS);
        assertThat(filter.deptIds()).containsExactly(2L, 4L);
    }

    /**
     * 自定义部门集合为空时拒绝全部数据与知识库。
     */
    @Test
    void customScopeWithEmptyDeptsDeniesAll() {
        PermissionScope scope = scope(2, 3L, 100L, List.of(), List.of(), false);
        List<KnowledgeBaseSummary> bases = List.of(kb(1L, 1, 999L, 9L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).isEmpty();
        assertThat(service.documentScopeFilter(scope).mode()).isEqualTo(DocumentScopeFilter.Mode.DENY);
    }

    /**
     * 无角色时失败关闭。
     */
    @Test
    void missingDataScopeDeniesAll() {
        PermissionScope scope = scope(null, 2L, 100L, List.of(), List.of(), false);
        List<KnowledgeBaseSummary> bases = List.of(kb(1L, 1, 999L, 9L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).isEmpty();
        assertThat(service.documentScopeFilter(scope).mode()).isEqualTo(DocumentScopeFilter.Mode.DENY);
    }

    /**
     * 超级管理员跳过数据权限，但不跳过知识库可见范围。
     */
    @Test
    void superAdminSkipsDataScopeButRespectsKbVisibility() {
        PermissionScope scope = scope(1, 1L, 1L, List.of(), List.of(1L, 2L), true);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 1, 999L, 9L),
                kb(2L, 5, 999L, 1L),
                kb(3L, 4, 999L, 2L, 2L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 3L);
        assertThat(service.documentScopeFilter(scope).mode()).isEqualTo(DocumentScopeFilter.Mode.ALL);
    }

    /**
     * 本部门及下级数据权限可看到下级部门共享的知识库。
     */
    @Test
    void deptAndChildScopeSeesChildKb() {
        PermissionScope scope = scope(4, 2L, 100L, List.of(3L, 4L, 2L), List.of(3L, 4L, 2L), false);
        List<KnowledgeBaseSummary> bases = List.of(
                kb(1L, 4, 999L, 3L, 3L),
                kb(2L, 4, 999L, 9L, 9L),
                kb(3L, 3, 999L, 2L, 2L));

        assertThat(service.visibleKnowledgeBaseIds(scope, bases)).containsExactly(1L, 3L);
        DocumentScopeFilter filter = service.documentScopeFilter(scope);
        assertThat(filter.mode()).isEqualTo(DocumentScopeFilter.Mode.DEPTS);
        assertThat(filter.deptIds()).containsExactly(3L, 4L, 2L);
    }

    private PermissionScope scope(Integer dataScope, Long deptId, Long userId, List<Long> visibleDeptIds,
                                  List<Long> kbVisibleDeptIds, boolean superAdmin) {
        return new PermissionScope(userId, "test", superAdmin, dataScope, deptId, visibleDeptIds,
                kbVisibleDeptIds, List.of("ai:kb:query"), List.of());
    }

    private KnowledgeBaseSummary kb(Long id, int visibility, Long ownerUserId, Long ownerDeptId, Long... sharedDeptIds) {
        return new KnowledgeBaseSummary(id, visibility, ownerUserId, ownerDeptId, List.of(sharedDeptIds));
    }
}
