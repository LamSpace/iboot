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

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.iboot.workflow.application.service.notification.NotificationCommand;
import com.iboot.workflow.application.service.notification.NotificationPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 通知登记端口的 HTTP 适配器。
 *
 * <p>把登记命令投递到 {@code iboot-notify} 的内部端点。命令在业务事务提交后执行，
 * 调用线程使用虚拟线程；失败按指数退避重试，全部失败后只记录指标与结构化日志，
 * 不向业务调用方传播——审批提交不因通知不可用而失败。
 *
 * <p>投递过程由 Sentinel 资源 {@value #RESOURCE} 保护：下游持续失败时熔断打开，
 * 后续请求快速降级，不再等待每次重试耗尽；熔断窗口结束后自动半开探测。
 *
 * <p>退出条件：引入 RocketMQ 后替换为事件发布器，端口与业务代码不变。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class HttpNotificationPort implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(HttpNotificationPort.class);

    /**
     * 内部端点的共享密钥请求头。
     */
    private static final String TOKEN_HEADER = "X-Internal-Token";

    /**
     * Sentinel 治理资源名。
     */
    static final String RESOURCE = "iboot-workflow:notify-enqueue";

    /**
     * 单次登记的最大尝试次数。
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 首次重试的退避时间，之后逐次翻倍。
     */
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);

    private final RestClient restClient;

    private final String endpoint;

    private final String internalToken;

    private final Counter failedCounter;

    private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 创建适配器。
     *
     * @param restClientBuilder 按 Nacos 服务名寻址的 REST 客户端构建器
     * @param baseUrl 通知服务地址，默认使用注册中心服务名
     * @param internalToken 内部端点共享密钥
     * @param meterRegistry 指标注册表
     */
    public HttpNotificationPort(@Qualifier("loadBalancedRestClientBuilder") final RestClient.Builder restClientBuilder,
                                @Value("${iboot.notification.base-url:http://iboot-notify}") final String baseUrl,
                                @Value("${iboot.notification.internal-token:}") final String internalToken,
                                final MeterRegistry meterRegistry) {
        this.restClient = restClientBuilder.build();
        this.endpoint = baseUrl + "/internal/notifications";
        this.internalToken = internalToken;
        this.failedCounter = Counter.builder("notification_trigger_failed")
                .description("通知登记在重试耗尽后仍然失败的次数")
                .tag("application", "iboot-workflow")
                .register(meterRegistry);
        if (internalToken == null || internalToken.isBlank()) {
            log.warn("iboot.notification.internal-token 未配置，通知登记将全部失败关闭");
        }
    }

    /**
     * 在事务提交后异步投递登记命令。
     *
     * <p>无事务时立即异步执行。任何失败（含熔断阻塞）都不向调用方抛出。
     *
     * @param command 通知登记命令
     */
    @Override
    public void enqueue(final NotificationCommand command) {
        Runnable task = () -> send(command);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }

    private void send(final NotificationCommand command) {
        Entry entry = null;
        try {
            entry = SphU.entry(RESOURCE);
            deliverWithRetry(command);
        } catch (BlockException e) {
            failedCounter.increment();
            log.warn("通知登记被熔断，eventId: {}, reason: {}",
                    command.getEventId(), e.getClass().getSimpleName());
        } catch (RuntimeException e) {
            // 把异常计入 Sentinel 统计，异常比例熔断规则才能据此打开
            Tracer.trace(e);
            failedCounter.increment();
            log.error("通知登记失败，重试已耗尽，eventId: {}, template: {}, recipient: {}",
                    command.getEventId(), command.getTemplateCode(), command.getRecipientId(), e);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    private void deliverWithRetry(final NotificationCommand command) {
        Duration backoff = INITIAL_BACKOFF;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(endpoint)
                        .header(TOKEN_HEADER, internalToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(command)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RuntimeException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("通知登记失败，准备重试，eventId: {}, attempt: {}, reason: {}",
                        command.getEventId(), attempt, e.getMessage());
                sleep(backoff);
                backoff = backoff.multipliedBy(2);
            }
        }
    }

    private void sleep(final Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
