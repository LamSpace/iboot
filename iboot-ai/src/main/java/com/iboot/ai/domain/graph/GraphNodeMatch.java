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
 * 名称解析命中的图节点。
 *
 * <p>只承载解析所需的标识与展示字段：{@code id} 直接用于后续查询模板，
 * {@code parentName} 用于区分同名部门，{@code permission} 用于菜单解析后继续查询权限持有人。
 * 字段按节点类型填充，不适用的字段为 {@code null}。
 *
 * @param type       节点类型，取值为 {@code dept}、{@code user}、{@code menu}
 * @param id         节点主键
 * @param name       展示名称：部门名称、用户昵称（缺失时用用户名）、菜单名称
 * @param label      辅助标识：部门编码、用户名、菜单类型
 * @param parentName 上级部门名称，仅部门结果有值
 * @param permission 权限码，仅菜单结果有值
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphNodeMatch(String type, Long id, String name, String label, String parentName,
                             String permission) {
}
