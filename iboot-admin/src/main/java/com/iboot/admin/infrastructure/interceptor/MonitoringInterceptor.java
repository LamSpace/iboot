package com.iboot.admin.infrastructure.interceptor;

import com.iboot.admin.application.service.BusinessMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控拦截器
 * 用于收集HTTP请求相关的监控指标
 */
@Slf4j
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    @Autowired
    private BusinessMetricsService businessMetricsService;

    private final Map<String, LocalDateTime> requestStartTimeMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = generateRequestId(request);
        requestStartTimeMap.put(requestId, LocalDateTime.now());
        
        // 记录请求开始
        log.debug("开始处理请求: {} {}", request.getMethod(), request.getRequestURI());
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestId = generateRequestId(request);
        LocalDateTime startTime = requestStartTimeMap.remove(requestId);
        
        if (startTime != null) {
            long durationMillis = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());
            
            // 记录请求耗时和结果
            String uri = request.getRequestURI();
            String method = request.getMethod();
            String outcome = determineOutcome(response.getStatus(), ex);
            
            // 使用现有的 MonitorUtils 记录指标
            // 注意：这里假设 MonitorUtils 已经被注入或可以通过其他方式访问
            
            log.debug("请求完成: {} {} - 状态码: {}, 耗时: {}ms", 
                     method, uri, response.getStatus(), durationMillis);
        }
    }

    /**
     * 生成请求唯一标识
     */
    private String generateRequestId(HttpServletRequest request) {
        return request.getRemoteAddr() + "-" + request.getSession().getId() + "-" + System.currentTimeMillis();
    }

    /**
     * 根据响应状态和异常确定请求结果
     */
    private String determineOutcome(int statusCode, Exception ex) {
        if (ex != null) {
            return "SERVER_ERROR";
        }
        
        if (statusCode >= 200 && statusCode < 300) {
            return "SUCCESS";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "CLIENT_ERROR";
        } else if (statusCode >= 500) {
            return "SERVER_ERROR";
        }
        
        return "UNKNOWN";
    }

    /**
     * 获取当前活跃请求数
     */
    public int getActiveRequestCount() {
        return requestStartTimeMap.size();
    }
}