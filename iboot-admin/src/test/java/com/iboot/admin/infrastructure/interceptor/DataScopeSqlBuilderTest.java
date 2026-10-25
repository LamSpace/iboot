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

package com.iboot.admin.infrastructure.interceptor;

import com.iboot.admin.common.enums.DataScopeEnum;
import com.iboot.admin.domain.system.model.UserDataScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeSqlBuilder} 的单元测试。
 *
 * <p>锁定五级数据权限生成的 SQL 片段，确保从 {@code DataScopeInterceptor} 抽取解析逻辑后
 * 既有行为不变。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DataScopeSqlBuilderTest {

    private final DataScopeSqlBuilder builder = new DataScopeSqlBuilder();

    /**
     * 全部数据权限不追加过滤条件。
     */
    @Test
    void allScopeReturnsNull() {
        assertThat(builder.build(scope(DataScopeEnum.ALL, List.of()), "u", false, 7L)).isNull();
    }

    /**
     * 自定义权限按部门集合过滤。
     */
    @Test
    void customScopeUsesDeptList() {
        assertThat(builder.build(scope(DataScopeEnum.CUSTOM, List.of(10L, 20L)), "u", false, 7L))
                .isEqualTo("u.dept_id IN (10,20)");
    }

    /**
     * 自定义权限部门集合为空时拒绝全部数据。
     */
    @Test
    void customScopeWithEmptyDeptsDeniesAll() {
        assertThat(builder.build(scope(DataScopeEnum.CUSTOM, List.of()), "u", false, 7L)).isEqualTo("1=0");
    }

    /**
     * 本部门权限使用用户部门子查询。
     */
    @Test
    void deptScopeUsesUserDeptSubquery() {
        assertThat(builder.build(scope(DataScopeEnum.DEPT, List.of()), "u", false, 7L))
                .isEqualTo("u.dept_id = (SELECT dept_id FROM sys_user WHERE id = 7)");
    }

    /**
     * 部门表使用 id 列并直接比较已解析的部门 ID。
     */
    @Test
    void deptTableUsesIdColumn() {
        assertThat(builder.build(scope(DataScopeEnum.DEPT, List.of()), "d", true, 7L))
                .isEqualTo("d.id = 99");
    }

    /**
     * 部门表且用户无部门时拒绝全部数据。
     */
    @Test
    void deptTableWithoutUserDeptDeniesAll() {
        UserDataScope scope = new UserDataScope(DataScopeEnum.DEPT.getCode(), null, List.of(), List.of(), false);
        assertThat(builder.build(scope, "d", true, 7L)).isEqualTo("1=0");
    }

    /**
     * 本部门及下级权限按已解析的部门集合过滤。
     */
    @Test
    void deptAndChildScopeUsesResolvedDeptList() {
        assertThat(builder.build(scope(DataScopeEnum.DEPT_AND_CHILD, List.of(3L, 4L, 5L)), "u", false, 7L))
                .isEqualTo("u.dept_id IN (3,4,5)");
    }

    /**
     * 仅本人权限按用户 ID 过滤。
     */
    @Test
    void selfScopeUsesUserId() {
        assertThat(builder.build(scope(DataScopeEnum.SELF, List.of()), "u", false, 7L)).isEqualTo("u.id = 7");
    }

    /**
     * 无角色或未知范围不追加过滤条件。
     */
    @Test
    void missingScopeReturnsNull() {
        assertThat(builder.build(new UserDataScope(null, null, List.of(), List.of(), false), "u", false, 7L)).isNull();
    }

    private UserDataScope scope(DataScopeEnum dataScope, List<Long> visibleDeptIds) {
        return new UserDataScope(dataScope.getCode(), 99L, visibleDeptIds, visibleDeptIds, false);
    }
}
