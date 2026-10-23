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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NotificationSanitizer} 的脱敏规则测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationSanitizerTest {

    /**
     * 验证邮箱保留首字符与域名。
     */
    @Test
    void masksEmail() {
        assertThat(NotificationSanitizer.maskEmail("alice@example.com")).isEqualTo("a***@example.com");
        assertThat(NotificationSanitizer.maskEmail("a@b.com")).isEqualTo("a***@b.com");
        assertThat(NotificationSanitizer.maskEmail(null)).isNull();
    }

    /**
     * 验证手机号保留前三位与后四位。
     */
    @Test
    void masksPhone() {
        assertThat(NotificationSanitizer.maskPhone("13800138000")).isEqualTo("138****8000");
        assertThat(NotificationSanitizer.maskPhone("12345")).isEqualTo("*****");
    }

    /**
     * 验证错误摘要会移除 Webhook、Secret 与 AccessKey。
     */
    @Test
    void sanitizesSecretsFromError() {
        String raw = "failed url=https://oapi.dingtalk.com/robot/send?access_token=abc123 "
                + "secret=SECxyz AccessKeyId=LTAIxxx";

        String sanitized = NotificationSanitizer.sanitizeError(raw);

        assertThat(sanitized).doesNotContain("abc123").doesNotContain("SECxyz").doesNotContain("LTAIxxx");
        assertThat(sanitized).contains("***");
    }

    /**
     * 验证超长错误被截断。
     */
    @Test
    void truncatesLongError() {
        String sanitized = NotificationSanitizer.sanitizeError("x".repeat(2000));

        assertThat(sanitized).hasSizeLessThanOrEqualTo(NotificationSanitizer.MAX_ERROR_LENGTH);
    }
}
