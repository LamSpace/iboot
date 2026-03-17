package com.iboot.admin.infrastructure.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // 这里可以添加具体的健康检查逻辑
        // 例如：检查数据库连接、外部服务可用性等

        // 示例：模拟检查通过
        boolean isHealthy = true;

        if (isHealthy) {
            return Health.up()
                    .withDetail("status", "iBoot Admin System is running smoothly")
                    .withDetail("version", "1.0.0")
                    .withDetail("author", "iBoot Team")
                    .build();
        } else {
            return Health.down().withDetail("status", "iBoot Admin System has encountered issues").build();
        }
    }

}