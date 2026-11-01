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

package com.iboot.notify.infrastructure.push;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * SSE 推送指标。
 *
 * <p>只覆盖推送链路的三项指标，指标名沿用管理端既有定义，`application` 标签改为
 * {@code iboot-notify}（与认证服务迁出时同样的处理）。后续若其他指标也随域迁入，再扩展本类。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class PushMetrics {

    /**
     * 指标归属的服务名，用于 Grafana 按服务区分。
     */
    private static final String APPLICATION_TAG = "iboot-notify";

    private final AtomicInteger connectionCount = new AtomicInteger(0);

    private final Counter eventSentCounter;

    private final Counter eventFailedCounter;

    /**
     * 创建并注册推送指标。
     *
     * @param meterRegistry 指标注册表
     */
    public PushMetrics(final MeterRegistry meterRegistry) {
        Gauge.builder("push_connection_active", connectionCount, AtomicInteger::get)
                .description("当前活跃的 SSE 推送连接数")
                .tag("application", APPLICATION_TAG)
                .baseUnit(BaseUnits.CONNECTIONS)
                .register(meterRegistry);
        this.eventSentCounter = Counter.builder("push_event_sent")
                .description("推送事件发送成功次数")
                .tag("application", APPLICATION_TAG)
                .register(meterRegistry);
        this.eventFailedCounter = Counter.builder("push_event_failed")
                .description("推送事件发送失败次数")
                .tag("application", APPLICATION_TAG)
                .register(meterRegistry);
    }

    /**
     * 更新当前连接数。
     *
     * @param count 当前连接数
     */
    public void updatePushConnectionCount(final int count) {
        connectionCount.set(count);
    }

    /**
     * 记录一次推送成功。
     */
    public void recordPushEventSent() {
        eventSentCounter.increment();
    }

    /**
     * 记录一次推送失败。
     */
    public void recordPushEventFailed() {
        eventFailedCounter.increment();
    }
}
