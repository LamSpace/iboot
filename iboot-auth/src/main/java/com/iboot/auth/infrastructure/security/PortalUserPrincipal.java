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

package com.iboot.auth.infrastructure.security;

import com.iboot.auth.domain.model.AuthUser;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录页认证通过后的用户主体。
 *
 * <p>作为 Spring Security 的 {@code principal} 在授权码流程中传递，令牌定制器从这里读取
 * 用户标识、角色、权限与登录上下文，写入令牌声明与共享会话。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @param user 用户
 * @param permissions 权限码集合
 * @param roles 角色编码集合
 * @param loginIp 登录 IP
 * @param browser 浏览器
 * @param os 操作系统
 * @param loginTime 登录时间
 */
public record PortalUserPrincipal(AuthUser user, List<String> permissions, List<String> roles,
                                  String loginIp, String browser, String os, LocalDateTime loginTime)
        implements Principal {

    /**
     * 认证主体名称，取用户名。
     *
     * @return 用户名
     */
    @Override
    public String getName() {
        return user.username();
    }
}
