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

/**
 * 「用户 → 角色 → 菜单」路径上的一条权限记录。
 *
 * @param roleCode   角色编码
 * @param roleName   角色名称
 * @param menuName   菜单名称
 * @param permission 权限标识
 * @author iBoot Team
 * @since 1.0.0
 */
public record PermissionRef(String roleCode, String roleName, String menuName, String permission) {
}
