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

package com.iboot.admin.application.service.notification;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 通知投递监听器。
 *
 * <p>监听登记与人工重试事件，在事务提交后异步触发发送，避免外部 I/O 拖长业务事务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class NotificationDispatchListener {

    private final NotificationApplicationService notificationApplicationService;

    @SuppressWarnings("all")
    public NotificationDispatchListener(final NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 事务提交后投递新登记的事件。
     *
     * @param event 通知已登记事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(final NotificationQueuedEvent event) {
        notificationApplicationService.dispatchEvent(event.getEventId());
    }

    /**
     * 投递人工重试的记录。
     *
     * <p>人工重试只做一次条件更新，不依赖业务事务；使用普通事件 + 异步执行，
     * 确保重置完成后立即投递。
     *
     * @param event 人工重试请求事件
     */
    @Async
    @EventListener
    public void onRetryRequested(final NotificationRetryRequestedEvent event) {
        notificationApplicationService.dispatch(event.getDeliveryId());
    }
}
