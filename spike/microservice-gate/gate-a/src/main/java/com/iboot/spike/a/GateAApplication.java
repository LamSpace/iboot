package com.iboot.spike.a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 门禁服务 A。
 *
 * <p>启动后以 {@code spike-a} 之名注册到 Nacos，按服务名调用服务 B；
 * {@code /call-b} 资源由 Sentinel 保护，用于验证限流规则生效。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class GateAApplication {

    /**
     * 启动应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GateAApplication.class, args);
    }
}
