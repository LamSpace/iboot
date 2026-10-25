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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库。
 *
 * @param id           知识库 ID
 * @param name         名称
 * @param description  描述
 * @param visibility   可见范围码
 * @param ownerUserId  所有者用户 ID
 * @param ownerDeptId  所有者部门 ID
 * @param sharedDeptIds 已解析的共享部门集合
 * @param createdBy    创建人
 * @param createTime   创建时间
 * @param updateTime   更新时间
 * @author iBoot Team
 * @since 1.0.0
 */
public record KnowledgeBase(Long id, String name, String description, Integer visibility, Long ownerUserId,
                            Long ownerDeptId, List<Long> sharedDeptIds, String createdBy, LocalDateTime createTime,
                            LocalDateTime updateTime) {

    /**
     * 规范化共享部门集合。
     */
    public KnowledgeBase {
        sharedDeptIds = sharedDeptIds == null ? List.of() : List.copyOf(sharedDeptIds);
    }
}
