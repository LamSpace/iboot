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

package com.iboot.ai.infrastructure.ocr;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * PDF 页面渲染器。
 *
 * <p>把指定页面渲染为 JPEG，供 OCR 使用；超过最大边长时等比缩放。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PdfPageRenderer {

    private static final int MAX_DIMENSION = 2048;

    /**
     * 渲染指定页面。
     *
     * @param pdf     PDF 内容
     * @param pageNo  页码，从 1 开始
     * @param dpi     渲染 DPI
     * @return JPEG 字节
     */
    public byte[] render(byte[] pdf, int pageNo, int dpi) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                throw new OcrException("RENDER_FAILED", "PDF 页码越界：" + pageNo);
            }
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(pageNo - 1, dpi, ImageType.RGB);
            image = downscale(image);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "jpg", out)) {
                throw new OcrException("RENDER_FAILED", "JPEG 编码失败");
            }
            return out.toByteArray();
        } catch (OcrException e) {
            throw e;
        } catch (Exception e) {
            throw new OcrException("RENDER_FAILED", "PDF 页面渲染失败", e);
        }
    }

    private BufferedImage downscale(BufferedImage image) {
        int max = Math.max(image.getWidth(), image.getHeight());
        if (max <= MAX_DIMENSION) {
            return image;
        }
        double scale = (double) MAX_DIMENSION / max;
        int width = Math.max(1, (int) (image.getWidth() * scale));
        int height = Math.max(1, (int) (image.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }
}
