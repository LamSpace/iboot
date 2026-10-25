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
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCX 解析器。
 *
 * <p>使用 POI 提取段落、标题与表格文本；DOCX 没有稳定页码，引用使用标题路径与段落内容。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DocxParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }

    @Override
    public ParsedDocument parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("EMPTY_FILE", "文件内容为空");
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<ParsedBlock> blocks = new ArrayList<>();
            StringBuilder headingPath = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText() == null ? "" : paragraph.getText().trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    if (isHeading(paragraph.getStyle())) {
                        headingPath = new StringBuilder(text);
                    }
                    blocks.add(new ParsedBlock(null, headingPath.isEmpty() ? null : headingPath.toString(), text));
                } else if (element instanceof XWPFTable table) {
                    String text = tableText(table);
                    if (!text.isEmpty()) {
                        blocks.add(new ParsedBlock(null, headingPath.isEmpty() ? null : headingPath.toString(), text));
                    }
                }
            }
            ParsedDocument parsed = new ParsedDocument(blocks);
            if (!parsed.hasText()) {
                throw new DocumentParseException("NO_TEXT", "DOCX 中没有可提取文本");
            }
            return parsed;
        } catch (DocumentParseException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentParseException("PARSE_FAILED", "DOCX 解析失败", e);
        }
    }

    private boolean isHeading(String style) {
        return style != null && (style.startsWith("Heading") || style.startsWith("标题"));
    }

    private String tableText(XWPFTable table) {
        StringBuilder builder = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();
                if (text != null && !text.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append('\t');
                    }
                    builder.append(text.trim());
                }
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }
}
