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
 * {@link DingTalkNotificationChannel} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DingTalkNotificationChannelTest {

    private FakeDingTalkClient client;

    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        client = new FakeDingTalkClient();
        properties = new NotificationProperties();
        properties.getDingtalk().setEnabled(true);
        properties.getDingtalk().setWebhook("https://oapi.dingtalk.com/robot/send?access_token=x");
        properties.getDingtalk().setSecret("SEC-test");
    }

    /**
     * 验证消息带关键词前缀并交给客户端。
     */
    @Test
    void sendsWithKeywordPrefix() {
        DingTalkNotificationChannel channel = new DingTalkNotificationChannel(client, properties);

        channel.send(delivery());

        assertThat(client.content).isEqualTo("【iBoot】您有一条新的审批待办");
        assertThat(client.webhook).contains("access_token");
        assertThat(client.secret).isEqualTo("SEC-test");
    }

    /**
     * 验证每分钟限流生效，超限抛出可重试异常。
     */
    @Test
    void rejectsWhenRateLimitExceeded() {
        properties.getDingtalk().setRatePerMinute(1);
        DingTalkNotificationChannel channel = new DingTalkNotificationChannel(client, properties);

        channel.send(delivery());

        assertThatThrownBy(() -> channel.send(delivery()))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isTrue());
    }

    /**
     * 验证 Webhook 或 Secret 缺失时渠道禁用。
     */
    @Test
    void disabledWithoutWebhookOrSecret() {
        properties.getDingtalk().setSecret(null);
        DingTalkNotificationChannel channel = new DingTalkNotificationChannel(client, properties);

        assertThat(channel.isEnabled()).isFalse();
    }

    /**
     * 验证客户端异常按可重试失败向上抛出。
     */
    @Test
    void propagatesClientFailure() {
        client.failure = new NotificationDeliveryException("errcode=310000", true);
        DingTalkNotificationChannel channel = new DingTalkNotificationChannel(client, properties);

        assertThatThrownBy(() -> channel.send(delivery()))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isTrue());
    }

    private NotificationDelivery delivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(NotificationChannelType.DINGTALK);
        delivery.setRecipientKey("GROUP:default");
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条新的审批待办");
        return delivery;
    }

    /**
     * 记录调用参数的测试客户端。
     */
    private static final class FakeDingTalkClient implements DingTalkClient {

        private String webhook;

        private String secret;

        private String content;

        private NotificationDeliveryException failure;

        @Override
        public void send(final String webhook, final String secret, final String content) {
            this.webhook = webhook;
            this.secret = secret;
            this.content = content;
            if (failure != null) {
                throw failure;
            }
        }
    }
}
