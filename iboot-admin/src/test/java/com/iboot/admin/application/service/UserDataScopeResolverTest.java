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

package com.iboot.admin.application.service;

import com.iboot.admin.common.enums.DataScopeEnum;
import com.iboot.admin.domain.system.model.UserDataScope;
import com.iboot.admin.infrastructure.persistence.mapper.DeptMapper;
import com.iboot.admin.infrastructure.persistence.mapper.RoleMapper;
import com.iboot.admin.infrastructure.persistence.mapper.UserMapper;
import com.iboot.admin.infrastructure.persistence.po.RolePO;
import com.iboot.admin.infrastructure.persistence.po.UserPO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserDataScopeResolver} 的单元测试。
 *
 * <p>覆盖五级数据权限解析、超级管理员、自定义部门并集、部门及下级展开与缓存。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class UserDataScopeResolverTest {

    private RoleMapper roleMapper;

    private DeptMapper deptMapper;

    private UserMapper userMapper;

    private UserDataScopeResolver resolver;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        roleMapper = mock(RoleMapper.class);
        deptMapper = mock(DeptMapper.class);
        userMapper = mock(UserMapper.class);
        ObjectProvider<RoleMapper> roleProvider = mock(ObjectProvider.class);
        ObjectProvider<DeptMapper> deptProvider = mock(ObjectProvider.class);
        ObjectProvider<UserMapper> userProvider = mock(ObjectProvider.class);
        when(roleProvider.getObject()).thenReturn(roleMapper);
        when(deptProvider.getObject()).thenReturn(deptMapper);
        when(userProvider.getObject()).thenReturn(userMapper);
        resolver = new UserDataScopeResolver(roleProvider, deptProvider, userProvider);
    }

    /**
     * 超级管理员获得全部数据权限。
     */
    @Test
    void superAdminGetsAllScope() {
        UserPO user = user(1L, 1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserDataScope scope = resolver.resolve(1L, "admin");

        assertThat(scope.superAdmin()).isTrue();
        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.ALL.getCode());
        assertThat(scope.deptId()).isEqualTo(1L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(1L);
    }

    /**
     * 仅本人权限不展开部门集合。
     */
    @Test
    void selfScopeHasNoDepartments() {
        UserPO user = user(100L, 2L);
        RolePO role = role(10L, 5);
        when(userMapper.selectById(100L)).thenReturn(user);
        when(roleMapper.selectByUserId(100L)).thenReturn(List.of(role));

        UserDataScope scope = resolver.resolve(100L, "rag_self");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.SELF.getCode());
        assertThat(scope.deptId()).isEqualTo(2L);
        assertThat(scope.visibleDeptIds()).isEmpty();
        assertThat(scope.kbVisibleDeptIds()).containsExactly(2L);
    }

    /**
     * 本部门权限只包含用户所在部门。
     */
    @Test
    void deptScopeContainsOwnDept() {
        UserPO user = user(101L, 2L);
        RolePO role = role(11L, 3);
        when(userMapper.selectById(101L)).thenReturn(user);
        when(roleMapper.selectByUserId(101L)).thenReturn(List.of(role));

        UserDataScope scope = resolver.resolve(101L, "rag_dept");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.DEPT.getCode());
        assertThat(scope.visibleDeptIds()).containsExactly(2L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(2L);
    }

    /**
     * 本部门及下级权限展开子部门。
     */
    @Test
    void deptAndChildScopeExpandsChildren() {
        UserPO user = user(102L, 2L);
        RolePO role = role(12L, 4);
        when(userMapper.selectById(102L)).thenReturn(user);
        when(roleMapper.selectByUserId(102L)).thenReturn(List.of(role));
        when(deptMapper.selectChildrenIds(2L)).thenReturn(List.of(3L, 4L));

        UserDataScope scope = resolver.resolve(102L, "rag_child");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.DEPT_AND_CHILD.getCode());
        assertThat(scope.visibleDeptIds()).containsExactly(3L, 4L, 2L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(3L, 4L, 2L);
    }

    /**
     * 自定义权限合并多个角色的部门集合。
     */
    @Test
    void customScopeMergesRoleDepartments() {
        UserPO user = user(103L, 3L);
        RolePO first = role(13L, 2);
        RolePO second = role(14L, 2);
        when(userMapper.selectById(103L)).thenReturn(user);
        when(roleMapper.selectByUserId(103L)).thenReturn(List.of(first, second));
        when(roleMapper.selectDeptIdsByRoleId(13L)).thenReturn(List.of(2L, 4L));
        when(roleMapper.selectDeptIdsByRoleId(14L)).thenReturn(List.of(4L, 5L));

        UserDataScope scope = resolver.resolve(103L, "rag_custom");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.CUSTOM.getCode());
        assertThat(scope.visibleDeptIds()).containsExactly(2L, 4L, 5L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(2L, 4L, 5L);
    }

    /**
     * 全部数据权限优先于其他范围。
     */
    @Test
    void allScopeWinsOverNarrowerScopes() {
        UserPO user = user(104L, 2L);
        RolePO deptRole = role(15L, 3);
        RolePO allRole = role(16L, 1);
        when(userMapper.selectById(104L)).thenReturn(user);
        when(roleMapper.selectByUserId(104L)).thenReturn(List.of(deptRole, allRole));
        when(deptMapper.selectChildrenIds(2L)).thenReturn(List.of(3L, 4L));

        UserDataScope scope = resolver.resolve(104L, "rag_all");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.ALL.getCode());
        assertThat(scope.kbVisibleDeptIds()).containsExactly(3L, 4L, 2L);
    }

    /**
     * 自定义范围优先于更窄的范围，且不受角色顺序影响。
     */
    @Test
    void customScopeWinsOverNarrowerScopeRegardlessOfOrder() {
        UserPO user = user(107L, 2L);
        RolePO deptRole = role(18L, 3);
        RolePO customRole = role(19L, 2);
        when(userMapper.selectById(107L)).thenReturn(user);
        when(roleMapper.selectByUserId(107L)).thenReturn(List.of(deptRole, customRole));
        when(roleMapper.selectDeptIdsByRoleId(19L)).thenReturn(List.of(4L));

        UserDataScope scope = resolver.resolve(107L, "rag_mixed");

        assertThat(scope.dataScope()).isEqualTo(DataScopeEnum.CUSTOM.getCode());
        assertThat(scope.visibleDeptIds()).containsExactly(4L);
        assertThat(scope.kbVisibleDeptIds()).containsExactly(4L);
    }

    /**
     * 用户无角色时返回空范围。
     */
    @Test
    void noRolesReturnsNullScope() {
        UserPO user = user(105L, 2L);
        when(userMapper.selectById(105L)).thenReturn(user);
        when(roleMapper.selectByUserId(105L)).thenReturn(List.of());

        UserDataScope scope = resolver.resolve(105L, "no_role");

        assertThat(scope.dataScope()).isNull();
        assertThat(scope.kbVisibleDeptIds()).isEmpty();
    }

    /**
     * 同一用户的重复解析命中缓存。
     */
    @Test
    void repeatedResolveUsesCache() {
        UserPO user = user(106L, 2L);
        RolePO role = role(17L, 5);
        when(userMapper.selectById(106L)).thenReturn(user);
        when(roleMapper.selectByUserId(106L)).thenReturn(List.of(role));

        resolver.resolve(106L, "cached");
        resolver.resolve(106L, "cached");

        verify(roleMapper).selectByUserId(106L);
    }

    private RolePO role(Long id, Integer dataScope) {
        RolePO role = mock(RolePO.class);
        when(role.getId()).thenReturn(id);
        when(role.getDataScope()).thenReturn(dataScope);
        return role;
    }

    private UserPO user(Long id, Long deptId) {
        UserPO user = mock(UserPO.class);
        when(user.getId()).thenReturn(id);
        when(user.getDeptId()).thenReturn(deptId);
        return user;
    }
}
