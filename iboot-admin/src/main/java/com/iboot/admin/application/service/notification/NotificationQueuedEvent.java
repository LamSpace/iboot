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

import org.springframework.context.ApplicationEvent;

/**
 * 通知已登记事件。
 *
 * <p>在业务事务内发布，由监听器在事务提交后触发一次即时投递。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationQueuedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 业务事件 ID。
     */
    private final String eventId;

    /**
     * 创建通知已登记事件。
     *
     * @param source  事件源
     * @param eventId 业务事件 ID
     */
    public NotificationQueuedEvent(final Object source, final String eventId) {
        super(source);
        this.eventId = eventId;
    }

    /**
     * 业务事件 ID。
     *
     * @return 事件 ID
     */
    public String getEventId() {
        return eventId;
    }
}
