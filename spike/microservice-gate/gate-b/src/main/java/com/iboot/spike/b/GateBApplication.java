package com.iboot.spike.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 门禁服务 B。
 *
 * <p>启动后以 {@code spike-b} 之名注册到 Nacos，并提供 {@code /hello} 供服务 A 调用；
 * {@code spike.message} 从配置中心读取，支持热更新。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class GateBApplication {

    /**
     * 启动应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GateBApplication.class, args);
    }
}
