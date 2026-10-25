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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实百炼模型的延迟验证。
 *
 * <p>标记为 {@code cloud} 分组，默认被 surefire 排除；运行前需注入 DASHSCOPE_* 环境变量与可用 Redis。
 * 首 token 口径与 Spike 报告一致：请求发出 → 第一个非空 content 增量。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Tag("cloud")
@SpringBootTest
class CloudLatencyTest {

    private static final String PROMPT = "请用一句话介绍杭州。";

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Test
    void p95FirstTokenUnderThreeSeconds() {
        List<Long> samples = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            String conversationId = UUID.randomUUID().toString();
            long start = System.nanoTime();
            AtomicLong firstContent = new AtomicLong(-1);

            chatClient.prompt()
                    .user(PROMPT)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        if (!ChatApplicationService.textOf(response).isEmpty()) {
                            firstContent.compareAndSet(-1, System.nanoTime());
                        }
                    })
                    .blockLast();

            if (firstContent.get() > 0) {
                samples.add((firstContent.get() - start) / 1_000_000);
            }
            chatMemory.clear(conversationId);
        }

        assertThat(samples).hasSizeGreaterThan(1);
        List<Long> measured = samples.subList(1, samples.size()).stream().sorted().toList();
        long p95 = measured.get((int) Math.ceil(0.95 * measured.size()) - 1);

        System.out.printf("first-token ms: samples=%s p50=%d p95=%d%n",
                samples, measured.get(measured.size() / 2), p95);
        assertThat(p95).isLessThan(3000L);
    }
}
