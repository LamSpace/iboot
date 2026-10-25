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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文档分块器。
 *
 * <p>按页/标题边界聚合文本，再按配置的最大长度与重叠长度切分；不同页码的内容不会被拼进
 * 同一个分块。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class ChunkSplitter {

    private final RagProperties properties;

    /**
     * 创建分块器。
     *
     * @param properties 知识库配置
     */
    public ChunkSplitter(RagProperties properties) {
        this.properties = properties;
    }

    /**
     * 切分解析后的文档。
     *
     * @param document 解析结果
     * @return 分块草稿
     */
    public List<ChunkDraft> split(ParsedDocument document) {
        int maxChars = properties.getChunkMaxChars();
        int overlapChars = properties.getChunkOverlapChars();
        if (maxChars <= 0 || overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("chunk-overlap-chars 必须小于 chunk-max-chars");
        }
        if (document == null || document.blocks().isEmpty()) {
            return List.of();
        }
        List<ChunkDraft> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Integer pageNo = null;
        String headingPath = null;
        int index = 0;
        for (ParsedBlock block : document.blocks()) {
            String text = normalize(block.text());
            if (text.isEmpty()) {
                continue;
            }
            boolean boundary = buffer.length() > 0
                    && (!Objects.equals(pageNo, block.pageNo()) || !Objects.equals(headingPath, block.headingPath()));
            if (boundary) {
                index = flush(chunks, buffer, pageNo, headingPath, index);
            }
            if (buffer.length() == 0) {
                pageNo = block.pageNo();
                headingPath = block.headingPath();
            } else {
                buffer.append('\n');
            }
            buffer.append(text);
            while (buffer.length() > maxChars) {
                int cut = findCut(buffer, maxChars);
                String content = buffer.substring(0, cut).trim();
                if (!content.isEmpty()) {
                    chunks.add(new ChunkDraft(index++, pageNo, headingPath, content, content.length()));
                }
                int nextStart = cut - overlapChars;
                if (nextStart <= 0) {
                    nextStart = cut;
                }
                buffer.delete(0, nextStart);
            }
        }
        flush(chunks, buffer, pageNo, headingPath, index);
        return chunks;
    }

    private int flush(List<ChunkDraft> chunks, StringBuilder buffer, Integer pageNo, String headingPath, int index) {
        String content = buffer.toString().trim();
        buffer.setLength(0);
        if (!content.isEmpty()) {
            chunks.add(new ChunkDraft(index++, pageNo, headingPath, content, content.length()));
        }
        return index;
    }

    private int findCut(StringBuilder buffer, int maxChars) {
        int lowerBound = Math.max(1, (int) (maxChars * 0.6));
        for (int i = maxChars; i >= lowerBound; i--) {
            char c = buffer.charAt(i - 1);
            if (c == '。' || c == '！' || c == '？' || c == '!' || c == '?' || c == ';' || c == '；'
                    || c == '.' || c == '\n') {
                return i;
            }
        }
        return maxChars;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
