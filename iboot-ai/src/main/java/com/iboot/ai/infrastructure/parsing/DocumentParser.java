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

/**
 * 文档解析器 SPI。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface DocumentParser {

    /**
     * 是否支持指定扩展名。
     *
     * @param extension 小写扩展名
     * @return 支持时返回 {@code true}
     */
    boolean supports(String extension);

    /**
     * 解析文档。
     *
     * @param content 文件内容
     * @return 解析结果
     * @throws DocumentParseException 解析失败时抛出
     */
    ParsedDocument parse(byte[] content);
}
