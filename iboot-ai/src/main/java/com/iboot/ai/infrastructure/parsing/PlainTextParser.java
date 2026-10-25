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

package com.iboot.ai.infrastructure.parsing;

import com.iboot.ai.domain.knowledge.ParsedBlock;
import com.iboot.ai.domain.knowledge.ParsedDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 纯文本与 Markdown 解析器。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PlainTextParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "txt".equalsIgnoreCase(extension) || "md".equalsIgnoreCase(extension)
                || "markdown".equalsIgnoreCase(extension);
    }

    @Override
    public ParsedDocument parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("EMPTY_FILE", "文件内容为空");
        }
        String text = new String(content, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            throw new DocumentParseException("NO_TEXT", "文件中没有可提取文本");
        }
        return new ParsedDocument(List.of(new ParsedBlock(null, null, text)));
    }
}
