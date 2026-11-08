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

package com.iboot.workflow.domain.notification.model;

/**
 * 通知 outbox 事件状态。
 *
 * <p>状态流转：{@code PENDING -> SENDING -> SENT}；投递失败回到 {@code PENDING}，
 * 超过最大尝试次数进入 {@code DEAD}。{@code SENDING} 是短租约状态，中继崩溃后由
 * 超时重置逻辑恢复为 {@code PENDING}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum NotificationOutboxStatus {

    /**
     * 待投递。
     */
    PENDING,

    /**
     * 已被中继抢占，正在投递。
     */
    SENDING,

    /**
     * 已成功投递到 broker。
     */
    SENT,

    /**
     * 超过最大尝试次数，等待人工处理。
     */
    DEAD
}
