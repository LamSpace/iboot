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

/**
 * 非流式对话响应。
 *
 * @param conversationId 会话 ID
 * @param content        回答内容
 * @param model          模型标识
 * @param usage          token 用量
 * @author iBoot Team
 * @since 1.0.0
 */
public record ChatResponseDto(String conversationId, String content, String model, UsageDto usage) {
}
