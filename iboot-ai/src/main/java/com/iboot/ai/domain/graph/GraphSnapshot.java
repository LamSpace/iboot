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

package com.iboot.ai.domain.graph;

import java.util.List;

/**
 * 一次同步的完整组织快照。
 *
 * <p>{@code syncId} 会盖到本次写入的每个节点上，写事务末尾据此清理已删除实体。
 * 关系由节点自身字段推导：部门层级取自 {@link DeptNode#parentId()}，用户归属取自
 * {@link UserNode#deptId()}，角色授权取自 {@link RoleNode#menuIds()} 与
 * {@link RoleNode#deptIds()}，用户角色取自 {@link UserNode#roleIds()}。
 *
 * @param syncId      本次同步标识
 * @param departments 部门节点
 * @param users       用户节点
 * @param roles       角色节点
 * @param menus       菜单节点
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphSnapshot(String syncId, List<DeptNode> departments, List<UserNode> users,
                            List<RoleNode> roles, List<MenuNode> menus) {

    /**
     * 规范化集合字段，避免调用方处理 {@code null}。
     */
    public GraphSnapshot {
        departments = departments == null ? List.of() : List.copyOf(departments);
        users = users == null ? List.of() : List.copyOf(users);
        roles = roles == null ? List.of() : List.copyOf(roles);
        menus = menus == null ? List.of() : List.copyOf(menus);
    }
}
