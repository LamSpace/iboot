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

package com.iboot.auth.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人访问令牌的列表视图。
 *
 * <p>状态由元数据与共享存储中的撤销记录共同决定：{@code ACTIVE}、{@code REVOKED}、
 * {@code EXPIRED}。
 *
 * @param id 记录 ID
 * @param name 令牌名称
 * @param scopes 权限码范围；空列表表示继承用户全部权限
 * @param status 当前状态
 * @param createdAt 创建时间
 * @param expiresAt 过期时间
 * @param lastUsedAt 最近使用时间；从未使用或记录已丢失时为 {@code null}
 * @author iBoot Team
 * @since 1.0.0
 */
public record PersonalAccessTokenSummary(Long id, String name, List<String> scopes, String status,
                                        LocalDateTime createdAt, LocalDateTime expiresAt,
                                        LocalDateTime lastUsedAt) {
}
