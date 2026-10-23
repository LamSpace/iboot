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
import com.iboot.admin.domain.notification.model.NotificationSendResult;
import com.iboot.admin.domain.notification.service.NotificationChannel;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import org.springframework.stereotype.Component;

/**
 * 阿里云短信渠道。
 *
 * <p>本期默认关闭；启用后使用配置的签名、模板 Code 与模板参数发送。手机号缺失视为
 * 不可重试失败，API 故障按可重试失败处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsClient smsClient;

    private final NotificationProperties properties;

    @SuppressWarnings("all")
    public SmsNotificationChannel(final SmsClient smsClient, final NotificationProperties properties) {
        this.smsClient = smsClient;
        this.properties = properties;
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.SMS;
    }

    @Override
    public boolean isEnabled() {
        NotificationProperties.Sms config = properties.getSms();
        return config.isEnabled()
                && !isBlank(config.getAccessKeyId())
                && !isBlank(config.getAccessKeySecret())
                && !isBlank(config.getSignName())
                && !isBlank(config.getTemplateCode());
    }

    @Override
    public NotificationSendResult send(final NotificationDelivery delivery) {
        if (isBlank(delivery.getRecipientAddress())) {
            throw new NotificationDeliveryException("短信接收手机号为空", false);
        }
        NotificationProperties.Sms config = properties.getSms();
        smsClient.send(delivery.getRecipientAddress(), config.getSignName(), config.getTemplateCode(),
                delivery.getParamsJson());
        return NotificationSendResult.none();
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
