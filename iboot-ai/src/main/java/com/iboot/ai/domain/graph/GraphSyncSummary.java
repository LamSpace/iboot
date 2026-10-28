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
 * 一次同步的结果摘要。
 *
 * @param syncId      同步标识
 * @param syncedAt    同步完成时间
 * @param departments 部门节点数
 * @param users       用户节点数
 * @param roles       角色节点数
 * @param menus       菜单节点数
 * @param relations   关系总数
 * @param removed     本次清理的过期节点数
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphSyncSummary(String syncId, Instant syncedAt, int departments, int users, int roles, int menus,
                               int relations, int removed) {
}
