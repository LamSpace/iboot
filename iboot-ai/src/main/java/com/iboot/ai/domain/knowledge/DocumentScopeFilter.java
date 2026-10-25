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

package com.iboot.ai.domain.knowledge;

import java.util.List;

/**
 * 文档级数据权限过滤条件。
 *
 * <p>与具体检索后端无关，由 ES 适配器翻译为查询条件；{@link Mode#DENY} 表示不返回任何文档。
 *
 * @param mode   过滤模式
 * @param userId 仅本人模式使用的用户 ID
 * @param deptId 本部门模式使用的部门 ID
 * @param deptIds 多部门模式使用的部门集合
 * @author iBoot Team
 * @since 1.0.0
 */
public record DocumentScopeFilter(Mode mode, Long userId, Long deptId, List<Long> deptIds) {

    /**
     * 过滤模式。
     */
    public enum Mode {
        /**
         * 不过滤。
         */
        ALL,
        /**
         * 仅本人。
         */
        SELF,
        /**
         * 本部门。
         */
        DEPT,
        /**
         * 多个部门。
         */
        DEPTS,
        /**
         * 拒绝全部。
         */
        DENY
    }

    /**
     * 规范化部门集合。
     */
    public DocumentScopeFilter {
        deptIds = deptIds == null ? List.of() : List.copyOf(deptIds);
    }

    /**
     * 不过滤。
     *
     * @return 全部可见过滤条件
     */
    public static DocumentScopeFilter all() {
        return new DocumentScopeFilter(Mode.ALL, null, null, List.of());
    }

    /**
     * 拒绝全部。
     *
     * @return 拒绝过滤条件
     */
    public static DocumentScopeFilter deny() {
        return new DocumentScopeFilter(Mode.DENY, null, null, List.of());
    }
}
