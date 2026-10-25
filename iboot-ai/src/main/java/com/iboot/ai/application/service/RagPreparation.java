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

package com.iboot.ai.application.service;

import java.util.List;

/**
 * 已准备好上下文的 RAG 问答。
 *
 * @param userId        用户 ID
 * @param conversationId 会话 ID
 * @param question      用户问题
 * @param systemPrompt  带检索上下文的系统提示词
 * @param citations     引用列表
 * @param degraded      是否降级
 * @param noHit         是否未命中
 * @author iBoot Team
 * @since 1.0.0
 */
public record RagPreparation(Long userId, String conversationId, String question, String systemPrompt,
                             List<RagCitation> citations, boolean degraded, boolean noHit) {

    /**
     * 规范化引用列表。
     */
    public RagPreparation {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
