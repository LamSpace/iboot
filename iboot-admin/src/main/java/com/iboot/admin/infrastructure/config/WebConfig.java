package com.iboot.admin.infrastructure.config;

import com.iboot.admin.infrastructure.interceptor.MonitoringInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>注册监控拦截器，并排除 Actuator、Swagger、Druid、Spring Boot Admin 等
 * 非业务路径，避免监控埋点重复统计。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MonitoringInterceptor monitoringInterceptor;

    /**
     * 注册监控拦截器。
     *
     * <p>拦截所有路径，排除 Actuator、Swagger、Druid 和 Spring Boot Admin 端点。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册监控拦截器
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**",
                        "/favicon.ico", "/druid/**", "/sba/**");
    }

}