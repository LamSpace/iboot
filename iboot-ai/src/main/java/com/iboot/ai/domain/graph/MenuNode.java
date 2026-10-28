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
 * 图中的一个菜单节点。
 *
 * <p>{@code permission} 是 RBAC 权限标识，用于回答「谁能访问什么」。
 *
 * @param id         菜单主键
 * @param name       菜单名称
 * @param type       菜单类型：目录 / 菜单 / 按钮
 * @param permission 权限标识，可为 {@code null}
 * @param parentId   上级菜单主键，可为 {@code null}
 * @param status     状态：0-停用，1-正常
 * @author iBoot Team
 * @since 1.0.0
 */
public record MenuNode(Long id, String name, String type, String permission, Long parentId, Integer status) {
}
