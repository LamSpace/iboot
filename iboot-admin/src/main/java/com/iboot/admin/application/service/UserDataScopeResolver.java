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

import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.enums.DataScopeEnum;
import com.iboot.admin.domain.system.model.UserDataScope;
import com.iboot.admin.infrastructure.persistence.mapper.DeptMapper;
import com.iboot.admin.infrastructure.persistence.mapper.RoleMapper;
import com.iboot.admin.infrastructure.persistence.mapper.UserMapper;
import com.iboot.admin.infrastructure.persistence.po.RolePO;
import com.iboot.admin.infrastructure.persistence.po.UserPO;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户数据权限范围解析服务。
 *
 * <p>按用户所有角色的数据范围计算最终可见范围，并向 MyBatis 数据权限拦截器与只读
 * scope 接口提供同一份结果。角色范围按数值优先级合并（数值越小优先级越高），
 * 自定义范围合并所有自定义角色的部门集合。计算结果按用户缓存 5 分钟，角色或部门变更后
 * 可调用 {@link #clearUserScopeCache(Long)} 主动失效。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class UserDataScopeResolver {

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final ObjectProvider<RoleMapper> roleMapperProvider;

    private final ObjectProvider<DeptMapper> deptMapperProvider;

    private final ObjectProvider<UserMapper> userMapperProvider;

    /**
     * 用户数据权限缓存，避免每次查询都访问角色与部门表。
     */
    private final Map<Long, CachedScope> userScopeCache = new ConcurrentHashMap<>();

    /**
     * 创建解析服务。
     *
     * @param roleMapperProvider 角色 Mapper 提供者
     * @param deptMapperProvider 部门 Mapper 提供者
     * @param userMapperProvider 用户 Mapper 提供者
     */
    public UserDataScopeResolver(ObjectProvider<RoleMapper> roleMapperProvider,
                                 ObjectProvider<DeptMapper> deptMapperProvider,
                                 ObjectProvider<UserMapper> userMapperProvider) {
        this.roleMapperProvider = roleMapperProvider;
        this.deptMapperProvider = deptMapperProvider;
        this.userMapperProvider = userMapperProvider;
    }

    /**
     * 解析当前登录用户的数据权限范围。
     *
     * @return 数据权限范围
     */
    public UserDataScope resolveCurrentUser() {
        return resolve(SecurityUtils.getCurrentUserId(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 解析指定用户的数据权限范围。
     *
     * @param userId   用户 ID
     * @param username 用户名；超级管理员跳过数据权限
     * @return 数据权限范围；用户无角色时 {@code dataScope} 为 {@code null}
     */
    public UserDataScope resolve(Long userId, String username) {
        if (Constants.ADMIN_USERNAME.equals(username)) {
            UserPO user = userMapperProvider.getObject().selectById(userId);
            Long deptId = user == null ? null : user.getDeptId();
            return new UserDataScope(DataScopeEnum.ALL.getCode(), deptId, List.of(), deptSubtree(deptId), true);
        }
        CachedScope cached = userScopeCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return cached.scope();
        }
        UserDataScope scope = compute(userId);
        userScopeCache.put(userId, new CachedScope(scope, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        return scope;
    }

    /**
     * 清除用户数据权限缓存。
     *
     * @param userId 用户 ID；为 {@code null} 时清空全部
     */
    public void clearUserScopeCache(Long userId) {
        if (userId == null) {
            userScopeCache.clear();
        } else {
            userScopeCache.remove(userId);
        }
    }

    private UserDataScope compute(Long userId) {
        UserPO user = userMapperProvider.getObject().selectById(userId);
        Long deptId = user == null ? null : user.getDeptId();
        List<RolePO> roles = roleMapperProvider.getObject().selectByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return new UserDataScope(null, deptId, List.of(), List.of(), false);
        }
        Integer dataScope = null;
        LinkedHashSet<Long> customDeptIds = new LinkedHashSet<>();
        for (RolePO role : roles) {
            Integer roleScope = role.getDataScope();
            if (roleScope == null) {
                continue;
            }
            if (DataScopeEnum.ALL.getCode().equals(roleScope)) {
                dataScope = roleScope;
                customDeptIds.clear();
                break;
            }
            if (DataScopeEnum.CUSTOM.getCode().equals(roleScope)) {
                List<Long> deptIds = roleMapperProvider.getObject().selectDeptIdsByRoleId(role.getId());
                if (deptIds != null) {
                    customDeptIds.addAll(deptIds);
                }
            }
            if (dataScope == null || roleScope < dataScope) {
                dataScope = roleScope;
            }
        }
        List<Long> visibleDeptIds = visibleDeptIds(dataScope, deptId, customDeptIds);
        List<Long> kbVisibleDeptIds = kbVisibleDeptIds(dataScope, deptId, customDeptIds);
        return new UserDataScope(dataScope, deptId, visibleDeptIds, kbVisibleDeptIds, false);
    }

    private List<Long> visibleDeptIds(Integer dataScope, Long deptId, LinkedHashSet<Long> customDeptIds) {
        if (DataScopeEnum.CUSTOM.getCode().equals(dataScope)) {
            return new ArrayList<>(customDeptIds);
        }
        if (DataScopeEnum.DEPT.getCode().equals(dataScope)) {
            return deptId == null ? List.of() : List.of(deptId);
        }
        if (DataScopeEnum.DEPT_AND_CHILD.getCode().equals(dataScope)) {
            return deptSubtree(deptId);
        }
        return List.of();
    }

    private List<Long> kbVisibleDeptIds(Integer dataScope, Long deptId, LinkedHashSet<Long> customDeptIds) {
        if (DataScopeEnum.CUSTOM.getCode().equals(dataScope)) {
            return new ArrayList<>(customDeptIds);
        }
        if (DataScopeEnum.DEPT.getCode().equals(dataScope) || DataScopeEnum.SELF.getCode().equals(dataScope)) {
            return deptId == null ? List.of() : List.of(deptId);
        }
        if (DataScopeEnum.DEPT_AND_CHILD.getCode().equals(dataScope) || DataScopeEnum.ALL.getCode().equals(dataScope)) {
            return deptSubtree(deptId);
        }
        return List.of();
    }

    private List<Long> deptSubtree(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        List<Long> children = deptMapperProvider.getObject().selectChildrenIds(deptId);
        if (children != null) {
            ids.addAll(children);
        }
        ids.add(deptId);
        return new ArrayList<>(ids);
    }

    /**
     * 带过期时间的缓存项。
     *
     * @param scope     数据权限范围
     * @param expiresAt 过期时间戳（毫秒）
     */
    private record CachedScope(UserDataScope scope, long expiresAt) {

        /**
         * 判断缓存是否过期。
         *
         * @return 已过期时返回 {@code true}
         */
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
