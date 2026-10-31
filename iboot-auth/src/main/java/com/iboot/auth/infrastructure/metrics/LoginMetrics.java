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

package com.iboot.auth.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录相关指标。
 *
 * <p>指标名与拆分前完全一致（{@code user_login_success}、{@code user_login_failed}、
 * {@code user_online_count}），Grafana 面板与告警规则的查询语句无需改动；只把
 * {@code application} 标签改为 {@code iboot-auth}，看板按指标名聚合，不按该标签过滤。
 *
 * <p>在线用户数是本实例的内存计数，服务重启后归零；精确的在线数仍以共享 Redis 中的会话键为准，
 * 由管理端的在线用户接口提供。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class LoginMetrics {

    private final Counter loginSuccessCounter;

    private final Counter loginFailedCounter;

    private final AtomicInteger onlineUserCount = new AtomicInteger(0);

    /**
     * 注册登录相关计数器与在线用户数指标。
     *
     * @param meterRegistry 指标注册表
     */
    public LoginMetrics(MeterRegistry meterRegistry) {
        this.loginSuccessCounter = Counter.builder("user_login_success")
                .description("用户登录成功次数")
                .tag("application", "iboot-auth")
                .register(meterRegistry);
        this.loginFailedCounter = Counter.builder("user_login_failed")
                .description("用户登录失败次数")
                .tag("application", "iboot-auth")
                .register(meterRegistry);
        Gauge.builder("user_online_count", onlineUserCount, AtomicInteger::get)
                .description("在线用户数量")
                .tag("application", "iboot-auth")
                .register(meterRegistry);
    }

    /**
     * 记录一次登录成功，同时递增在线用户数。
     */
    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
        onlineUserCount.incrementAndGet();
    }

    /**
     * 记录一次登录失败，不影响在线用户数。
     */
    public void recordLoginFailed() {
        loginFailedCounter.increment();
    }

    /**
     * 记录一次登出，递减在线用户数。
     */
    public void recordLogout() {
        onlineUserCount.decrementAndGet();
    }
}
