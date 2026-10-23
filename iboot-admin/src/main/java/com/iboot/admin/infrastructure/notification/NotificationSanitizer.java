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

import java.util.regex.Pattern;

/**
 * 通知错误与地址脱敏工具。
 *
 * <p>投递记录会被管理员查看并长期保存，因此写入前必须移除 Webhook、Secret、
 * AccessKey 等凭据，并对邮箱、手机号做掩码。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class NotificationSanitizer {

    /**
     * 错误摘要最大长度，与数据库字段保持一致。
     */
    public static final int MAX_ERROR_LENGTH = 1000;

    private static final Pattern ACCESS_TOKEN = Pattern.compile("access_token=[^&\\s]+");

    private static final Pattern SECRET = Pattern.compile("(?i)(secret|accesskeysecret)=[^&\\s,;]+");

    private static final Pattern ACCESS_KEY_ID = Pattern.compile("(?i)(accesskeyid)=[^&\\s,;]+");

    private NotificationSanitizer() {
        // 工具类，禁止实例化
    }

    /**
     * 对邮箱做掩码，保留首字符与域名。
     *
     * @param email 邮箱地址
     * @return 掩码后的邮箱；输入为空时返回原值
     */
    public static String maskEmail(final String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 对手机号做掩码，保留前三位与后四位。
     *
     * @param phone 手机号
     * @return 掩码后的手机号；输入为空时返回原值
     */
    public static String maskPhone(final String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        if (phone.length() < 11) {
            return "*".repeat(phone.length());
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 清理错误摘要中的凭据并截断到最大长度。
     *
     * @param error 原始错误
     * @return 脱敏后的错误；输入为空时返回原值
     */
    public static String sanitizeError(final String error) {
        if (error == null) {
            return null;
        }
        String sanitized = ACCESS_TOKEN.matcher(error).replaceAll("access_token=***");
        sanitized = SECRET.matcher(sanitized).replaceAll("$1=***");
        sanitized = ACCESS_KEY_ID.matcher(sanitized).replaceAll("$1=***");
        return sanitized.length() <= MAX_ERROR_LENGTH ? sanitized : sanitized.substring(0, MAX_ERROR_LENGTH);
    }
}
