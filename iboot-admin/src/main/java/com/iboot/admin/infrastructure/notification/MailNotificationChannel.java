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
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件通知渠道。
 *
 * <p>使用 Spring Mail 与配置的 SMTP 服务发送纯文本邮件；主题取模板名称，正文取
 * 渲染后的内容。SMTP 未配置时渠道自动禁用，地址缺失视为不可重试失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class MailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final ObjectProvider<MailProperties> mailPropertiesProvider;

    private final NotificationProperties notificationProperties;

    @SuppressWarnings("all")
    public MailNotificationChannel(final ObjectProvider<JavaMailSender> mailSenderProvider,
                                   final ObjectProvider<MailProperties> mailPropertiesProvider,
                                   final NotificationProperties notificationProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailPropertiesProvider = mailPropertiesProvider;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public NotificationChannelType channelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        MailProperties mailProperties = mailPropertiesProvider.getIfAvailable();
        return notificationProperties.getEmail().isEnabled()
                && mailProperties != null
                && !isBlank(mailProperties.getHost())
                && mailSenderProvider.getIfAvailable() != null;
    }

    @Override
    public NotificationSendResult send(final NotificationDelivery delivery) {
        if (isBlank(delivery.getRecipientAddress())) {
            throw new NotificationDeliveryException("邮件收件地址为空", false);
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        MailProperties mailProperties = mailPropertiesProvider.getIfAvailable();
        if (mailSender == null || mailProperties == null) {
            throw new NotificationDeliveryException("邮件渠道未配置", false);
        }
        String from = notificationProperties.getEmail().getFrom();
        if (isBlank(from)) {
            from = mailProperties.getUsername();
        }
        if (isBlank(from)) {
            throw new NotificationDeliveryException("邮件发件人未配置", false);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(delivery.getRecipientAddress());
            helper.setSubject(delivery.getTitle());
            helper.setText(delivery.getContent(), false);
            mailSender.send(message);
            return NotificationSendResult.none();
        } catch (Exception e) {
            throw new NotificationDeliveryException(NotificationSanitizer.sanitizeError(e.getMessage()), true);
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
