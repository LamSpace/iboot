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

package com.iboot.ai.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库问答请求。
 *
 * @param conversationId 会话 ID；为空时新建
 * @param content        用户问题
 * @param knowledgeBaseId 选择的知识库；为空时检索全部可见知识库
 * @author iBoot Team
 * @since 1.0.0
 */
public record RagChatRequest(String conversationId,
                             @NotBlank(message = "消息内容不能为空") String content,
                             Long knowledgeBaseId) {
}
