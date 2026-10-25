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

/**
 * 解析后的文本块。
 *
 * @param pageNo      页码；DOCX/TXT 等无稳定页码时为 {@code null}
 * @param headingPath 标题路径；无标题时为 {@code null}
 * @param text        文本内容；扫描页可能为空串
 * @author iBoot Team
 * @since 1.0.0
 */
public record ParsedBlock(Integer pageNo, String headingPath, String text) {
}
