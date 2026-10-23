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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.notification.NotificationApplicationService;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationDeliveryStatus;
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.iboot.admin.interfaces.dto.response.NotificationDeliveryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationDeliveryController} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationDeliveryControllerTest {

    private NotificationApplicationService service;

    private NotificationDeliveryController controller;

    @BeforeEach
    void setUp() {
        service = mock(NotificationApplicationService.class);
        controller = new NotificationDeliveryController(service);
    }

    /**
     * 验证列表返回脱敏后的接收人与错误。
     */
    @Test
    void listMasksRecipientsAndErrors() {
        when(service.findPage(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(delivery(NotificationChannelType.EMAIL, "alice@example.com", "USER:2"),
                        delivery(NotificationChannelType.SMS, "13800138000", "USER:3"),
                        delivery(NotificationChannelType.DINGTALK, null, "GROUP:default")));
        when(service.count(any(), any(), any(), any(), any(), any())).thenReturn(3L);

        Result<PageResult<NotificationDeliveryResponse>> result = controller.list(1, 10, null, null, null, null,
                null, null);

        List<NotificationDeliveryResponse> rows = result.getData().getData();
        assertThat(rows).extracting(NotificationDeliveryResponse::getRecipient)
                .containsExactly("a***@example.com", "138****8000", "GROUP:default");
        assertThat(rows.get(0).getLastError()).doesNotContain("abc123").contains("***");
    }

    /**
     * 验证人工重试委托给应用服务。
     */
    @Test
    void retryDelegatesToService() {
        Result<Void> result = controller.retry(9L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(service).retryManually(9L);
    }

    private NotificationDelivery delivery(final NotificationChannelType channel, final String address,
                                          final String recipientKey) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setId(1L);
        delivery.setEventId("approval-todo-pid-1");
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(channel);
        delivery.setRecipientKey(recipientKey);
        delivery.setRecipientId(2L);
        delivery.setRecipientAddress(address);
        delivery.setTemplateCode("APPROVAL_TODO");
        delivery.setTitle("审批待办通知");
        delivery.setStatus(NotificationDeliveryStatus.FAILED);
        delivery.setAttempts(1);
        delivery.setMaxAttempts(5);
        delivery.setLastError("failed access_token=abc123");
        return delivery;
    }
}
