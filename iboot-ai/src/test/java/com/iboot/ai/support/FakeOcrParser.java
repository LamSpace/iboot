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

package com.iboot.ai.support;

import com.iboot.ai.domain.knowledge.OcrParser;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用 OCR 解析器。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class FakeOcrParser implements OcrParser {

    private final AtomicInteger calls = new AtomicInteger();

    private volatile String text = "识别文本";

    @Override
    public String recognize(byte[] imageBytes, String contentType) {
        calls.incrementAndGet();
        return text;
    }

    /**
     * 调用次数。
     *
     * @return 调用次数
     */
    public int calls() {
        return calls.get();
    }

    /**
     * 设置返回文本。
     *
     * @param text 返回文本
     */
    public void setText(String text) {
        this.text = text;
    }
}
