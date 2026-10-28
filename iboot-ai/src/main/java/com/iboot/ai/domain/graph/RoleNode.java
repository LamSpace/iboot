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
 * 图中的一个角色节点。
 *
 * <p>{@code menuIds} 生成角色授予菜单的关系，{@code deptIds} 生成自定义数据权限范围关系。
 *
 * @param id        角色主键
 * @param code      角色编码
 * @param name      角色名称
 * @param dataScope 数据权限码
 * @param status    状态：0-停用，1-正常
 * @param menuIds   授予的菜单主键列表
 * @param deptIds   自定义数据权限范围的部门主键列表
 * @author iBoot Team
 * @since 1.0.0
 */
public record RoleNode(Long id, String code, String name, Integer dataScope, Integer status,
                       List<Long> menuIds, List<Long> deptIds) {

    /**
     * 规范化集合字段，避免调用方处理 {@code null}。
     */
    public RoleNode {
        menuIds = menuIds == null ? List.of() : List.copyOf(menuIds);
        deptIds = deptIds == null ? List.of() : List.copyOf(deptIds);
    }
}
