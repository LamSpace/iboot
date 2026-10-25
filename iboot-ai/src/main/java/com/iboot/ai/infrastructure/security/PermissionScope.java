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

package com.iboot.ai.infrastructure.security;

import java.util.List;

/**
 * 当前用户的数据权限范围快照。
 *
 * <p>由 admin 的只读 scope 接口提供，供知识库检索在查询前构造 ES 过滤条件。
 * {@code dataScope} 为空表示用户没有可用角色，调用方必须按失败关闭处理。
 *
 * @param userId         用户 ID
 * @param username       用户名
 * @param superAdmin     是否超级管理员
 * @param dataScope      五级数据权限码
 * @param deptId         用户所在部门 ID
 * @param visibleDeptIds   数据权限过滤使用的可见部门集合
 * @param kbVisibleDeptIds 知识库共享判断使用的部门集合
 * @param permissions      RBAC 权限标识
 * @param roleCodes        角色编码
 * @author iBoot Team
 * @since 1.0.0
 */
public record PermissionScope(Long userId, String username, boolean superAdmin, Integer dataScope, Long deptId,
                              List<Long> visibleDeptIds, List<Long> kbVisibleDeptIds, List<String> permissions,
                              List<String> roleCodes) {

    /**
     * 规范化集合字段，避免调用方处理 {@code null}。
     */
    public PermissionScope {
        visibleDeptIds = visibleDeptIds == null ? List.of() : List.copyOf(visibleDeptIds);
        kbVisibleDeptIds = kbVisibleDeptIds == null ? List.of() : List.copyOf(kbVisibleDeptIds);
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
    }
}
