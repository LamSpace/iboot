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

import java.time.Instant;

/**
 * 知识图谱的当前状态。
 *
 * <p>{@code synced=false} 表示尚未成功同步过，此时计数与时间均为空值。
 *
 * @param synced      是否已同步
 * @param syncedAt    上次成功同步时间
 * @param departments 部门节点数
 * @param users       用户节点数
 * @param roles       角色节点数
 * @param menus       菜单节点数
 * @param relations   关系总数
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphStatus(boolean synced, Instant syncedAt, int departments, int users, int roles, int menus,
                          int relations) {

    /**
     * 未同步时的空状态。
     *
     * @return 空状态
     */
    public static GraphStatus empty() {
        return new GraphStatus(false, null, 0, 0, 0, 0, 0);
    }
}
