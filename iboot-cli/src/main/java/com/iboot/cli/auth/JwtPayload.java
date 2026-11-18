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

package com.iboot.cli.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 访问令牌中的展示用声明。
 *
 * <p>只用于 {@code status} 展示，不做签名校验；安全判断始终由资源服务完成。
 *
 * @param username 用户名
 * @param nickname 昵称
 * @param userId 用户 ID
 * @param expiresAt 到期时间（epoch 毫秒），无法解析时为 {@code 0}
 * @author iBoot Team
 * @since 1.0.0
 */
public record JwtPayload(String username, String nickname, Long userId, long expiresAt) {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * 解析 JWT 载荷。
     *
     * @param token 访问令牌
     * @return 载荷；无法解析时返回 {@code null}
     */
    public static JwtPayload parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            String payload = parts[1];
            int padding = (4 - payload.length() % 4) % 4;
            byte[] decoded = Base64.getUrlDecoder().decode(payload + "=".repeat(padding));
            JsonNode root = MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
            String username = text(root, "username");
            String nickname = text(root, "nickname");
            Long userId = root.has("userId") && root.get("userId").isNumber()
                    ? root.get("userId").asLong() : null;
            long expiresAt = root.has("exp") && root.get("exp").isNumber()
                    ? root.get("exp").asLong() * 1000 : 0L;
            return new JwtPayload(username, nickname, userId, expiresAt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(JsonNode root, String field) {
        if (root == null || !root.has(field)) {
            return null;
        }
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }
}
