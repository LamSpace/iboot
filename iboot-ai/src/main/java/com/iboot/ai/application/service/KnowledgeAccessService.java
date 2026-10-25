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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 知识库可见范围与文档数据权限计算服务。
 *
 * <p>把 admin 提供的权限范围快照转换为两类结果：用户可见的知识库 ID 集合，以及文档级
 * 数据权限过滤条件。任何缺失或非法权限都按失败关闭处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class KnowledgeAccessService {

    private static final int SCOPE_ALL = 1;

    private static final int SCOPE_CUSTOM = 2;

    private static final int SCOPE_DEPT = 3;

    private static final int SCOPE_DEPT_AND_CHILD = 4;

    private static final int SCOPE_SELF = 5;

    /**
     * 计算用户可见的知识库 ID。
     *
     * @param scope 权限范围快照
     * @param bases 知识库摘要列表
     * @return 可见知识库 ID；无可见知识库时为空列表
     */
    public List<Long> visibleKnowledgeBaseIds(PermissionScope scope, List<KnowledgeBaseSummary> bases) {
        if (scope == null || scope.dataScope() == null || bases == null || bases.isEmpty()) {
            return List.of();
        }
        if (documentScopeFilter(scope).mode() == DocumentScopeFilter.Mode.DENY) {
            return List.of();
        }
        List<Long> visible = new ArrayList<>();
        for (KnowledgeBaseSummary base : bases) {
            if (isKnowledgeBaseVisible(scope, base)) {
                visible.add(base.id());
            }
        }
        return visible;
    }

    /**
     * 计算文档级数据权限过滤条件。
     *
     * @param scope 权限范围快照
     * @return 过滤条件；无法确定权限时返回拒绝全部
     */
    public DocumentScopeFilter documentScopeFilter(PermissionScope scope) {
        if (scope == null) {
            return DocumentScopeFilter.deny();
        }
        if (scope.superAdmin() || Integer.valueOf(SCOPE_ALL).equals(scope.dataScope())) {
            return DocumentScopeFilter.all();
        }
        if (Integer.valueOf(SCOPE_CUSTOM).equals(scope.dataScope())) {
            return scope.visibleDeptIds().isEmpty()
                    ? DocumentScopeFilter.deny()
                    : new DocumentScopeFilter(DocumentScopeFilter.Mode.DEPTS, null, null, scope.visibleDeptIds());
        }
        if (Integer.valueOf(SCOPE_DEPT).equals(scope.dataScope())) {
            return scope.deptId() == null
                    ? DocumentScopeFilter.deny()
                    : new DocumentScopeFilter(DocumentScopeFilter.Mode.DEPT, null, scope.deptId(), List.of());
        }
        if (Integer.valueOf(SCOPE_DEPT_AND_CHILD).equals(scope.dataScope())) {
            return scope.visibleDeptIds().isEmpty()
                    ? DocumentScopeFilter.deny()
                    : new DocumentScopeFilter(DocumentScopeFilter.Mode.DEPTS, null, null, scope.visibleDeptIds());
        }
        if (Integer.valueOf(SCOPE_SELF).equals(scope.dataScope())) {
            return new DocumentScopeFilter(DocumentScopeFilter.Mode.SELF, scope.userId(), null, List.of());
        }
        return DocumentScopeFilter.deny();
    }

    private boolean isKnowledgeBaseVisible(PermissionScope scope, KnowledgeBaseSummary base) {
        if (base == null || base.visibility() == null) {
            return false;
        }
        return switch (base.visibility()) {
            case SCOPE_ALL -> true;
            case SCOPE_CUSTOM, SCOPE_DEPT, SCOPE_DEPT_AND_CHILD ->
                    base.sharedDeptIds().stream().anyMatch(scope.kbVisibleDeptIds()::contains);
            case SCOPE_SELF -> Objects.equals(base.ownerUserId(), scope.userId());
            default -> false;
        };
    }
}
