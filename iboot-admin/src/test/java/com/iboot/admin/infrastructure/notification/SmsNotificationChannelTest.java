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

import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationEventType;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SmsNotificationChannel} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SmsNotificationChannelTest {

    private FakeSmsClient client;

    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        client = new FakeSmsClient();
        properties = new NotificationProperties();
        properties.getSms().setEnabled(true);
        properties.getSms().setAccessKeyId("ak");
        properties.getSms().setAccessKeySecret("sk");
        properties.getSms().setSignName("iBoot");
        properties.getSms().setTemplateCode("SMS_123");
    }

    /**
     * 验证默认关闭时不启用，也不调用客户端。
     */
    @Test
    void disabledByDefault() {
        NotificationProperties defaults = new NotificationProperties();
        SmsNotificationChannel channel = new SmsNotificationChannel(client, defaults);

        assertThat(channel.isEnabled()).isFalse();
    }

    /**
     * 验证启用后传递手机号、签名、模板与参数。
     */
    @Test
    void sendsConfiguredTemplate() {
        SmsNotificationChannel channel = new SmsNotificationChannel(client, properties);
        NotificationDelivery delivery = delivery();
        delivery.setParamsJson("{\"taskName\":\"年假\"}");

        channel.send(delivery);

        assertThat(client.phone).isEqualTo("13800138000");
        assertThat(client.signName).isEqualTo("iBoot");
        assertThat(client.templateCode).isEqualTo("SMS_123");
        assertThat(client.paramsJson).contains("年假");
    }

    /**
     * 验证手机号为空时不可重试失败。
     */
    @Test
    void rejectsMissingPhone() {
        SmsNotificationChannel channel = new SmsNotificationChannel(client, properties);
        NotificationDelivery delivery = delivery();
        delivery.setRecipientAddress(null);

        assertThatThrownBy(() -> channel.send(delivery))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isFalse());
    }

    /**
     * 验证凭证缺失时渠道禁用。
     */
    @Test
    void disabledWhenCredentialsMissing() {
        properties.getSms().setTemplateCode(null);
        SmsNotificationChannel channel = new SmsNotificationChannel(client, properties);

        assertThat(channel.isEnabled()).isFalse();
    }

    private NotificationDelivery delivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(NotificationChannelType.SMS);
        delivery.setRecipientId(2L);
        delivery.setRecipientAddress("13800138000");
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条新的审批待办");
        delivery.setParamsJson("{}");
        return delivery;
    }

    /**
     * 记录调用参数的测试客户端。
     */
    private static final class FakeSmsClient implements SmsClient {

        private String phone;

        private String signName;

        private String templateCode;

        private String paramsJson;

        @Override
        public void send(final String phone, final String signName, final String templateCode,
                         final String paramsJson) {
            this.phone = phone;
            this.signName = signName;
            this.templateCode = templateCode;
            this.paramsJson = paramsJson;
        }
    }
}
