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

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OIDC 发现与令牌端点客户端。
 *
 * <p>只使用标准端点：发现文档、授权码兑换与刷新令牌。客户端是公共客户端，不发送
 * 客户端密钥；所有表单参数按 {@code application/x-www-form-urlencoded} 编码。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class OidcClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final long DEFAULT_EXPIRES_IN_SECONDS = 30 * 60L;

    private final HttpClient httpClient;

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 创建客户端。
     */
    public OidcClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    OidcClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 读取 OIDC 发现文档。
     *
     * @param context 当前上下文
     * @return 发现文档中的端点
     */
    public Discovery discover(Context context) {
        URI uri = URI.create(context.issuer() + "/.well-known/openid-configuration");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new CliException(ExitCode.NETWORK,
                        "读取 OIDC 发现文档失败：HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            String authorization = text(root, "authorization_endpoint");
            String token = text(root, "token_endpoint");
            if (authorization == null || token == null) {
                throw new CliException(ExitCode.BUSINESS, "OIDC 发现文档缺少授权或令牌端点");
            }
            return new Discovery(text(root, "issuer"), authorization, token,
                    text(root, "end_session_endpoint"));
        } catch (IOException e) {
            throw new CliException(ExitCode.NETWORK,
                    "无法连接认证服务 " + context.issuer() + "：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(ExitCode.NETWORK, "读取 OIDC 发现文档被中断", e);
        }
    }

    /**
     * 用授权码兑换令牌。
     *
     * @param discovery 发现文档端点
     * @param code 授权码
     * @param verifier PKCE code verifier
     * @param redirectUri 回调地址
     * @param clientId 客户端标识
     * @return 令牌集合
     */
    public TokenSet exchange(Discovery discovery, String code, String verifier,
                             String redirectUri, String clientId) {
        return tokenRequest(discovery.tokenEndpoint(), Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri,
                "client_id", clientId,
                "code_verifier", verifier));
    }

    /**
     * 用刷新令牌换取新令牌。
     *
     * @param discovery 发现文档端点
     * @param refreshToken 刷新令牌
     * @param clientId 客户端标识
     * @return 新的令牌集合
     */
    public TokenSet refresh(Discovery discovery, String refreshToken, String clientId) {
        return tokenRequest(discovery.tokenEndpoint(), Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", clientId));
    }

    private TokenSet tokenRequest(String endpoint, Map<String, String> form) {
        String body = form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return parseToken(response.body());
            }
            throw tokenError(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new CliException(ExitCode.NETWORK,
                    "无法连接令牌端点 " + endpoint + "：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(ExitCode.NETWORK, "令牌请求被中断", e);
        }
    }

    private TokenSet parseToken(String body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "令牌响应不是合法 JSON", e);
        }
        String accessToken = text(root, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new CliException(ExitCode.BUSINESS, "令牌响应缺少 access_token");
        }
        long expiresIn = root.has("expires_in") && root.get("expires_in").isNumber()
                ? root.get("expires_in").asLong()
                : DEFAULT_EXPIRES_IN_SECONDS;
        long expiresAt = System.currentTimeMillis() + Math.max(expiresIn, 60) * 1000 - 30_000;
        String tokenType = text(root, "token_type");
        return new TokenSet(accessToken, text(root, "refresh_token"),
                text(root, "id_token"), expiresAt,
                tokenType == null || tokenType.isBlank() ? "Bearer" : tokenType);
    }

    private CliException tokenError(int status, String body) {
        String error = null;
        String description = null;
        try {
            JsonNode root = mapper.readTree(body);
            error = text(root, "error");
            description = text(root, "error_description");
        } catch (RuntimeException ignored) {
            // 非 JSON 错误体按状态码处理。
        }
        if ("invalid_grant".equals(error) || status == 401) {
            return new CliException(ExitCode.AUTH,
                    description == null ? "登录已失效，请重新执行 iboot login" : description);
        }
        if (status >= 500) {
            return new CliException(ExitCode.NETWORK, "认证服务暂时不可用：HTTP " + status);
        }
        String message = description != null ? description
                : error != null ? error : "令牌请求失败：HTTP " + status;
        return new CliException(ExitCode.BUSINESS, message);
    }

    private String text(JsonNode root, String field) {
        if (root == null || !root.has(field)) {
            return null;
        }
        JsonNode value = root.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * OIDC 发现文档中的端点。
     *
     * @param issuer 签发者
     * @param authorizationEndpoint 授权端点
     * @param tokenEndpoint 令牌端点
     * @param endSessionEndpoint 登出端点，可为 {@code null}
     * @author iBoot Team
     * @since 1.0.0
     */
    public record Discovery(String issuer, String authorizationEndpoint, String tokenEndpoint,
                            String endSessionEndpoint) {
    }
}
