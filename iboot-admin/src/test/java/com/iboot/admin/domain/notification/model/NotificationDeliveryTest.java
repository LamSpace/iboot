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

package com.iboot.admin.domain.notification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NotificationDelivery} 的状态流转测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationDeliveryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0);

    /**
     * 验证新建投递可被领取。
     */
    @Test
    void newDeliveryCanBeClaimed() {
        NotificationDelivery delivery = delivery();

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(delivery.canClaim(NOW)).isTrue();
    }

    /**
     * 验证已被领取的投递不可再次领取。
     */
    @Test
    void sendingDeliveryCannotBeClaimed() {
        NotificationDelivery delivery = delivery();
        delivery.markSending();

        assertThat(delivery.canClaim(NOW)).isFalse();
    }

    /**
     * 验证失败后按指数退避安排重试，达到上限进入 DEAD。
     */
    @Test
    void failureSchedulesExponentialRetryThenDead() {
        NotificationDelivery delivery = delivery();

        delivery.markFailed("smtp timeout", 3, 60, NOW);
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getNextRetryTime()).isEqualTo(NOW.plusSeconds(60));

        delivery.markFailed("smtp timeout", 3, 60, NOW.plusSeconds(60));
        assertThat(delivery.getAttempts()).isEqualTo(2);
        assertThat(delivery.getNextRetryTime()).isEqualTo(NOW.plusSeconds(180));

        delivery.markFailed("smtp timeout", 3, 60, NOW.plusSeconds(180));
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.DEAD);
        assertThat(delivery.getNextRetryTime()).isNull();
    }

    /**
     * 验证成功时记录消息 ID 与发送时间并清理重试时间。
     */
    @Test
    void successRecordsMessageAndClearsRetry() {
        NotificationDelivery delivery = delivery();
        delivery.markFailed("temporary", 5, 60, NOW);

        delivery.markSuccess(99L, NOW.plusMinutes(1));

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SUCCESS);
        assertThat(delivery.getMessageId()).isEqualTo(99L);
        assertThat(delivery.getSentTime()).isEqualTo(NOW.plusMinutes(1));
        assertThat(delivery.getNextRetryTime()).isNull();
        assertThat(delivery.getLastError()).isNull();
    }

    /**
     * 验证跳过状态不安排重试。
     */
    @Test
    void skipClearsRetry() {
        NotificationDelivery delivery = delivery();
        delivery.markFailed("temporary", 5, 60, NOW);

        delivery.markSkipped("邮箱为空");

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
        assertThat(delivery.getNextRetryTime()).isNull();
    }

    /**
     * 验证人工重试重置尝试次数并立即可领取。
     */
    @Test
    void manualRetryResetsAttemptsAndMakesClaimable() {
        NotificationDelivery delivery = delivery();
        delivery.markFailed("permanent", 1, 60, NOW);
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.DEAD);

        delivery.resetForRetry(NOW);

        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(delivery.getAttempts()).isZero();
        assertThat(delivery.canClaim(NOW)).isTrue();
    }

    private NotificationDelivery delivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventId("event-1");
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(NotificationChannelType.EMAIL);
        delivery.setRecipientKey("USER:2");
        delivery.setRecipientId(2L);
        delivery.setRecipientAddress("user@example.com");
        delivery.setTemplateCode("APPROVAL_TODO");
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条审批待办");
        delivery.setMaxAttempts(5);
        delivery.setCreateTime(NOW);
        delivery.setUpdateTime(NOW);
        return delivery;
    }
}
