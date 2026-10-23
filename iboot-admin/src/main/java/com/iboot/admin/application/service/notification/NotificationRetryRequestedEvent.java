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
 * 人工重试请求事件。
 *
 * <p>在重试事务内发布，由监听器在事务提交后立即投递指定记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class NotificationRetryRequestedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /**
     * 投递记录 ID。
     */
    private final Long deliveryId;

    /**
     * 创建人工重试请求事件。
     *
     * @param source     事件源
     * @param deliveryId 投递记录 ID
     */
    public NotificationRetryRequestedEvent(final Object source, final Long deliveryId) {
        super(source);
        this.deliveryId = deliveryId;
    }

    /**
     * 投递记录 ID。
     *
     * @return 记录 ID
     */
    public Long getDeliveryId() {
        return deliveryId;
    }
}
