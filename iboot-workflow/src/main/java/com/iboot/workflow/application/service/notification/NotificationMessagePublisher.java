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

package com.iboot.workflow.application.service.notification;

import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;

/**
 * 通知事件消息发布端口。
 *
 * <p>由 outbox 中继调用，把一条已持久化的事件投递到消息中间件。实现 MUST 在失败时
 * 抛出异常，由中继决定退避重试或标记死信；实现 MUST NOT 在构造期强依赖 broker 可用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationMessagePublisher {

    /**
     * 发布一条通知事件。
     *
     * @param event 待发布事件
     * @return broker 返回的消息 ID
     * @throws RuntimeException 发布失败时抛出
     */
    String publish(NotificationOutboxEvent event);
}
