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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通知枚举的转换与状态判断测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationEnumsTest {

    /**
     * 验证渠道类型可从字符串转换且忽略大小写。
     */
    @Test
    void channelTypeParsesCaseInsensitively() {
        assertThat(NotificationChannelType.from("email")).isEqualTo(NotificationChannelType.EMAIL);
        assertThat(NotificationChannelType.from("DINGTALK")).isEqualTo(NotificationChannelType.DINGTALK);
    }

    /**
     * 验证未知渠道类型抛出明确异常。
     */
    @Test
    void channelTypeRejectsUnknownValue() {
        assertThatThrownBy(() -> NotificationChannelType.from("PIGEON"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIGEON");
    }

    /**
     * 验证终态与可人工重试的判断。
     */
    @Test
    void statusKnowsTerminalAndManualRetryStates() {
        assertThat(NotificationDeliveryStatus.SUCCESS.isTerminal()).isTrue();
        assertThat(NotificationDeliveryStatus.DEAD.isTerminal()).isTrue();
        assertThat(NotificationDeliveryStatus.SKIPPED.isTerminal()).isTrue();
        assertThat(NotificationDeliveryStatus.FAILED.isTerminal()).isFalse();
        assertThat(NotificationDeliveryStatus.PENDING.isTerminal()).isFalse();
        assertThat(NotificationDeliveryStatus.SENDING.isTerminal()).isFalse();

        assertThat(NotificationDeliveryStatus.FAILED.canRetryManually()).isTrue();
        assertThat(NotificationDeliveryStatus.DEAD.canRetryManually()).isTrue();
        assertThat(NotificationDeliveryStatus.SUCCESS.canRetryManually()).isFalse();
    }
}
