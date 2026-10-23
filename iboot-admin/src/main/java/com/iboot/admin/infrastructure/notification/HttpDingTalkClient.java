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

package com.iboot.admin.infrastructure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 基于 JDK {@link HttpClient} 的钉钉客户端。
 *
 * <p>发送文本消息并校验返回的 {@code errcode}；网络错误、HTTP 非 2xx 与业务错误
 * 均按可重试失败处理，错误摘要不包含 Webhook 或 Secret。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class HttpDingTalkClient implements DingTalkClient {

    private final DingTalkSigner signer;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    @SuppressWarnings("all")
    public HttpDingTalkClient(final DingTalkSigner signer, final ObjectMapper objectMapper) {
        this.signer = signer;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void send(final String webhook, final String secret, final String content) {
        long timestamp = System.currentTimeMillis();
        String sign = signer.sign(secret, timestamp);
        String url = webhook + "&timestamp=" + timestamp + "&sign=" + sign;
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", content == null ? "" : content)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NotificationDeliveryException("钉钉 HTTP 状态异常: " + response.statusCode(), true);
            }
            JsonNode result = objectMapper.readTree(response.body());
            int errcode = result.path("errcode").asInt(-1);
            if (errcode != 0) {
                throw new NotificationDeliveryException("钉钉发送失败: errcode=" + errcode
                        + ", errmsg=" + result.path("errmsg").asText(""), true);
            }
        } catch (NotificationDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationDeliveryException(NotificationSanitizer.sanitizeError(e.getMessage()), true);
        }
    }
}
