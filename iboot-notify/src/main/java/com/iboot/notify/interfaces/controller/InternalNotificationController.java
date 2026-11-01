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

package com.iboot.notify.interfaces.controller;

import com.iboot.kernel.web.BusinessException;
import com.iboot.kernel.web.Result;
import com.iboot.notify.application.service.notification.NotificationApplicationService;
import com.iboot.notify.application.service.notification.NotificationCommand;
import com.iboot.notify.domain.notification.model.NotificationEventType;
import com.iboot.notify.interfaces.dto.request.InternalNotificationRequest;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部通知登记接口。
 *
 * <p>供其他服务在业务事务提交后调用，不经网关暴露。使用共享密钥头
 * {@code X-Internal-Token} 鉴权：未配置密钥或密钥不匹配时一律拒绝（失败关闭）。
 *
 * <p>该路径不在 {@code /api/**} 下，因此不套 CloudEvents 响应信封。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Hidden
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    /**
     * 共享密钥请求头名称。
     */
    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final NotificationApplicationService notificationApplicationService;

    private final String internalToken;

    /**
     * 创建内部接口控制器。
     *
     * @param notificationApplicationService 通知编排服务
     * @param internalToken 共享密钥
     */
    public InternalNotificationController(final NotificationApplicationService notificationApplicationService,
                                          @Value("${iboot.notify.internal-token:}") final String internalToken) {
        this.notificationApplicationService = notificationApplicationService;
        this.internalToken = internalToken;
    }

    /**
     * 登记一次通知事件。
     *
     * @param token 共享密钥
     * @param request 登记请求
     * @return 空响应；共享密钥缺失或不匹配时为 HTTP 401
     * @throws BusinessException 事件类型未知、模板停用或接收人不存在时抛出
     */
    @PostMapping
    public ResponseEntity<Result<Void>> enqueue(
            @RequestHeader(value = TOKEN_HEADER, required = false) final String token,
            @RequestBody final InternalNotificationRequest request) {
        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "内部令牌无效"));
        }
        notificationApplicationService.enqueue(new NotificationCommand(
                request.getEventId(),
                parseEventType(request.getEventType()),
                request.getTemplateCode(),
                request.getRecipientId(),
                request.getParams()));
        return ResponseEntity.ok(Result.success());
    }

    private boolean isAuthorized(final String token) {
        if (internalToken == null || internalToken.isBlank() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    private NotificationEventType parseEventType(final String eventType) {
        try {
            return NotificationEventType.valueOf(eventType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(400, "未知的通知事件类型: " + eventType);
        }
    }
}
