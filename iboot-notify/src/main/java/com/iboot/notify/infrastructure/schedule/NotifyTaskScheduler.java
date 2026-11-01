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

package com.iboot.notify.infrastructure.schedule;

import com.iboot.notify.infrastructure.notification.NotificationRetryTask;
import com.iboot.notify.infrastructure.search.ContentIndexReconcileTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 通知服务的内部维护任务调度。
 *
 * <p>通知投递重试与内容索引对账原先由管理端 Quartz 通过 {@code sys_job} 驱动，
 * 拆出本服务后改由进程内调度执行。
 *
 * <p>多实例一致性用「时间窗抢占」实现：每一轮对应一个窗口键
 * （{@code iboot:notify:task:retry:<窗口序号>} 等），只有第一个
 * {@code SET NX EX} 成功的实例执行本轮，其余实例跳过。窗口键不主动释放，
 * 由 TTL 自然过期；因此同一窗口至多执行一次，即使执行中途失败也不会被其他实例重复执行，
 * 失败由下一窗口重试。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see NotificationRetryTask
 * @see ContentIndexReconcileTask
 */
@Component
@EnableScheduling
public class NotifyTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotifyTaskScheduler.class);

    /**
     * 投递重试的窗口键前缀。
     */
    private static final String RETRY_KEY_PREFIX = "iboot:notify:task:retry:";

    /**
     * 内容索引对账的窗口键前缀。
     */
    private static final String RECONCILE_KEY_PREFIX = "iboot:notify:task:reconcile:";

    /**
     * 投递重试的窗口长度（秒）。
     */
    private static final long RETRY_WINDOW_SECONDS = 60;

    /**
     * 内容索引对账的窗口长度（秒）。
     */
    private static final long RECONCILE_WINDOW_SECONDS = 300;

    /**
     * 窗口键的存活时间：只需覆盖一个窗口，留足余量以吸收时钟抖动。
     */
    private static final Duration RETRY_KEY_TTL = Duration.ofMinutes(5);

    /**
     * 对账窗口键的存活时间。
     */
    private static final Duration RECONCILE_KEY_TTL = Duration.ofMinutes(30);

    private final NotificationRetryTask notificationRetryTask;

    private final ContentIndexReconcileTask contentIndexReconcileTask;

    private final StringRedisTemplate redisTemplate;

    /**
     * 创建调度器。
     *
     * @param notificationRetryTask 通知投递重试任务
     * @param contentIndexReconcileTask 内容索引对账任务
     * @param redisTemplate 字符串 Redis 模板，用于窗口抢占
     */
    public NotifyTaskScheduler(final NotificationRetryTask notificationRetryTask,
                               final ContentIndexReconcileTask contentIndexReconcileTask,
                               final StringRedisTemplate redisTemplate) {
        this.notificationRetryTask = notificationRetryTask;
        this.contentIndexReconcileTask = contentIndexReconcileTask;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每分钟扫描到期的通知投递并重试。
     */
    @Scheduled(fixedDelayString = "${iboot.notify.scheduling.retry-interval-ms:60000}",
            initialDelayString = "${iboot.notify.scheduling.retry-initial-delay-ms:60000}")
    public void retryDeliveries() {
        runInWindow(RETRY_KEY_PREFIX, RETRY_WINDOW_SECONDS, RETRY_KEY_TTL, "通知投递重试", notificationRetryTask::retry);
    }

    /**
     * 每五分钟对账内容索引。
     */
    @Scheduled(fixedDelayString = "${iboot.notify.scheduling.reconcile-interval-ms:300000}",
            initialDelayString = "${iboot.notify.scheduling.reconcile-initial-delay-ms:300000}")
    public void reconcileContentIndex() {
        runInWindow(RECONCILE_KEY_PREFIX, RECONCILE_WINDOW_SECONDS, RECONCILE_KEY_TTL, "内容索引对账",
                contentIndexReconcileTask::reconcile);
    }

    private void runInWindow(final String keyPrefix, final long windowSeconds, final Duration ttl,
                             final String taskName, final Runnable task) {
        long window = System.currentTimeMillis() / 1000 / windowSeconds;
        String key = keyPrefix + window;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, taskName, ttl);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("{}本轮已由其他实例执行，跳过，key: {}", taskName, key);
            return;
        }
        try {
            log.debug("{}开始执行，key: {}", taskName, key);
            task.run();
        } catch (RuntimeException e) {
            log.warn("{}执行失败，本窗口不再重试，key: {}", taskName, key, e);
        }
    }
}
