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
 * 解析后的文档。
 *
 * @param blocks 文本块列表
 * @author iBoot Team
 * @since 1.0.0
 */
public record ParsedDocument(List<ParsedBlock> blocks) {

    /**
     * 规范化文本块列表。
     */
    public ParsedDocument {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    /**
     * 判断是否存在可提取文本。
     *
     * @return 任一块包含非空白文本时返回 {@code true}
     */
    public boolean hasText() {
        return blocks.stream().anyMatch(block -> block.text() != null && !block.text().isBlank());
    }
}
