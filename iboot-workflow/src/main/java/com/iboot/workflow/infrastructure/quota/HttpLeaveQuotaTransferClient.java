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
import com.iboot.workflow.application.service.quota.LeaveQuotaTransferPort;
import com.iboot.workflow.infrastructure.config.WorkflowLeaveQuotaProperties;
import org.apache.seata.core.context.RootContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * 额度划转 HTTP 客户端。
 *
 * <p>以内部令牌调用系统服务的 TCC Try 端点，并通过共享的 XID 拦截器透传全局事务。
 * 未处于全局事务时直接失败关闭，避免划转退化为单边操作。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class HttpLeaveQuotaTransferClient implements LeaveQuotaTransferPort {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private static final String TRY_PATH = "/internal/leave/quota/transfer/try";

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
    public HttpLeaveQuotaTransferClient(@Qualifier("leaveQuotaRestClient") final RestClient restClient,
                                        final WorkflowLeaveQuotaProperties properties,
                                        final ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void transfer(final String transferId, final Long fromUserId, final Long toUserId,
                         final int year, final int count) {
        requireGlobalTransaction();
        Map<String, Object> body = new HashMap<>();
        body.put("transferId", transferId);
        body.put("fromUserId", fromUserId);
        body.put("toUserId", toUserId);
        body.put("year", year);
        body.put("count", count);
        String response;
        try {
            response = restClient.post()
                    .uri(TRY_PATH)
                    .header(TOKEN_HEADER, properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw mapError(e);
        } catch (RuntimeException e) {
            throw new BusinessException(503, "额度服务不可用，划转未完成");
        }
        ensureAccepted(response);
    }

    private void requireGlobalTransaction() {
        String xid = RootContext.getXID();
        if (xid == null || xid.isBlank()) {
            throw new BusinessException(503, "分布式事务未开启，额度划转已拒绝");
        }
    }

    private void ensureAccepted(final String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            int code = root.path("code").asInt(-1);
            if (code != 200) {
                throw new BusinessException(code == 400 || code == 409 ? code : 503,
                        root.path("message").asText("额度服务返回错误"));
            }
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
        if (status == 409) {
            return new BusinessException(409, "划转已取消");
        }
        if (status == 401 || status == 403) {
            return new BusinessException(503, "额度服务鉴权失败");
        }
        if (status == 503) {
            return new BusinessException(503, "额度服务不可用，划转未完成");
        }
        return new BusinessException(503, "额度服务返回 " + status);
    }
}
