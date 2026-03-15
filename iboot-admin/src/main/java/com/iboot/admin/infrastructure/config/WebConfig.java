package com.iboot.admin.infrastructure.config;

import com.iboot.admin.infrastructure.interceptor.MonitoringInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 注册监控拦截器和其他Web相关配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MonitoringInterceptor monitoringInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册监控拦截器
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/druid/**",
                        "/sba/**"
                );
    }
}