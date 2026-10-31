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

package com.iboot.ai.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.kernel.security.SecurityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * admin 数据权限范围客户端。
 *
 * <p>带当前用户 JWT 调用 admin 的只读 scope 接口，本地缓存 60 秒；任何失败都抛出
 * {@link PermissionScopeUnavailableException}，由调用方按失败关闭处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PermissionScopeClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;

    private final AiProperties properties;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final Cache<Long, PermissionScope> cache;

    /**
     * 创建客户端。
     *
     * @param builder    REST 客户端构建器
     * @param properties AI 配置
     */
    public PermissionScopeClient(RestClient.Builder builder, AiProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
        long cacheSeconds = Math.max(1L, properties.getPermission().getCacheSeconds());
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheSeconds))
                .maximumSize(1000)
                .build();
    }

    /**
     * 查询当前登录用户的权限范围。
     *
     * @return 权限范围快照
     * @throws PermissionScopeUnavailableException admin 不可用或响应非法时抛出
     */
    public PermissionScope fetchCurrentUserScope() {
        Long userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (RuntimeException e) {
            throw new PermissionScopeUnavailableException("无法获取当前用户", e);
        }
        return fetchScope(userId, currentToken());
    }

    /**
     * 按显式用户与令牌查询权限范围。
     *
     * <p>供不在请求线程上执行的调用方使用（如智能体工具）；与请求线程路径共用同一份缓存。
     *
     * @param userId 用户 ID
     * @param token  该用户的 Bearer 令牌
     * @return 权限范围快照
     * @throws PermissionScopeUnavailableException admin 不可用或响应非法时抛出
     */
    public PermissionScope fetchScope(Long userId, String token) {
        PermissionScope cached = cache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        String body;
        try {
            body = restClient.get()
                    .uri(properties.getPermission().getScopeUrl())
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException e) {
            throw new PermissionScopeUnavailableException("数据权限范围服务不可用", e);
        }
        PermissionScope scope = parse(body);
        cache.put(userId, scope);
        return scope;
    }

    private String currentToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new PermissionScopeUnavailableException("缺少 Authorization 请求头");
        }
        String header = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new PermissionScopeUnavailableException("缺少 Authorization 请求头");
        }
        return header.substring(BEARER_PREFIX.length());
    }

    private PermissionScope parse(String body) {
        if (body == null || body.isBlank()) {
            throw new PermissionScopeUnavailableException("数据权限范围响应为空");
        }
        try {
            JsonNode data = jsonMapper.readTree(body).path("data").path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new PermissionScopeUnavailableException("数据权限范围响应缺少 data");
            }
            return new PermissionScope(
                    data.path("userId").asLong(),
                    text(data, "username"),
                    data.path("superAdmin").asBoolean(false),
                    integer(data, "dataScope"),
                    longValue(data, "deptId"),
                    longList(data, "visibleDeptIds"),
                    longList(data, "kbVisibleDeptIds"),
                    textList(data, "permissions"),
                    textList(data, "roleCodes"));
        } catch (PermissionScopeUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PermissionScopeUnavailableException("数据权限范围响应解析失败", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private List<Long> longList(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        value.forEach(item -> result.add(item.asLong()));
        return result;
    }

    private List<String> textList(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> result.add(item.asString()));
        return result;
    }
}
