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

package com.iboot.workflow.infrastructure.quota;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.kernel.web.BusinessException;
import com.iboot.workflow.application.service.quota.LeaveQuotaPort;
import com.iboot.workflow.application.service.quota.LeaveQuotaSnapshot;
import com.iboot.workflow.infrastructure.config.WorkflowLeaveQuotaProperties;
import org.apache.seata.core.context.RootContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * 请假额度 HTTP 客户端。
 *
 * <p>以内部令牌调用系统服务的额度端点，并在存在全局事务标识时透传给下游。
 * 扣减方法要求当前线程已处于全局事务中；没有标识时直接失败关闭，避免审批在本地
 * 提交而额度未进入同一事务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class HttpLeaveQuotaClient implements LeaveQuotaPort {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private static final String DEDUCT_PATH = "/internal/leave/quota/deduct";

    private static final String QUERY_PATH = "/internal/leave/quota";

    private final RestClient restClient;

    private final WorkflowLeaveQuotaProperties properties;

    private final ObjectMapper objectMapper;

    /**
     * 创建客户端。
     *
     * @param restClient   额度客户端
     * @param properties   额度客户端配置
     * @param objectMapper Jackson 2 映射器
     */
    public HttpLeaveQuotaClient(@Qualifier("leaveQuotaRestClient") final RestClient restClient,
                                final WorkflowLeaveQuotaProperties properties,
                                final ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LeaveQuotaSnapshot deduct(final String requestId, final Long userId, final int year) {
        requireGlobalTransaction();
        String body;
        try {
            body = restClient.post()
                    .uri(DEDUCT_PATH)
                    .header(TOKEN_HEADER, properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("requestId", requestId, "userId", userId, "year", year))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw mapError(e);
        } catch (RuntimeException e) {
            throw new BusinessException(503, "额度服务不可用，审批未完成");
        }
        return parse(body);
    }

    @Override
    public LeaveQuotaSnapshot view(final Long userId, final int year) {
        String body;
        try {
            body = restClient.get()
                    .uri(builder -> builder.path(QUERY_PATH)
                            .queryParam("userId", userId)
                            .queryParam("year", year)
                            .build())
                    .header(TOKEN_HEADER, properties.getInternalToken())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw mapError(e);
        } catch (RuntimeException e) {
            throw new BusinessException(503, "额度服务不可用");
        }
        return parse(body);
    }

    private void requireGlobalTransaction() {
        String xid = RootContext.getXID();
        if (xid == null || xid.isBlank()) {
            throw new BusinessException(503, "分布式事务未开启，审批已拒绝");
        }
    }

    private LeaveQuotaSnapshot parse(final String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                throw new BusinessException(code == 400 ? 400 : 503,
                        root.path("message").asText("额度服务返回错误"));
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new BusinessException(503, "额度服务返回为空");
            }
            return new LeaveQuotaSnapshot(
                    data.path("totalCount").asInt(),
                    data.path("usedCount").asInt(),
                    data.path("frozenCount").asInt(),
                    data.path("remainingCount").asInt());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(503, "额度服务响应无法解析");
        }
    }

    private BusinessException mapError(final RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 400) {
            return new BusinessException(400, "请假额度不足");
        }
        if (status == 401 || status == 403) {
            return new BusinessException(503, "额度服务鉴权失败");
        }
        if (status == 503) {
            return new BusinessException(503, "额度服务不可用，审批未完成");
        }
        return new BusinessException(503, "额度服务返回 " + status);
    }
}
