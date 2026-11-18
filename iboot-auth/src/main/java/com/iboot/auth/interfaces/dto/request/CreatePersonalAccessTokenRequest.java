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

package com.iboot.auth.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建个人访问令牌请求。
 *
 * <p>有效期上限由服务端配置决定，因此在服务层校验；这里只约束名称与最小天数。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "创建个人访问令牌请求")
public class CreatePersonalAccessTokenRequest {

    /**
     * 令牌名称，用于在列表中识别用途。
     */
    @Schema(description = "令牌名称，便于识别用途")
    @NotBlank(message = "令牌名称不能为空")
    @Size(max = 64, message = "令牌名称长度不能超过 64 个字符")
    private String name;

    /**
     * 有效期天数；为空时使用服务端默认值。
     */
    @Schema(description = "有效期天数；为空时使用默认值")
    @Min(value = 1, message = "有效期至少 1 天")
    private Long expiresDays;

    /**
     * 创建空请求，供框架反序列化使用。
     */
    public CreatePersonalAccessTokenRequest() {
    }

    /**
     * 返回令牌名称。
     *
     * @return 令牌名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置令牌名称。
     *
     * @param name 令牌名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 返回有效期天数。
     *
     * @return 有效期天数；未指定时为 {@code null}
     */
    public Long getExpiresDays() {
        return expiresDays;
    }

    /**
     * 设置有效期天数。
     *
     * @param expiresDays 有效期天数
     */
    public void setExpiresDays(Long expiresDays) {
        this.expiresDays = expiresDays;
    }
}
