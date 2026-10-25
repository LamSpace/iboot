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
 * 知识库共享判断所需的摘要。
 *
 * <p>只包含可见范围计算需要的字段，不承载正文或文档信息。
 *
 * @param id            知识库 ID
 * @param visibility    可见范围码，1-全部、2-自定义、3-本部门、4-本部门及以下、5-仅本人
 * @param ownerUserId   所有者用户 ID
 * @param ownerDeptId   所有者部门 ID
 * @param sharedDeptIds 已解析的共享部门集合；可见范围 2/3/4 使用，1/5 为空
 * @author iBoot Team
 * @since 1.0.0
 */
public record KnowledgeBaseSummary(Long id, Integer visibility, Long ownerUserId, Long ownerDeptId,
                                   List<Long> sharedDeptIds) {

    /**
     * 规范化共享部门集合。
     */
    public KnowledgeBaseSummary {
        sharedDeptIds = sharedDeptIds == null ? List.of() : List.copyOf(sharedDeptIds);
    }
}
