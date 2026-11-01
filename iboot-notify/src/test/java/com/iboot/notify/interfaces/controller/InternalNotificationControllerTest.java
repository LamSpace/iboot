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

import com.iboot.kernel.web.Result;
import com.iboot.notify.application.service.notification.NotificationApplicationService;
import com.iboot.notify.application.service.notification.NotificationCommand;
import com.iboot.notify.domain.notification.model.NotificationEventType;
import com.iboot.notify.interfaces.dto.request.InternalNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link InternalNotificationController} 的单元测试。
 *
 * <p>覆盖共享密钥鉴权与命令转换：密钥正确时登记一次通知，密钥缺失或不匹配时失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class InternalNotificationControllerTest {

    private static final String TOKEN = "internal-secret";

    private NotificationApplicationService notificationApplicationService;

    private InternalNotificationController controller;

    @BeforeEach
    void setUp() {
        notificationApplicationService = mock(NotificationApplicationService.class);
        controller = new InternalNotificationController(notificationApplicationService, TOKEN);
    }

    /**
     * 验证密钥正确时把请求转换为登记命令。
     */
    @Test
    void acceptsRequestWithValidToken() {
        ResponseEntity<Result<Void>> response = controller.enqueue(TOKEN, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationApplicationService).enqueue(captor.capture());
        NotificationCommand command = captor.getValue();
        assertThat(command.getEventId()).isEqualTo("approval-todo-pid-1");
        assertThat(command.getEventType()).isEqualTo(NotificationEventType.APPROVAL_TODO);
        assertThat(command.getTemplateCode()).isEqualTo("APPROVAL_TODO");
        assertThat(command.getRecipientId()).isEqualTo(2L);
        assertThat(command.getParams()).containsEntry("taskName", "年假");
    }

    /**
     * 验证密钥缺失时拒绝请求。
     */
    @Test
    void rejectsRequestWithoutToken() {
        ResponseEntity<Result<Void>> response = controller.enqueue(null, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
        verifyNoInteractions(notificationApplicationService);
    }

    /**
     * 验证密钥不匹配时拒绝请求。
     */
    @Test
    void rejectsRequestWithWrongToken() {
        ResponseEntity<Result<Void>> response = controller.enqueue("wrong", request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(notificationApplicationService);
    }

    /**
     * 验证未配置密钥时端点失败关闭。
     */
    @Test
    void rejectsWhenServerTokenMissing() {
        InternalNotificationController closed = new InternalNotificationController(notificationApplicationService, "");

        ResponseEntity<Result<Void>> response = closed.enqueue("", request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private InternalNotificationRequest request() {
        InternalNotificationRequest request = new InternalNotificationRequest();
        request.setEventId("approval-todo-pid-1");
        request.setEventType("APPROVAL_TODO");
        request.setTemplateCode("APPROVAL_TODO");
        request.setRecipientId(2L);
        request.setParams(Map.of("taskName", "年假"));
        return request;
    }
}
