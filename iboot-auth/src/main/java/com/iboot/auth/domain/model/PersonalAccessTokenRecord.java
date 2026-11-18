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
 * 个人访问令牌的元数据记录。
 *
 * <p>只包含令牌标识与生命周期信息，不包含令牌原文。撤销状态与共享存储中的
 * {@code pat_tokens:<jti>} 共同表达令牌是否仍可使用。
 *
 * @param id 记录 ID
 * @param userId 所属用户 ID
 * @param name 令牌名称
 * @param tokenId 令牌唯一标识（JWT jti）
 * @param scopes 权限码范围；空列表表示继承用户全部权限
 * @param expiresAt 过期时间
 * @param revoked 是否已撤销
 * @param revokedAt 撤销时间，未撤销时为 {@code null}
 * @param createTime 创建时间
 * @author iBoot Team
 * @since 1.0.0
 */
public record PersonalAccessTokenRecord(Long id, Long userId, String name, String tokenId,
                                       List<String> scopes, LocalDateTime expiresAt, boolean revoked,
                                       LocalDateTime revokedAt, LocalDateTime createTime) {
}
