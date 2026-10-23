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

import java.util.Locale;

/**
 * 通知渠道类型。
 *
 * <p>与投递记录的 {@code channel} 字段一一对应，用于选择渠道实现并做管理端筛选。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum NotificationChannelType {

    /**
     * 站内消息（落库 + SSE 实时推送）。
     */
    IN_APP,

    /**
     * 邮件。
     */
    EMAIL,

    /**
     * 钉钉群机器人。
     */
    DINGTALK,

    /**
     * 阿里云短信。
     */
    SMS;

    /**
     * 从字符串解析渠道类型，忽略大小写。
     *
     * @param value 渠道标识
     * @return 渠道类型
     * @throws IllegalArgumentException 渠道标识为空或未知时抛出
     */
    public static NotificationChannelType from(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知的通知渠道: " + value, e);
        }
    }
}
