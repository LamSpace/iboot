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

import com.iboot.ai.domain.knowledge.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析器注册表。
 *
 * <p>按扩展名选择解析器；不支持的扩展名返回 {@code UNSUPPORTED_TYPE}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;

    /**
     * 创建注册表。
     *
     * @param parsers 所有可用解析器
     */
    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    /**
     * 判断是否支持指定文件。
     *
     * @param fileName 原始文件名
     * @return 支持时返回 {@code true}
     */
    public boolean supports(String fileName) {
        String extension = extensionOf(fileName);
        return parsers.stream().anyMatch(candidate -> candidate.supports(extension));
    }

    /**
     * 解析文档。
     *
     * @param fileName 原始文件名
     * @param content  文件内容
     * @return 解析结果
     * @throws DocumentParseException 解析失败或类型不支持时抛出
     */
    public ParsedDocument parse(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("EMPTY_FILE", "文件内容为空");
        }
        String extension = extensionOf(fileName);
        DocumentParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(extension))
                .findFirst()
                .orElseThrow(() -> new DocumentParseException("UNSUPPORTED_TYPE", "不支持的文件类型：" + extension));
        try {
            return parser.parse(content);
        } catch (DocumentParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DocumentParseException("PARSE_FAILED", "文档解析失败", e);
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
