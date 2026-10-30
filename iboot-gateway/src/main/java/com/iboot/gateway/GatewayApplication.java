package com.iboot.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iBoot 网关。
 *
 * <p>阶段 2 的统一入口：前端只与本服务交互，后端接口在服务之间迁移时只改路由表。
 * 本服务不承载业务逻辑，也不做鉴权决策——鉴权在认证中心交付后前置到这里。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 启动网关。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
