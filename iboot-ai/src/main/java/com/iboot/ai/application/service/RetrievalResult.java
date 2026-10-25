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

package com.iboot.ai.application.service;

import java.util.List;

/**
 * 检索结果。
 *
 * @param chunks   命中的分块
 * @param degraded 是否降级
 * @param mode     检索模式：hybrid 或 bm25
 * @author iBoot Team
 * @since 1.0.0
 */
public record RetrievalResult(List<RetrievedChunk> chunks, boolean degraded, String mode) {

    /**
     * 规范化分块列表。
     */
    public RetrievalResult {
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }

    /**
     * 空结果。
     *
     * @return 空检索结果
     */
    public static RetrievalResult empty() {
        return new RetrievalResult(List.of(), false, "hybrid");
    }
}
