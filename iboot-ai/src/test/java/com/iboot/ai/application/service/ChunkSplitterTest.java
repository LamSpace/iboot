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

import com.iboot.ai.domain.knowledge.ChunkDraft;
import com.iboot.ai.domain.knowledge.ParsedBlock;
import com.iboot.ai.domain.knowledge.ParsedDocument;
import com.iboot.ai.infrastructure.config.RagProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ChunkSplitter} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ChunkSplitterTest {

    /**
     * 超长文本按长度切分并保留重叠。
     */
    @Test
    void splitsLongTextWithOverlap() {
        ChunkSplitter splitter = splitter(300, 50);
        String text = "甲".repeat(1000);

        List<ChunkDraft> chunks = splitter.split(new ParsedDocument(List.of(new ParsedBlock(1, "标题", text))));

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(300));
        assertThat(chunks.get(0).content().substring(250))
                .isEqualTo(chunks.get(1).content().substring(0, 50));
    }

    /**
     * 不同页码的内容不会被拼进同一个分块。
     */
    @Test
    void doesNotMixPages() {
        ChunkSplitter splitter = splitter(200, 20);
        ParsedDocument document = new ParsedDocument(List.of(
                new ParsedBlock(1, null, "第一页".repeat(100)),
                new ParsedBlock(2, null, "第二页".repeat(100))));

        List<ChunkDraft> chunks = splitter.split(document);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk -> {
            if (chunk.pageNo() == 1) {
                assertThat(chunk.content()).doesNotContain("第二页");
            } else {
                assertThat(chunk.pageNo()).isEqualTo(2);
                assertThat(chunk.content()).doesNotContain("第一页");
            }
        });
    }

    /**
     * 同页短块会合并。
     */
    @Test
    void mergesShortBlocksOnSamePage() {
        ChunkSplitter splitter = splitter(100, 10);
        ParsedDocument document = new ParsedDocument(List.of(
                new ParsedBlock(1, "标题", "短句一"),
                new ParsedBlock(1, "标题", "短句二")));

        List<ChunkDraft> chunks = splitter.split(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).contains("短句一").contains("短句二");
    }

    /**
     * 空块被跳过。
     */
    @Test
    void skipsEmptyBlocks() {
        ChunkSplitter splitter = splitter(100, 10);
        ParsedDocument document = new ParsedDocument(List.of(
                new ParsedBlock(1, null, "  "),
                new ParsedBlock(2, null, "有效内容")));

        List<ChunkDraft> chunks = splitter.split(document);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().pageNo()).isEqualTo(2);
    }

    /**
     * 优先在句末切分。
     */
    @Test
    void prefersSentenceBoundary() {
        ChunkSplitter splitter = splitter(300, 20);
        String text = "甲".repeat(280) + "。" + "乙".repeat(200);

        List<ChunkDraft> chunks = splitter.split(new ParsedDocument(List.of(new ParsedBlock(1, null, text))));

        assertThat(chunks.getFirst().content()).endsWith("。");
    }

    /**
     * 分块序号连续。
     */
    @Test
    void keepsSequentialIndices() {
        ChunkSplitter splitter = splitter(100, 10);
        List<ChunkDraft> chunks = splitter.split(new ParsedDocument(List.of(
                new ParsedBlock(1, null, "内容".repeat(200)))));

        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    /**
     * 重叠长度不能大于等于分块长度。
     */
    @Test
    void rejectsOverlapNotLessThanMax() {
        ChunkSplitter splitter = splitter(100, 100);
        assertThatThrownBy(() -> splitter.split(new ParsedDocument(List.of(
                new ParsedBlock(1, null, "内容".repeat(200))))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ChunkSplitter splitter(int maxChars, int overlapChars) {
        RagProperties properties = new RagProperties();
        properties.setChunkMaxChars(maxChars);
        properties.setChunkOverlapChars(overlapChars);
        return new ChunkSplitter(properties);
    }
}
