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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RagPromptBuilder} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder();

    /**
     * 上下文按编号排列，并包含资料约束。
     */
    @Test
    void numbersContextAndAddsInstructions() {
        String prompt = builder.build(List.of(
                new RetrievedChunk("1:0", 1L, 10L, "年假政策", null, "年假五天", 3, 0, 1.0),
                new RetrievedChunk("2:0", 1L, 11L, "报销流程", null, "差旅报销", null, 1, 0.5)));

        assertThat(prompt).contains("[1] 《年假政策》 第 3 页")
                .contains("年假五天")
                .contains("[2] 《报销流程》")
                .contains("差旅报销")
                .contains("只依据")
                .contains("引用格式")
                .contains("<context>")
                .contains("</context>");
    }
}
