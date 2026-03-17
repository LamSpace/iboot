package com.iboot.admin.infrastructure.filter;

import com.iboot.admin.common.util.MonitorUtils;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 监控过滤器，用于记录请求指标
 */
@Component
@Order(1)
public class MonitorFilter implements Filter {

    @Autowired
    private MonitorUtils monitorUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 执行请求
            chain.doFilter(request, response);
        } finally {
            // 记录请求结束时间
            long duration = System.currentTimeMillis() - startTime;

            // 记录请求指标
            String outcome = String.valueOf(httpResponse.getStatus()).startsWith("2") ? "SUCCESS" : "FAILURE";

            // 增加请求计数
            monitorUtils.incrementRequest(uri, method, outcome);

            // 记录请求耗时
            Timer.Sample sample = Timer.start(monitorUtils.getMeterRegistry());
            if (sample != null && monitorUtils.getMeterRegistry() != null) {
                sample.stop(Timer.builder("http.server.requests")
                        .tag("method", method)
                        .tag("uri", uri)
                        .tag("status", String.valueOf(httpResponse.getStatus()))
                        .tag("outcome", outcome)
                        .register(monitorUtils.getMeterRegistry()));
            }
        }
    }

}