package com.iboot.spike.b;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务 B 的演示端点。
 *
 * <p>{@code message} 来自配置中心（{@code spike-b.yml}），{@link RefreshScope} 使其在配置变更后
 * 无需重启即可生效——这是门禁 2.0-A「配置热更新」的观测点。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RefreshScope
public class HelloController {

    /** 来自配置中心的消息内容。 */
    @Value("${spike.message:local-default}")
    private String message;

    /**
     * JVM 启动时间。
     *
     * <p>必须取进程级时间而非 Bean 字段：本类带 {@link RefreshScope}，配置刷新时 Bean 会被重建，
     * 实例字段的时间戳会跟着变化，无法区分「热更新」与「重启」。
     */
    private static final long JVM_STARTED_AT = java.lang.management.ManagementFactory
            .getRuntimeMXBean().getStartTime();

    /**
     * 返回服务标识、配置中心下发的内容与启动时间。
     *
     * @return 演示数据
     */
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "spike-b");
        body.put("message", message);
        body.put("startedAt", JVM_STARTED_AT);
        return body;
    }
}
