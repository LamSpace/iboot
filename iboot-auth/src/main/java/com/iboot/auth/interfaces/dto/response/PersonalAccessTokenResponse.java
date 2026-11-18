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

/**
 * 个人访问令牌列表项。
 *
 * <p>不包含令牌明文；状态取值 {@code ACTIVE}、{@code REVOKED}、{@code EXPIRED}。
 *
 * @param id 记录 ID
 * @param name 令牌名称
 * @param status 当前状态
 * @param createdAt 创建时间
 * @param expiresAt 过期时间
 * @param lastUsedAt 最近使用时间；从未使用时为 {@code null}
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "个人访问令牌列表项")
public record PersonalAccessTokenResponse(
        @Schema(description = "记录 ID") Long id,
        @Schema(description = "令牌名称") String name,
        @Schema(description = "状态：ACTIVE、REVOKED、EXPIRED") String status,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "过期时间") LocalDateTime expiresAt,
        @Schema(description = "最近使用时间") LocalDateTime lastUsedAt) {
}
