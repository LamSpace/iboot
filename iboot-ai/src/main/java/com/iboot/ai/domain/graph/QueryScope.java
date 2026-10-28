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
 * 图查询使用的数据权限范围。
 *
 * <p>超管不受限制；其余用户只能看到 {@code visibleDeptIds} 内的部门与用户。
 *
 * @param unrestricted   是否不受范围限制
 * @param visibleDeptIds 可见部门主键列表
 * @author iBoot Team
 * @since 1.0.0
 */
public record QueryScope(boolean unrestricted, List<Long> visibleDeptIds) {

    /**
     * 规范化可见部门，避免调用方处理 {@code null}。
     */
    public QueryScope {
        visibleDeptIds = visibleDeptIds == null ? List.of() : List.copyOf(visibleDeptIds);
    }
}
