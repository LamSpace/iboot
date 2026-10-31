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

package com.iboot.auth.domain.model;

import java.util.List;

/**
 * 认证所需的用户视图。
 *
 * <p>只携带登录判定与令牌签发真正用到的字段：凭据、展示信息、部门与状态。不包含邮箱、手机号、
 * 岗位等管理端才需要的属性，避免认证服务复制系统域的完整用户模型。
 *
 * @param id 用户 ID
 * @param username 用户名
 * @param password 已加密的密码
 * @param nickname 昵称
 * @param avatar 头像地址
 * @param deptId 所属部门 ID
 * @param status 状态：0-停用，1-正常
 * @param roleIds 角色 ID 列表
 * @author iBoot Team
 * @since 1.0.0
 */
public record AuthUser(Long id, String username, String password, String nickname, String avatar,
                       Long deptId, Integer status, List<Long> roleIds) {

    /**
     * 判断账号是否处于启用状态。
     *
     * @return 状态值为 1 时返回 {@code true}
     */
    public boolean enabled() {
        return status != null && status == 1;
    }
}
