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

package com.iboot.ai.application.service.graph;

import java.util.List;

/**
 * 图查询结果页。
 *
 * <p>查询时多取一行用于判断是否还有更多数据；返回前裁到上限并置 {@code truncated}。
 *
 * @param rows      结果行
 * @param truncated 是否因达到上限而被截断
 * @param <T>       行类型
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphRows<T>(List<T> rows, boolean truncated) {

    /**
     * 规范化结果行，避免调用方处理 {@code null}。
     */
    public GraphRows {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /**
     * 按上限裁剪查询结果。
     *
     * @param fetched 查询返回的行，最多为上限加一
     * @param limit   允许返回的最大行数
     * @param <T>     行类型
     * @return 结果页
     */
    public static <T> GraphRows<T> of(List<T> fetched, int limit) {
        List<T> safe = fetched == null ? List.of() : fetched;
        boolean truncated = safe.size() > limit;
        return new GraphRows<>(truncated ? safe.subList(0, limit) : safe, truncated);
    }
}
