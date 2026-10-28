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
 * 两个部门之间的组织路径。
 *
 * @param departmentNames 从起点到终点依次经过的部门名称
 * @param hops            路径长度
 * @author iBoot Team
 * @since 1.0.0
 */
public record DeptPath(List<String> departmentNames, int hops) {

    /**
     * 规范化部门名称，避免调用方处理 {@code null}。
     */
    public DeptPath {
        departmentNames = departmentNames == null ? List.of() : List.copyOf(departmentNames);
    }
}
