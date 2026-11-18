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

package com.iboot.cli.http;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;

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
 * 经网关发送业务请求。
 *
 * <p>统一携带 Bearer 令牌、{@code Accept-Version: 1} 与 {@code X-Client: iboot-cli}
 * 请求头；响应交给 {@link ResponseDecoder} 解包。CLI 不判断权限，也不在本地重试
 * 非幂等请求。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class GatewayClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final Context context;

    private final TokenProvider tokenProvider;

    private final HttpClient httpClient;

    private final ResponseDecoder decoder = new ResponseDecoder();

    /**
     * 创建客户端。
     *
     * @param context 当前上下文
     * @param tokenProvider 令牌来源
     */
    public GatewayClient(Context context, TokenProvider tokenProvider) {
        this(context, tokenProvider, HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    GatewayClient(Context context, TokenProvider tokenProvider, HttpClient httpClient) {
        this.context = context;
        this.tokenProvider = tokenProvider;
        this.httpClient = httpClient;
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 接口路径
     * @param query 查询参数
     * @return 已解包响应
     */
    public ApiResponse get(String path, Map<String, String> query) {
        return request("GET", path, null, query, false);
    }

    /**
     * 发送 POST 请求。
     *
     * @param path 接口路径
     * @param body JSON 请求体
     * @return 已解包响应
     */
    public ApiResponse post(String path, String body) {
        return request("POST", path, body, Map.of(), false);
    }

    /**
     * 发送 PUT 请求。
     *
     * @param path 接口路径
     * @param body JSON 请求体
     * @return 已解包响应
     */
    public ApiResponse put(String path, String body) {
        return request("PUT", path, body, Map.of(), false);
    }

    /**
     * 发送 DELETE 请求。
     *
     * @param path 接口路径
     * @return 已解包响应
     */
    public ApiResponse delete(String path) {
        return request("DELETE", path, null, Map.of(), false);
    }

    /**
     * 发送任意方法请求。
     *
     * @param method HTTP 方法
     * @param path 接口路径
     * @param body 请求体，可为 {@code null}
     * @param query 查询参数
     * @param raw 是否跳过解包
     * @return 已解包响应
     */
    public ApiResponse request(String method, String path, String body,
                               Map<String, String> query, boolean raw) {
        URI uri = uri(path, query);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Accept-Version", "1")
                .header("X-Client", "iboot-cli");
        String token = tokenProvider.accessToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return decoder.decode(response.statusCode(), response.body(), raw);
        } catch (IOException e) {
            throw new CliException(ExitCode.NETWORK,
                    "无法连接网关 " + context.baseUrl() + "：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(ExitCode.NETWORK, "请求被中断", e);
        }
    }

    private URI uri(String path, Map<String, String> query) {
        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new CliException(ExitCode.USAGE, "接口路径必须以 / 开头：" + path);
        }
        String queryString = query == null || query.isEmpty()
                ? ""
                : query.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        String url = context.baseUrl() + path + (queryString.isEmpty() ? "" : "?" + queryString);
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new CliException(ExitCode.USAGE, "接口地址非法：" + url, e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
