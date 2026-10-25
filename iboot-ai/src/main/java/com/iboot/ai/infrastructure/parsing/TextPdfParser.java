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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本类 PDF 解析器。
 *
 * <p>使用 PDFBox 逐页提取文本；加密 PDF 返回 {@code ENCRYPTED}，损坏文件返回
 * {@code PARSE_FAILED}。扫描页会返回空文本块，由 OCR 阶段决定是否补全。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class TextPdfParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    @Override
    public ParsedDocument parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException("EMPTY_FILE", "文件内容为空");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new DocumentParseException("ENCRYPTED", "PDF 已加密，无法解析");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<ParsedBlock> blocks = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                blocks.add(new ParsedBlock(page, null, stripper.getText(document).trim()));
            }
            return new ParsedDocument(blocks);
        } catch (InvalidPasswordException e) {
            throw new DocumentParseException("ENCRYPTED", "PDF 已加密，无法解析", e);
        } catch (DocumentParseException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new DocumentParseException("PARSE_FAILED", "PDF 解析失败", e);
        }
    }
}
