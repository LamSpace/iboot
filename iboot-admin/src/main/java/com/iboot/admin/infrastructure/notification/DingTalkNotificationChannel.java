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

import com.google.common.util.concurrent.RateLimiter;
import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.service.NotificationChannel;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 钉钉群机器人通知渠道。
 *
 * <p>发送前做 20 条/分钟限流（可配置），超限按可重试失败处理；正文自动加关键词前缀，
 * 兼容机器人同时启用“自定义关键词”的场景。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DingTalkNotificationChannel implements NotificationChannel {

    private static final int DEFAULT_RATE_PER_MINUTE = 20;

    private final DingTalkClient client;

    private final NotificationProperties properties;

    private final RateLimiter rateLimiter;

    @SuppressWarnings("all")
    public DingTalkNotificationChannel(final DingTalkClient client, final NotificationProperties properties) {
        this.client = client;
        this.properties = properties;
        int rate = properties.getDingtalk().getRatePerMinute() > 0
                ? properties.getDingtalk().getRatePerMinute() : DEFAULT_RATE_PER_MINUTE;
        this.rateLimiter = RateLimiter.create(rate / 60.0);
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.DINGTALK;
    }

    @Override
    public boolean isEnabled() {
        NotificationProperties.DingTalk config = properties.getDingtalk();
        return config.isEnabled() && !isBlank(config.getWebhook()) && !isBlank(config.getSecret());
    }

    @Override
    public NotificationSendResult send(final NotificationDelivery delivery) {
        if (!rateLimiter.tryAcquire(0, TimeUnit.SECONDS)) {
            throw new NotificationDeliveryException("钉钉发送频率超限，等待重试", true);
        }
        NotificationProperties.DingTalk config = properties.getDingtalk();
        String prefix = config.getKeywordPrefix() == null ? "" : config.getKeywordPrefix();
        client.send(config.getWebhook(), config.getSecret(), prefix + delivery.getContent());
        return NotificationSendResult.none();
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
