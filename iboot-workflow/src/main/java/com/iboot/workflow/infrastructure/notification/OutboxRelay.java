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

package com.iboot.workflow.infrastructure.notification;

import com.iboot.workflow.application.service.notification.NotificationMessagePublisher;
import com.iboot.workflow.domain.notification.model.NotificationOutboxEvent;
import com.iboot.workflow.domain.notification.model.NotificationOutboxStatus;
import com.iboot.workflow.domain.notification.repository.NotificationOutboxRepository;
import com.iboot.workflow.infrastructure.config.OutboxProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通知 outbox 中继。
 *
 * <p>以虚拟线程按固定间隔轮询 outbox：短事务内抢占待发事件，事务外投递到 RocketMQ，
 * 成功标记已发送，失败按指数退避重排或标记死信。broker 不可用时只影响中继自身的重试，
 * 不影响审批线程；多实例通过 {@code FOR UPDATE SKIP LOCKED} 避免重复发布。
 *
 * <p>该组件是基础设施轮询器，不接入 Quartz，也不承载业务定时任务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class OutboxRelay implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    /**
     * 清理已发送记录的固定间隔。
     */
    private static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);

    /**
     * 退避时间的上限。
     */
    private static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private final NotificationOutboxRepository repository;

    private final NotificationMessagePublisher publisher;

    private final OutboxProperties properties;

    private final Clock clock;

    private final Counter sentCounter;

    private final Counter retryCounter;

    private final Counter deadCounter;

    private final AtomicLong pendingGauge = new AtomicLong();

    private final AtomicLong deadGauge = new AtomicLong();

    private volatile boolean running;

    private Thread worker;

    private LocalDateTime nextCleanup;

    @Autowired
    @SuppressWarnings("all")
    public OutboxRelay(final NotificationOutboxRepository repository,
                       final NotificationMessagePublisher publisher,
                       final OutboxProperties properties,
                       final MeterRegistry meterRegistry) {
        this(repository, publisher, properties, meterRegistry, Clock.systemDefaultZone());
    }

    OutboxRelay(final NotificationOutboxRepository repository,
                final NotificationMessagePublisher publisher,
                final OutboxProperties properties,
                final MeterRegistry meterRegistry,
                final Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
        this.sentCounter = Counter.builder("notification_outbox_sent_total")
                .description("通知 outbox 成功投递的事件数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("notification_outbox_retry_total")
                .description("通知 outbox 投递失败并重新排队的事件数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        this.deadCounter = Counter.builder("notification_outbox_dead_total")
                .description("通知 outbox 超过重试上限的事件数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        Gauge.builder("notification_outbox_pending", pendingGauge, AtomicLong::doubleValue)
                .description("通知 outbox 待投递事件数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        Gauge.builder("notification_outbox_dead", deadGauge, AtomicLong::doubleValue)
                .description("通知 outbox 死信事件数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        this.nextCleanup = LocalDateTime.now(clock).plus(CLEANUP_INTERVAL);
    }

    /**
     * 执行一轮中继：重置超租约记录、抢占并投递、刷新指标、按需清理。
     */
    void relayOnce() {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.resetStaleSending(now.minus(properties.getLease()), now);
        List<NotificationOutboxEvent> events = repository.claimBatch(properties.getBatchSize(), now);
        for (NotificationOutboxEvent event : events) {
            deliver(event, now);
        }
        refreshGauges();
        if (!now.isBefore(nextCleanup)) {
            cleanup(now);
            nextCleanup = now.plus(CLEANUP_INTERVAL);
        }
    }

    /**
     * 删除保留期之前的已发送记录。
     *
     * @param now 当前时间
     */
    void cleanup(LocalDateTime now) {
        int deleted = repository.deleteSentBefore(now.minus(properties.getRetention()));
        if (deleted > 0) {
            log.info("清理已发送通知事件，数量: {}", deleted);
        }
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        running = true;
        worker = Thread.ofVirtual().name("notification-outbox-relay").start(this::loop);
        log.info("通知 outbox 中继已启动，batchSize: {}", properties.getBatchSize());
    }

    @Override
    public void stop() {
        running = false;
        Thread current = worker;
        if (current != null) {
            current.interrupt();
            try {
                current.join(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("通知 outbox 中继已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        while (running) {
            try {
                relayOnce();
            } catch (RuntimeException e) {
                log.error("通知 outbox 中继执行失败", e);
            }
            try {
                Thread.sleep(properties.getPollInterval().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void deliver(final NotificationOutboxEvent event, final LocalDateTime now) {
        try {
            String messageId = publisher.publish(event);
            repository.markSent(event.getId(), messageId, now);
            sentCounter.increment();
            log.info("通知事件已投递，eventId: {}, messageId: {}", event.getEventId(), messageId);
        } catch (RuntimeException e) {
            int attempts = event.getAttempts() + 1;
            String error = e.getMessage();
            if (attempts >= properties.getMaxAttempts()) {
                repository.markDead(event.getId(), attempts, error, now);
                deadCounter.increment();
                log.error("通知事件进入死信，eventId: {}, attempts: {}", event.getEventId(), attempts, e);
                return;
            }
            Duration backoff = backoff(attempts);
            repository.markRetry(event.getId(), attempts, now.plus(backoff), error, now);
            retryCounter.increment();
            log.warn("通知事件投递失败，eventId: {}, attempts: {}, nextRetry: {}",
                    event.getEventId(), attempts, now.plus(backoff), e);
        }
    }

    private Duration backoff(final int attempts) {
        long multiplier = 1L << Math.min(Math.max(attempts - 1, 0), 10);
        Duration backoff = properties.getInitialBackoff().multipliedBy(multiplier);
        return backoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff;
    }

    private void refreshGauges() {
        pendingGauge.set(repository.countByStatus(NotificationOutboxStatus.PENDING));
        deadGauge.set(repository.countByStatus(NotificationOutboxStatus.DEAD));
    }
}
