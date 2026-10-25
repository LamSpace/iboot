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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DocumentParserRegistry} 的单元测试。
 *
 * <p>覆盖 PDF 页码、DOCX 标题路径、纯文本、损坏/加密文件与不支持类型。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DocumentParserRegistryTest {

    private final DocumentParserRegistry registry = new DocumentParserRegistry(List.of(
            new TextPdfParser(), new DocxParser(), new PlainTextParser()));

    /**
     * PDF 逐页解析并保留页码。
     *
     * @throws Exception 生成 PDF 失败时抛出
     */
    @Test
    void parsesPdfPages() throws Exception {
        ParsedDocument document = registry.parse("a.pdf", pdf("Page one", "Page two"));

        assertThat(document.blocks()).hasSize(2);
        assertThat(document.blocks().get(0).pageNo()).isEqualTo(1);
        assertThat(document.blocks().get(0).text()).contains("Page one");
        assertThat(document.blocks().get(1).pageNo()).isEqualTo(2);
        assertThat(document.blocks().get(1).text()).contains("Page two");
    }

    /**
     * DOCX 保留标题路径与正文。
     *
     * @throws Exception 生成 DOCX 失败时抛出
     */
    @Test
    void parsesDocxHeadings() throws Exception {
        ParsedDocument document = registry.parse("a.docx", docx());

        assertThat(document.hasText()).isTrue();
        assertThat(document.blocks()).anySatisfy(block -> {
            assertThat(block.headingPath()).isEqualTo("第一章");
            assertThat(block.text()).contains("内容一");
        });
        assertThat(document.blocks()).anySatisfy(block -> {
            assertThat(block.headingPath()).isEqualTo("第二章");
            assertThat(block.text()).contains("内容二");
        });
    }

    /**
     * 纯文本可直接解析。
     */
    @Test
    void parsesPlainText() {
        ParsedDocument document = registry.parse("a.txt", "你好，知识库".getBytes(StandardCharsets.UTF_8));

        assertThat(document.hasText()).isTrue();
        assertThat(document.blocks().getFirst().text()).contains("你好");
    }

    /**
     * 损坏的 PDF 返回可辨识错误。
     */
    @Test
    void rejectsCorruptPdf() {
        assertThatThrownBy(() -> registry.parse("a.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DocumentParseException.class)
                .extracting(error -> ((DocumentParseException) error).code())
                .isEqualTo("PARSE_FAILED");
    }

    /**
     * 加密 PDF 返回可辨识错误。
     *
     * @throws Exception 生成加密 PDF 失败时抛出
     */
    @Test
    void rejectsEncryptedPdf() throws Exception {
        assertThatThrownBy(() -> registry.parse("a.pdf", encryptedPdf()))
                .isInstanceOf(DocumentParseException.class)
                .extracting(error -> ((DocumentParseException) error).code())
                .isEqualTo("ENCRYPTED");
    }

    /**
     * 不支持的扩展名返回可辨识错误。
     */
    @Test
    void rejectsUnsupportedType() {
        assertThatThrownBy(() -> registry.parse("a.exe", new byte[]{1, 2, 3}))
                .isInstanceOf(DocumentParseException.class)
                .extracting(error -> ((DocumentParseException) error).code())
                .isEqualTo("UNSUPPORTED_TYPE");
    }

    private byte[] pdf(String... pages) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (String text : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] encryptedPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            AccessPermission permission = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "user", permission);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] docx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            paragraph(document, "Heading1", "第一章");
            paragraph(document, null, "内容一");
            paragraph(document, "Heading1", "第二章");
            paragraph(document, null, "内容二");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }

    private void paragraph(XWPFDocument document, String style, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        if (style != null) {
            paragraph.setStyle(style);
        }
        paragraph.createRun().setText(text);
    }
}
