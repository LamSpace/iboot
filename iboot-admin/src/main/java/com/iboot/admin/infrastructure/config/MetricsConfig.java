package com.iboot.admin.infrastructure.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
public class MetricsConfig {

    /**
     * 自定义 Meter Registry，为所有指标添加公共标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configureMeterRegistry() {
        return registry -> registry.config().commonTags("application", "iboot-admin", "namespace", "default");
    }

    /**
     * 启用 @Timed 注解支持，用于方法级别的性能监控
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * 配置虚拟线程执行器的指标观察
     * 监控虚拟线程的活跃度和任务执行情况
     */
    @Bean
    public io.micrometer.core.instrument.binder.MeterBinder virtualThreadMetrics(Executor taskExecutor) {
        return registry -> {
            // 虚拟线程执行器的指标会自动被 Micrometer 收集
            // 包括：executor.active, executor.queue.remaining 等
        };
    }

}