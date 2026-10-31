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

import com.iboot.admin.domain.system.model.UserDataScope;
import com.iboot.admin.interfaces.dto.response.UserScopeResponse;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.security.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据权限范围应用服务。
 *
 * <p>登录、登出与注册已迁至 {@code iboot-auth}，本服务只保留 {@code GET /api/auth/scope} 的
 * 组装逻辑：把数据权限解析结果与当前会话中的权限、角色合并为一个只读快照。
 *
 * <p>解析结果按用户缓存，缓存失效由 {@code DataScopeInterceptor} 在角色变更时触发；本服务不写库、
 * 不持有事务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class AuthApplicationService {

    private final UserDataScopeResolver userDataScopeResolver;

    /**
     * 创建服务。
     *
     * @param userDataScopeResolver 数据权限范围解析器
     */
    public AuthApplicationService(final UserDataScopeResolver userDataScopeResolver) {
        this.userDataScopeResolver = userDataScopeResolver;
    }

    /**
     * 获取当前登录用户的数据权限范围。
     *
     * <p>权限取自安全上下文，角色取自会话中的登录信息；两者缺失时返回空列表而非报错，
     * 保证只读接口对匿名调用也不抛异常。
     *
     * @return 当前用户的权限范围快照
     */
    public UserScopeResponse getCurrentUserScope() {
        UserDataScope scope = userDataScopeResolver.resolveCurrentUser();
        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        Authentication authentication = SecurityUtils.getAuthentication();
        List<String> permissions = authentication == null
                ? List.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<String> roles = List.of();
        if (authentication != null && authentication.getPrincipal() instanceof Map<?, ?> principal
                && principal.get(Constants.ROLES) instanceof List<?> roleList) {
            roles = roleList.stream().map(Object::toString).toList();
        }
        return new UserScopeResponse(userId, username, scope.superAdmin(), scope.dataScope(), scope.deptId(),
                scope.visibleDeptIds(), scope.kbVisibleDeptIds(), permissions, roles);
    }
}
