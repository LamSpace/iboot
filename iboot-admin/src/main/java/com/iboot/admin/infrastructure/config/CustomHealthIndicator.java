package com.iboot.admin.infrastructure.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查指示器。
 *
 * <p>向 Actuator 健康端点提供应用自身状态、版本等摘要信息。当前实现只返回
 * 固定的 UP 状态，后续可在 {@link #health()} 中加入数据库或外部服务探测。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    /**
     * 返回应用健康状态。
     *
     * @return 包含状态、版本等详情的健康检查结果
     */
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