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

/**
 * 调度服务的下游客户端。
 *
 * <p>以调用者身份代理访问 {@code iboot-job} 的备份接口：权限与数据范围由调度服务自行校验，
 * 本类不复刻鉴权逻辑。响应以原始 JSON 字符串返回，由 {@link ToolResultTrimmer} 负责裁剪。
 *
 * <p>与 {@link AdminApiClient} 分开的原因是两者指向不同的服务：备份接口已随拆分迁到调度服务，
 * 而用户与登录日志仍由管理端提供。两个客户端都按注册中心里的服务名寻址。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class JobApiClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String VERSION_HEADER = "Accept-Version";

    private static final String VERSION = "1";

    private final RestClient restClient;

    /**
     * 创建客户端。
     *
     * @param restClient 指向调度服务的 REST 客户端
     */
    public JobApiClient(@Qualifier("jobRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 查询备份概览。
     *
     * @param token 调用者令牌
     * @return 原始响应 JSON
     * @throws AdminApiException 下游不可用或返回错误
     */
    public String backupSummary(String token) {
        requireToken(token);
        try {
            return restClient.get()
                    .uri("/api/monitor/backup/summary")
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .header(VERSION_HEADER, VERSION)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new AdminApiException(e.getStatusCode().value(), downstreamMessage(e), e);
        } catch (RuntimeException e) {
            throw new AdminApiException(0, "job 服务不可用", e);
        }
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
            throw new AdminApiException(0, "job 服务不可用", e);
        }
    }

    private void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AdminApiException(401, "缺少登录令牌");
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
}
