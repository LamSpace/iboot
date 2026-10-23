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

package com.iboot.admin.domain.notification.service;

import com.iboot.admin.domain.notification.model.NotificationChannelType;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.domain.notification.model.NotificationSendResult;

/**
 * 通知渠道端口。
 *
 * <p>站内、邮件、钉钉、短信分别实现该接口。编排服务只依赖端口，按
 * {@link #channelType()} 选择和启停渠道，不感知具体提供方。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationChannel {

    /**
     * 渠道类型。
     *
     * @return 渠道类型
     */
    NotificationChannelType channelType();

    /**
     * 当前配置下渠道是否可用。
     *
     * @return 可用返回 {@code true}
     */
    boolean isEnabled();

    /**
     * 发送一条投递。
     *
     * <p>成功时返回发送结果；可重试失败抛出 {@link NotificationDeliveryException}，
     * 不可重试失败将其 {@code retryable} 设为 {@code false}。实现不得吞掉异常。
     *
     * @param delivery 投递记录
     * @return 发送结果
     */
    NotificationSendResult send(NotificationDelivery delivery);
}
