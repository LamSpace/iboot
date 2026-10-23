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
 * 通知投递状态。
 *
 * <p>描述一条投递记录从登记到成功、失败、跳过或终止的生命周期。终态不再自动重试；
 * {@link #FAILED} 与 {@link #DEAD} 允许管理员人工重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum NotificationDeliveryStatus {

    /**
     * 已登记，等待首次或下一次尝试。
     */
    PENDING,

    /**
     * 已被某个执行者领取，正在发送。
     */
    SENDING,

    /**
     * 发送成功。
     */
    SUCCESS,

    /**
     * 本次失败，仍有自动重试机会。
     */
    FAILED,

    /**
     * 达到最大尝试次数，等待人工处理。
     */
    DEAD,

    /**
     * 接收人缺少地址或渠道已关闭，不重试。
     */
    SKIPPED;

    /**
     * 是否为终态。
     *
     * @return 成功、终止或跳过返回 {@code true}
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == DEAD || this == SKIPPED;
    }

    /**
     * 是否允许管理员人工重试。
     *
     * @return 失败或终止返回 {@code true}
     */
    public boolean canRetryManually() {
        return this == FAILED || this == DEAD;
    }
}
