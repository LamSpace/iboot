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

package com.iboot.admin.infrastructure.notification;

import com.iboot.admin.application.service.notification.NotificationApplicationService;
import com.iboot.admin.domain.notification.repository.NotificationDeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知投递重试任务。
 *
 * <p>由 Quartz 每分钟调用：先把超过 5 分钟仍处于发送中的记录重置为失败，再批量投递
 * 到期记录。单条失败只记录日志，不影响本轮其他记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class NotificationRetryTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryTask.class);

    private static final int STALE_SENDING_MINUTES = 5;

    private final NotificationDeliveryRepository deliveryRepository;

    private final NotificationApplicationService notificationApplicationService;

    private final NotificationProperties properties;

    private final Clock clock;

    @SuppressWarnings("all")
    public NotificationRetryTask(final NotificationDeliveryRepository deliveryRepository,
                                 final NotificationApplicationService notificationApplicationService,
                                 final NotificationProperties properties, final Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.notificationApplicationService = notificationApplicationService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 执行一轮重试。
     */
    public void retry() {
        LocalDateTime now = LocalDateTime.now(clock);
        try {
            int reset = deliveryRepository.resetStaleSending(now.minusMinutes(STALE_SENDING_MINUTES), now);
            if (reset > 0) {
                log.warn("重置超时通知投递记录 {} 条", reset);
            }
        } catch (Exception e) {
            log.warn("重置超时通知投递记录失败，本轮继续", e);
        }
        List<Long> dueIds;
        try {
            dueIds = deliveryRepository.findDueIds(now, properties.getRetry().normalizedBatchSize());
        } catch (Exception e) {
            log.warn("查询到期通知投递记录失败", e);
            return;
        }
        for (Long id : dueIds) {
            try {
                notificationApplicationService.dispatch(id);
            } catch (Exception e) {
                log.warn("通知投递重试失败，deliveryId: {}", id, e);
            }
        }
    }
}
