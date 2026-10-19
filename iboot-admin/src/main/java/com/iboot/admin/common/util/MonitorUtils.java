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

package com.iboot.admin.common.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer 监控指标工具。
 *
 * <p>为 API 请求计数和耗时提供便捷方法，内部按 URI、HTTP 方法和结果缓存
 * {@link Counter} / {@link Timer}，避免重复注册。{@link MeterRegistry} 为可选依赖，
 * 未注入时所有记录方法静默跳过。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class MonitorUtils {

    /**
     * Micrometer 注册表；未启用监控时为 {@code null}。
     */
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * 按指标键缓存的请求计数器。
     */
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    /**
     * 按指标键缓存的请求耗时计时器。
     */
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    /**
     * 增加 API 请求计数。
     *
     * @param uri 请求 URI
     * @param method HTTP 方法
     * @param outcome 请求结果标签，例如 success、failure
     */
    public void incrementRequest(String uri, String method, String outcome) {
        if (meterRegistry != null) {
            String key = "api.requests.total:" + uri + ":" + method + ":" + outcome;
            Counter counter = counters.computeIfAbsent(key, k ->
                    Counter.builder("api.requests.total")
                            .description("Total number of API requests")
                            .tag("uri", uri)
                            .tag("method", method)
                            .tag("outcome", outcome)
                            .register(meterRegistry));
            counter.increment();
        }
    }

    /**
     * 记录 API 请求耗时。
     *
     * @param uri 请求 URI
     * @param method HTTP 方法
     * @param duration 请求耗时
     * @param unit 耗时单位
     */
    public void recordRequestTime(String uri, String method, long duration, TimeUnit unit) {
        if (meterRegistry != null) {
            String key = "api.request.duration:" + uri + ":" + method;
            Timer timer = timers.computeIfAbsent(key, k ->
                    Timer.builder("api.request.duration")
                            .description("Duration of API requests")
                            .tag("uri", uri)
                            .tag("method", method)
                            .register(meterRegistry));
            timer.record(duration, unit);
        }
    }

    /**
     * 以秒为单位记录 API 请求耗时。
     *
     * @param uri 请求 URI
     * @param method HTTP 方法
     * @param durationSeconds 请求耗时，单位秒
     */
    public void recordRequestTimeSeconds(String uri, String method, double durationSeconds) {
        if (meterRegistry != null) {
            recordRequestTime(uri, method, (long) (durationSeconds * 1000), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 获取 Micrometer 注册表。
     *
     * @return 当前 {@link MeterRegistry}；未启用监控时为 {@code null}
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

}
