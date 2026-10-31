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

package com.iboot.file.infrastructure.interceptor;

import com.iboot.file.infrastructure.security.FilePermissionScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataScopeSqlBuilder} 的过滤片段测试。
 *
 * <p>文件列表的数据权限此前是坏的（`sys_file` 没有 `dept_id`）；这里的断言把修正后的语义
 * 钉住：过滤条件一律落在关联出来的用户表别名上，而不是文件表。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DataScopeSqlBuilderTest {

    private final DataScopeSqlBuilder builder = new DataScopeSqlBuilder();

    /**
     * 全部数据权限不追加任何条件。
     */
    @Test
    void allScopeAddsNothing() {
        assertThat(builder.build(new FilePermissionScope(1, 1L, List.of(), false), "u", false, 7L)).isNull();
    }

    /**
     * 本部门范围按用户表别名过滤部门。
     */
    @Test
    void deptScopeFiltersJoinedUserDept() {
        String sql = builder.build(new FilePermissionScope(3, 1L, List.of(), false), "u", false, 7L);

        assertThat(sql).isEqualTo("u.dept_id = (SELECT dept_id FROM sys_user WHERE id = 7)");
    }

    /**
     * 自定义范围使用可见部门集合。
     */
    @Test
    void customScopeUsesVisibleDeptIds() {
        String sql = builder.build(new FilePermissionScope(2, 1L, List.of(2L, 3L), false), "u", false, 7L);

        assertThat(sql).isEqualTo("u.dept_id IN (2,3)");
    }

    /**
     * 仅本人范围按用户主键过滤，而不是按文件主键。
     */
    @Test
    void selfScopeFiltersByJoinedUserId() {
        String sql = builder.build(new FilePermissionScope(5, 1L, List.of(), false), "u", false, 7L);

        assertThat(sql).isEqualTo("u.id = 7");
    }

    /**
     * 可见部门为空的自定义范围必须收紧为恒假，不能放行。
     */
    @Test
    void emptyCustomScopeFailsClosed() {
        String sql = builder.build(new FilePermissionScope(2, 1L, List.of(), false), "u", false, 7L);

        assertThat(sql).isEqualTo("1=0");
    }
}
