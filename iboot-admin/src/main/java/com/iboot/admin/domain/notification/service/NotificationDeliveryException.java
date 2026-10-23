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

/**
 * 通知渠道发送异常。
 *
 * <p>携带是否可重试的标志。可重试失败进入指数退避，不可重试失败直接终止；
 * 消息应已脱敏，不得包含凭据或完整地址。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationDeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 是否允许重试。
     */
    private final boolean retryable;

    /**
     * 创建可重试异常。
     *
     * @param message 脱敏后的错误消息
     */
    public NotificationDeliveryException(final String message) {
        this(message, true);
    }

    /**
     * 创建发送异常。
     *
     * @param message   脱敏后的错误消息
     * @param retryable 是否允许重试
     */
    public NotificationDeliveryException(final String message, final boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    /**
     * 是否允许重试。
     *
     * @return 允许重试返回 {@code true}
     */
    public boolean isRetryable() {
        return retryable;
    }
}
