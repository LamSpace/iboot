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
 * 图中的一个部门节点。
 *
 * <p>由 admin 的部门列表映射而来，{@code parentId} 决定组织层级关系。
 *
 * @param id       部门主键
 * @param name     部门名称
 * @param code     部门编码
 * @param parentId 上级部门主键，顶级部门为 {@code null}
 * @param status   状态：0-停用，1-正常
 * @author iBoot Team
 * @since 1.0.0
 */
public record DeptNode(Long id, String name, String code, Long parentId, Integer status) {
}
