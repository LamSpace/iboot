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

package com.iboot.file.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.iboot.kernel.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端数据权限范围客户端。
 *
 * <p>带当前用户 JWT 调用管理端的只读 scope 接口，本地缓存 60 秒；任何失败都抛出
 * {@link ScopeUnavailableException}，由调用方按失败关闭处理。
 *
 * <p>这是路线图 D10 的方向：数据权限由 system 提供 scope，各服务本地缓存并自行应用过滤。
 * 退出条件：管理端提供稳定的 scope 契约后，调用地址切到 {@code iboot-system}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PermissionScopeClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final RestClient restClient;

    private final String scopeUrl;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final Cache<Long, FilePermissionScope> cache;

    /**
     * 创建客户端。
     *
     * @param builder REST 客户端构建器
     * @param scopeUrl 管理端 scope 接口地址
     * @param cacheSeconds 缓存秒数
     */
    public PermissionScopeClient(RestClient.Builder builder,
                                 @Value("${iboot.file.permission.scope-url:http://localhost:8080/api/auth/scope}")
                                 String scopeUrl,
                                 @Value("${iboot.file.permission.cache-seconds:60}") long cacheSeconds) {
        this.restClient = builder.build();
        this.scopeUrl = scopeUrl;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(Math.max(1L, cacheSeconds)))
                .maximumSize(1000)
                .build();
    }

    /**
     * 查询当前登录用户的数据权限范围。
     *
     * @return 权限范围快照
     * @throws ScopeUnavailableException 管理端不可用、响应非法或缺少凭证时抛出
     */
    public FilePermissionScope fetchCurrentUserScope() {
        Long userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (RuntimeException e) {
            throw new ScopeUnavailableException("无法获取当前用户", e);
        }
        FilePermissionScope cached = cache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        String token;
        try {
            token = SecurityUtils.getCurrentBearerToken();
        } catch (RuntimeException e) {
            throw new ScopeUnavailableException("缺少 Authorization 请求头", e);
        }
        String body;
        try {
            body = restClient.get()
                    .uri(scopeUrl)
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .retrieve()
                    .body(String.class);
        } catch (RuntimeException e) {
            throw new ScopeUnavailableException("数据权限范围服务不可用", e);
        }
        FilePermissionScope scope = parse(body);
        cache.put(userId, scope);
        return scope;
    }

    private FilePermissionScope parse(String body) {
        if (body == null || body.isBlank()) {
            throw new ScopeUnavailableException("数据权限范围响应为空");
        }
        try {
            JsonNode data = jsonMapper.readTree(body).path("data").path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new ScopeUnavailableException("数据权限范围响应缺少 data");
            }
            return new FilePermissionScope(
                    data.path("dataScope").isMissingNode() || data.path("dataScope").isNull()
                            ? null : data.path("dataScope").asInt(),
                    data.path("deptId").isMissingNode() || data.path("deptId").isNull()
                            ? null : data.path("deptId").asLong(),
                    longList(data, "visibleDeptIds"),
                    data.path("superAdmin").asBoolean(false));
        } catch (ScopeUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ScopeUnavailableException("数据权限范围响应解析失败", e);
        }
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
}
