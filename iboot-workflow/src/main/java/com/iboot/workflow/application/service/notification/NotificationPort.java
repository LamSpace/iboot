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

/**
 * 通知登记端口。
 *
 * <p>业务主流程只依赖该端口，不感知通知能力部署在哪个服务。当前由
 * {@code OutboxNotificationPort} 在业务事务内写入本地 outbox，事务提交后由中继
 * 异步投递到 {@code iboot-notify}。
 *
 * <p>实现 MUST 在调用方事务内持久化事件；持久化失败 MUST 向上抛出，使业务事务回滚。
 * 投递失败 MUST NOT 向调用方抛出——通知投递失败由中继重试，不得阻断审批等主流程。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationPort {

    /**
     * 在调用方事务内登记一次通知事件。
     *
     * @param command 通知登记命令
     * @throws RuntimeException 持久化失败时抛出，调用方事务应随之回滚
     */
    void enqueue(NotificationCommand command);
}
