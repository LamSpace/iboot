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

package com.iboot.ai.infrastructure.tool;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 以调用者身份代理访问 admin 接口。
 *
 * <p>所有请求携带调用者的 Bearer 令牌与固定的 API 版本头，权限与数据范围由 admin 侧校验，
 * 本类不复刻鉴权逻辑。响应以原始 JSON 字符串返回，由 {@link ToolResultTrimmer} 负责裁剪。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class AdminApiClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String VERSION_HEADER = "Accept-Version";

    private static final String VERSION = "1";

    private final RestClient restClient;

    /**
     * 创建客户端。
     *
     * @param restClient 指向 admin 的 REST 客户端
     */
    public AdminApiClient(@Qualifier("adminRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 查询用户列表。
     *
     * @param token    调用者令牌
     * @param username 用户名关键字，可为 {@code null}
     * @param status   状态 0-停用 1-启用，可为 {@code null}
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 原始响应 JSON
     * @throws AdminApiException 下游不可用或返回错误
     */
    public String queryUsers(String token, String username, Integer status, int pageNum, int pageSize) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        params.put("username", username);
        params.put("status", status);
        return get(token, "/api/user/list", params);
    }

    /**
     * 查询登录日志列表。
     *
     * @param token     调用者令牌
     * @param username  用户名关键字，可为 {@code null}
     * @param ipAddress IP 关键字，可为 {@code null}
     * @param status    状态 0-失败 1-成功，可为 {@code null}
     * @param beginTime 开始时间，格式 {@code yyyy-MM-dd HH:mm:ss}，可为 {@code null}
     * @param endTime   结束时间，格式同上，可为 {@code null}
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 原始响应 JSON
     * @throws AdminApiException 下游不可用或返回错误
     */
    public String queryLoginLogs(String token, String username, String ipAddress, Integer status,
                                 String beginTime, String endTime, int pageNum, int pageSize) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        params.put("username", username);
        params.put("ipAddress", ipAddress);
        params.put("status", status);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        return get(token, "/api/log/login/list", params);
    }

    /**
     * 查询备份概览。
     *
     * @param token 调用者令牌
     * @return 原始响应 JSON
     * @throws AdminApiException 下游不可用或返回错误
     */
    public String backupSummary(String token) {
        return get(token, "/api/monitor/backup/summary", Map.of());
    }

    /**
     * 触发一次手动备份。
     *
     * @param token 调用者令牌
     * @return 原始响应 JSON
     * @throws AdminApiException 下游不可用或返回错误
     */
    public String triggerBackup(String token) {
        requireToken(token);
        try {
            return restClient.post()
                    .uri("/api/monitor/backup/records")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .header(VERSION_HEADER, VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new AdminApiException(e.getStatusCode().value(), downstreamMessage(e), e);
        } catch (RuntimeException e) {
            throw new AdminApiException(0, "admin 服务不可用", e);
        }
    }

    private String get(String token, String path, Map<String, Object> params) {
        requireToken(token);
        try {
            return restClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        params.forEach((key, value) -> {
                            if (value != null) {
                                builder.queryParam(key, value);
                            }
                        });
                        return builder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .header(VERSION_HEADER, VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new AdminApiException(e.getStatusCode().value(), downstreamMessage(e), e);
        } catch (RuntimeException e) {
            throw new AdminApiException(0, "admin 服务不可用", e);
        }
    }

    private String downstreamMessage(RestClientResponseException e) {
        if (e.getStatusCode().value() == 403) {
            return "无权访问下游接口";
        }
        if (e.getStatusCode().value() == 401) {
            return "登录态已失效";
        }
        return "下游接口返回 " + e.getStatusCode().value();
    }

    private void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AdminApiException(401, "缺少登录令牌");
        }
    }
}
