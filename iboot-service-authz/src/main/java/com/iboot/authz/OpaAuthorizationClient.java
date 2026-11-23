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

package com.iboot.authz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OPA 授权决策客户端。
 *
 * <p>把调用方、请求路径和按 first-match 排序的规则提交到
 * {@code /v1/data/iboot/authz/allow}。任何网络、超时、非 200 或解析失败都按拒绝处理，
 * 保证 OPA 不可用时失败关闭。客户端本身不缓存决策，规则变化由 OPA 立即生效。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
final class OpaAuthorizationClient {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthorizationClient.class);

    private static final String DECISION_PATH = "/v1/data/iboot/authz/allow";

    private final HttpClient httpClient;

    private final String decisionUrl;

    private final Duration timeout;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 创建客户端。
     *
     * @param baseUrl OPA 基础地址，例如 {@code http://opa:8181}
     * @param timeout 单次决策超时
     */
    OpaAuthorizationClient(final String baseUrl, final Duration timeout) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.decisionUrl = normalized + DECISION_PATH;
        this.timeout = timeout == null || timeout.isNegative() || timeout.isZero()
                ? Duration.ofMillis(500) : timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    /**
     * 同步请求决策。
     *
     * @param path   请求路径
     * @param caller 调用方名称，可为 {@code null}
     * @param rules  有序规则
     * @return 允许返回 {@code true}；任何异常返回 {@code false}
     */
    boolean isAllowed(final String path, final String caller,
                      final List<ServiceAuthorizationRule> rules) {
        try {
            return isAllowedAsync(path, caller, rules).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("OPA 决策失败，按拒绝处理：{}", e.toString());
            return false;
        }
    }

    /**
     * 异步请求决策，供 WebFlux 过滤器使用。
     *
     * @param path   请求路径
     * @param caller 调用方名称，可为 {@code null}
     * @param rules  有序规则
     * @return 允许为 {@code true}；异常时为 {@code false}
     */
    CompletableFuture<Boolean> isAllowedAsync(final String path, final String caller,
                                              final List<ServiceAuthorizationRule> rules) {
        final String body;
        try {
            body = requestBody(path, caller, rules);
        } catch (Exception e) {
            log.warn("构造 OPA 请求失败，按拒绝处理：{}", e.toString());
            return CompletableFuture.completedFuture(false);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(decisionUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseDecision(response.statusCode(), response.body()))
                .exceptionally(error -> {
                    log.warn("OPA 请求失败，按拒绝处理：{}", error.toString());
                    return false;
                });
    }

    private boolean parseDecision(final int statusCode, final String body) {
        if (statusCode != 200) {
            log.warn("OPA 返回非 200：{}", statusCode);
            return false;
        }
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode result = root.get("result");
            return result != null && result.asBoolean(false);
        } catch (Exception e) {
            log.warn("解析 OPA 响应失败，按拒绝处理：{}", e.toString());
            return false;
        }
    }

    private String requestBody(final String path, final String caller,
                               final List<ServiceAuthorizationRule> rules) {
        List<Map<String, Object>> ruleInputs = rules.stream()
                .map(rule -> Map.<String, Object>of(
                        "paths", rule.getPaths(),
                        "callers", rule.getCallers()))
                .toList();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("path", path);
        input.put("caller", caller == null ? "" : caller);
        input.put("rules", ruleInputs);
        return mapper.writeValueAsString(Map.of("input", input));
    }
}
