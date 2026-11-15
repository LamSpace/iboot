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

package com.iboot.system.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 参数分组统计响应。
 *
 * <p>返回每个分组下的参数配置数量，供门户左侧分组导航展示。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "参数分组统计")
public class ConfigGroupStatResponse {

    /**
     * 分组编码。
     */
    @Schema(description = "分组编码")
    private String configGroup;

    /**
     * 该分组下的配置数量。
     */
    @Schema(description = "配置数量")
    private Long count;

    @SuppressWarnings("all")
    public ConfigGroupStatResponse() {
    }

    @SuppressWarnings("all")
    public ConfigGroupStatResponse(final String configGroup, final Long count) {
        this.configGroup = configGroup;
        this.count = count;
    }

    @SuppressWarnings("all")
    public String getConfigGroup() {
        return this.configGroup;
    }

    @SuppressWarnings("all")
    public void setConfigGroup(final String configGroup) {
        this.configGroup = configGroup;
    }

    @SuppressWarnings("all")
    public Long getCount() {
        return this.count;
    }

    @SuppressWarnings("all")
    public void setCount(final Long count) {
        this.count = count;
    }
}
