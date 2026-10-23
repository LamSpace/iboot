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

/**
 * 渠道发送结果。
 *
 * @param messageId 站内消息 ID；非站内渠道为 {@code null}
 * @author iBoot Team
 * @since 1.0.0
 */
public record NotificationSendResult(Long messageId) {

    /**
     * 非站内渠道的空结果。
     *
     * @return 不含消息 ID 的结果
     */
    public static NotificationSendResult none() {
        return new NotificationSendResult(null);
    }
}
