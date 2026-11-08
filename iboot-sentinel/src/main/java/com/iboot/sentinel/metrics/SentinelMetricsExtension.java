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

package com.iboot.sentinel.metrics;

import com.alibaba.csp.sentinel.metric.extension.MetricExtension;
import com.alibaba.csp.sentinel.metric.extension.MetricExtensionProvider;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sentinel 治理指标的 Micrometer 桥接。
 *
 * <p>把 Sentinel 的通过、阻塞、异常、响应时间与线程数写入 {@link MeterRegistry}，
 * 使治理指标可以经 Actuator 暴露给 Prometheus，而不依赖 Sentinel Dashboard 在线。
 *
 * <p>指标按资源名与服务标识打标签；资源名由调用方控制，必须保持有界，
 * 禁止把用户、租户或请求参数放入资源名。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class SentinelMetricsExtension implements MetricExtension {

    private static final String UNKNOWN = "unknown";

    private volatile MeterRegistry registry;

    private final String application;

    private final Map<String, Counter> passCounters = new ConcurrentHashMap<>();

    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();

    private final Map<String, Counter> blockCounters = new ConcurrentHashMap<>();

    private final Map<String, Counter> exceptionCounters = new ConcurrentHashMap<>();

    private final Map<String, Timer> responseTimers = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> threadCounts = new ConcurrentHashMap<>();

    /**
     * 创建指标扩展。
     *
     * @param registry    Micrometer 注册表
     * @param application 服务标识，写入指标的 application 标签
     */
    public SentinelMetricsExtension(final MeterRegistry registry, final String application) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.application = application == null || application.isBlank() ? UNKNOWN : application;
    }

    /**
     * 注册到 Sentinel 的全局指标扩展点。
     *
     * <p>同一服务标识只保留一个扩展；重复创建上下文时更新已有扩展的注册表并清空缓存，
     * 避免测试或开发热重启后指标重复计数。
     */
    public void register() {
        synchronized (SentinelMetricsExtension.class) {
            for (MetricExtension extension : MetricExtensionProvider.getMetricExtensions()) {
                if (extension instanceof SentinelMetricsExtension existing) {
                    if (existing == this) {
                        return;
                    }
                    if (existing.application.equals(this.application)) {
                        existing.updateRegistry(this.registry);
                        return;
                    }
                }
            }
            MetricExtensionProvider.addMetricExtension(this);
        }
    }

    private synchronized void updateRegistry(final MeterRegistry newRegistry) {
        this.registry = newRegistry;
        passCounters.clear();
        successCounters.clear();
        blockCounters.clear();
        exceptionCounters.clear();
        responseTimers.clear();
        threadCounts.clear();
    }

    /**
     * 记录通过数。
     *
     * @param resource 资源名
     * @param count    本次增量
     * @param args     调用参数，未使用
     */
    @Override
    public void addPass(final String resource, final int count, final Object... args) {
        if (count > 0) {
            passCounter(resource).increment(count);
        }
    }

    /**
     * 记录阻塞数，并按阻塞异常类型打规则标签。
     *
     * @param resource       资源名
     * @param count          本次增量
     * @param origin         调用来源，未使用
     * @param blockException 阻塞异常
     * @param args           调用参数，未使用
     */
    @Override
    public void addBlock(final String resource, final int count, final String origin,
                         final BlockException blockException, final Object... args) {
        if (count > 0) {
            blockCounter(resource, ruleType(blockException)).increment(count);
        }
    }

    /**
     * 记录成功数。
     *
     * @param resource 资源名
     * @param count    本次增量
     * @param args     调用参数，未使用
     */
    @Override
    public void addSuccess(final String resource, final int count, final Object... args) {
        if (count > 0) {
            successCounter(resource).increment(count);
        }
    }

    /**
     * 记录异常数。
     *
     * @param resource  资源名
     * @param count     本次增量
     * @param throwable 业务异常
     */
    @Override
    public void addException(final String resource, final int count, final Throwable throwable) {
        if (count > 0) {
            exceptionCounter(resource).increment(count);
        }
    }

    /**
     * 记录响应时间。
     *
     * @param resource 资源名
     * @param rt       响应时间，单位毫秒
     * @param args     调用参数，未使用
     */
    @Override
    public void addRt(final String resource, final long rt, final Object... args) {
        responseTimer(resource).record(Duration.ofMillis(Math.max(0, rt)));
    }

    /**
     * 增加资源当前线程数。
     *
     * @param resource 资源名
     * @param args     调用参数，未使用
     */
    @Override
    public void increaseThreadNum(final String resource, final Object... args) {
        threadCount(resource).incrementAndGet();
    }

    /**
     * 减少资源当前线程数。
     *
     * @param resource 资源名
     * @param args     调用参数，未使用
     */
    @Override
    public void decreaseThreadNum(final String resource, final Object... args) {
        threadCount(resource).decrementAndGet();
    }

    /**
     * 返回服务标识。
     *
     * @return application 标签值
     */
    public String application() {
        return application;
    }

    private Counter passCounter(final String resource) {
        String key = safeResource(resource);
        return passCounters.computeIfAbsent(key, name ->
                Counter.builder("sentinel.pass").tags(tags(name)).register(registry));
    }

    private Counter successCounter(final String resource) {
        String key = safeResource(resource);
        return successCounters.computeIfAbsent(key, name ->
                Counter.builder("sentinel.success").tags(tags(name)).register(registry));
    }

    private Counter blockCounter(final String resource, final String rule) {
        String name = safeResource(resource);
        String key = name + '|' + rule;
        return blockCounters.computeIfAbsent(key, ignored ->
                Counter.builder("sentinel.block").tags(tags(name).and("rule", rule)).register(registry));
    }

    private Counter exceptionCounter(final String resource) {
        String key = safeResource(resource);
        return exceptionCounters.computeIfAbsent(key, name ->
                Counter.builder("sentinel.exception").tags(tags(name)).register(registry));
    }

    private Timer responseTimer(final String resource) {
        String key = safeResource(resource);
        return responseTimers.computeIfAbsent(key, name ->
                Timer.builder("sentinel.rt").tags(tags(name)).register(registry));
    }

    private AtomicInteger threadCount(final String resource) {
        String key = safeResource(resource);
        return threadCounts.computeIfAbsent(key, name -> {
            AtomicInteger value = new AtomicInteger();
            Gauge.builder("sentinel.threads", value, AtomicInteger::get).tags(tags(name)).register(registry);
            return value;
        });
    }

    private Tags tags(final String resource) {
        return Tags.of("resource", safeResource(resource), "application", application);
    }

    private String safeResource(final String resource) {
        return resource == null || resource.isBlank() ? UNKNOWN : resource;
    }

    private String ruleType(final BlockException blockException) {
        if (blockException == null) {
            return "other";
        }
        return switch (blockException.getClass().getSimpleName()) {
            case "FlowException" -> "flow";
            case "DegradeException" -> "degrade";
            case "SystemBlockException" -> "system";
            case "AuthorityException" -> "authority";
            case "ParamFlowException" -> "param";
            default -> "other";
        };
    }
}
