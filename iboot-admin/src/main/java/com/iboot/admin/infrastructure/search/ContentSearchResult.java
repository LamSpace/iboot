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

package com.iboot.admin.infrastructure.search;

import java.util.List;

/**
 * 业务内容检索结果。
 *
 * <p>同时携带当前页命中与命中的总记录数，供应用服务组装分页响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ContentSearchResult {

    /**
     * 当前页命中项。
     */
    private final List<ContentSearchHit> hits;

    /**
     * 命中的总记录数。
     */
    private final long total;

    /**
     * 创建检索结果。
     *
     * @param hits  当前页命中项
     * @param total 命中的总记录数
     */
    public ContentSearchResult(final List<ContentSearchHit> hits, final long total) {
        this.hits = hits;
        this.total = total;
    }

    /**
     * 获取当前页命中项。
     *
     * @return 当前页命中项
     */
    public List<ContentSearchHit> getHits() {
        return hits;
    }

    /**
     * 获取命中的总记录数。
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }

}
