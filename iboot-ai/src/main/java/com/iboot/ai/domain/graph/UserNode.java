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
 * 图中的一个用户节点。
 *
 * <p>{@code deptId} 与 {@code roleIds} 分别生成归属部门与持有角色两类关系。
 *
 * @param id       用户主键
 * @param username 用户名
 * @param nickname 昵称
 * @param deptId   所属部门主键，可为 {@code null}
 * @param status   状态：0-停用，1-正常
 * @param roleIds  角色主键列表
 * @author iBoot Team
 * @since 1.0.0
 */
public record UserNode(Long id, String username, String nickname, Long deptId, Integer status,
                       List<Long> roleIds) {

    /**
     * 规范化角色列表，避免调用方处理 {@code null}。
     */
    public UserNode {
        roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }
}
