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

package com.iboot.ai.infrastructure.config;

import com.iboot.ai.infrastructure.persistence.RedisChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对话客户端与会话记忆配置。
 *
 * <p>使用窗口记忆，窗口大小由 {@code iboot.ai.chat.max-messages} 控制；全局 {@link ChatClient}
 * 默认挂载记忆 advisor，调用方只需传入会话 ID。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class ChatClientConfig {

    /**
     * 创建会话记忆。
     *
     * @param repository   自定义 Redis 仓储
     * @param aiProperties AI 配置
     * @return 窗口记忆
     */
    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repository, AiProperties aiProperties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(aiProperties.getChat().getMaxMessages())
                .build();
    }

    /**
     * 创建带记忆 advisor 的 ChatClient。
     *
     * @param builder    Spring AI 自动配置的构建器
     * @param chatMemory 会话记忆
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
    }
}
