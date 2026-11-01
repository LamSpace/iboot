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

/**
 * 通知登记端口。
 *
 * <p>业务主流程只依赖该端口，不感知通知能力部署在哪个服务。阶段 2 之后由
 * {@code iboot-notify} 承接，实现通过跨服务 HTTP 调用完成登记。
 *
 * <p>实现 MUST 在业务事务提交后执行登记，且 MUST NOT 因通知失败向调用方抛出异常——
 * 通知失败不得阻断审批等主流程，失败由实现自行记录与重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface NotificationPort {

    /**
     * 登记一次通知事件。
     *
     * @param command 通知登记命令
     */
    void enqueue(NotificationCommand command);
}
