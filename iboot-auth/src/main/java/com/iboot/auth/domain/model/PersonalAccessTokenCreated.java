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
 * 新签发的个人访问令牌。
 *
 * <p>只在创建响应中出现一次明文；调用方保存后不再提供查询。
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
public record PersonalAccessTokenCreated(Long id, String name, List<String> scopes, String token,
                                        LocalDateTime createdAt, LocalDateTime expiresAt) {
}
