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

package com.iboot.auth.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新签发个人访问令牌的响应。
 *
 * <p>令牌明文只在此响应中出现一次，调用方必须立即保存。
 *
 * @param id 记录 ID
 * @param name 令牌名称
 * @param scopes 权限码范围；空列表表示继承用户全部权限
 * @param token 令牌明文
 * @param createdAt 创建时间
 * @param expiresAt 过期时间
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "新签发的个人访问令牌")
public record PersonalAccessTokenCreatedResponse(
        @Schema(description = "记录 ID") Long id,
        @Schema(description = "令牌名称") String name,
        @Schema(description = "权限码范围；为空表示继承全部权限") List<String> scopes,
        @Schema(description = "令牌明文，只返回一次") String token,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "过期时间") LocalDateTime expiresAt) {
}
