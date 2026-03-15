package com.iboot.admin.infrastructure.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * 自定义 Meter Registry，为所有指标添加公共标签
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configureMeterRegistry() {
        return registry -> registry.config()
                .commonTags("application", "iboot-admin", "namespace", "default");
    }

    /**
     * 启用 @Timed 注解支持，用于方法级别的性能监控
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}