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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MailNotificationChannel} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class MailNotificationChannelTest {

    private JavaMailSender mailSender;

    private MailProperties mailProperties;

    private NotificationProperties properties;

    private MailNotificationChannel channel;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mailProperties = new MailProperties();
        mailProperties.setHost("smtp.qq.com");
        mailProperties.setUsername("sender@qq.com");
        properties = new NotificationProperties();
        properties.getEmail().setFrom("iBoot <sender@qq.com>");
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> senderProvider = mock(ObjectProvider.class);
        when(senderProvider.getIfAvailable()).thenReturn(mailSender);
        @SuppressWarnings("unchecked")
        ObjectProvider<MailProperties> propertiesProvider = mock(ObjectProvider.class);
        when(propertiesProvider.getIfAvailable()).thenReturn(mailProperties);
        channel = new MailNotificationChannel(senderProvider, propertiesProvider, properties);
    }

    /**
     * 验证邮件主题、正文、收件人与发件人。
     */
    @Test
    void sendsMailWithTemplateContent() throws Exception {
        channel.send(delivery());

        verify(mailSender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("审批待办通知");
        assertThat(mimeMessage.getAllRecipients()).singleElement()
                .extracting(Object::toString)
                .isEqualTo("user@example.com");
        assertThat(mimeMessage.getFrom()).singleElement()
                .extracting(Object::toString)
                .isEqualTo("iBoot <sender@qq.com>");
        assertThat(mimeMessage.getContent()).isEqualTo("您有一条新的审批待办");
    }

    /**
     * 验证收件地址为空时不可重试失败。
     */
    @Test
    void rejectsMissingAddress() {
        NotificationDelivery delivery = delivery();
        delivery.setRecipientAddress(null);

        assertThatThrownBy(() -> channel.send(delivery))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isFalse());
    }

    /**
     * 验证 SMTP 未配置时渠道不可用。
     */
    @Test
    void disabledWithoutHost() {
        mailProperties.setHost(null);

        assertThat(channel.isEnabled()).isFalse();
    }

    /**
     * 验证发送异常按可重试失败抛出。
     */
    @Test
    void wrapsMailFailureAsRetryable() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> channel.send(delivery()))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(error -> assertThat(((NotificationDeliveryException) error).isRetryable()).isTrue());
    }

    private NotificationDelivery delivery() {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setEventType(NotificationEventType.APPROVAL_TODO);
        delivery.setChannel(NotificationChannelType.EMAIL);
        delivery.setRecipientId(2L);
        delivery.setRecipientAddress("user@example.com");
        delivery.setTitle("审批待办通知");
        delivery.setContent("您有一条新的审批待办");
        return delivery;
    }
}
