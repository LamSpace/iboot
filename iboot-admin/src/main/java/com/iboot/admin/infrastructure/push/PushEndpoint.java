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

package com.iboot.admin.infrastructure.push;

import com.iboot.admin.common.cloudevent.CloudEventBody;
import com.iboot.admin.common.cloudevent.CloudEventTypes;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 推送端点 Controller
 * <p>
 * 提供 SSE 连接建立接口
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushEndpoint {

    private final SseEmitterManager sseEmitterManager;
    private final PushEventService pushEventService;

    /**
     * 建立 SSE 连接
     * <p>
     * 客户端通过 EventSource 连接此接口，建立长连接接收推送事件
     *
     * @return SseEmitter
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户请求建立 SSE 连接，userId: {}", userId);

        SseEmitter emitter = sseEmitterManager.connect(userId);

        // 发送连接成功事件
        PushEvent connectedEvent = PushEvent.builder()
                .id("conn-" + java.util.UUID.randomUUID())
                .type(CloudEventTypes.PUSH_CONNECTION_ESTABLISHED)
                .source("/api/push/connect")
                .time(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .dataContentType("application/json")
                .data(java.util.Map.of(
                        "userId", userId,
                        "connectedAt", java.time.LocalDateTime.now().toString(),
                        "onlineCount", sseEmitterManager.getOnlineCount()
                ))
                .targetUserId(userId)
                .build();

        try {
            emitter.send(SseEmitter.event()
                    .id(connectedEvent.getId())
                    .name(connectedEvent.getType())
                    .reconnectTime(5000L)
                    .data(connectedEvent));
            log.info("SSE 连接建立成功，userId: {}, 在线人数：{}", userId, sseEmitterManager.getOnlineCount());
        } catch (Exception e) {
            log.error("发送连接成功事件失败，userId: {}", userId, e);
        }

        return emitter;
    }

    /**
     * 获取在线状态
     *
     * @return 在线状态信息
     */
    @GetMapping("/status")
    public CloudEventBody<java.util.Map<String, Object>> getStatus() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean online = sseEmitterManager.isOnline(userId);

        java.util.Map<String, Object> status = java.util.Map.of(
                "online", online,
                "totalOnline", sseEmitterManager.getOnlineCount()
        );

        return CloudEventBody.success("/api/push/status", "在线状态查询成功", status);
    }
}
