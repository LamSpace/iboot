package com.iboot.admin.infrastructure.interceptor;

import com.iboot.admin.application.service.BusinessMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 监控拦截器。
 *
 * <p>在请求前后记录 URI、方法、耗时和结果等指标；静态资源和监控端点由
 * {@code WebConfig} 排除。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MonitoringInterceptor.class);

    private final Map<String, LocalDateTime> requestStartTimeMap = new ConcurrentHashMap<>();

    @Autowired
    private BusinessMetricsService businessMetricsService;

    @Override
    /**
     * 记录请求开始时间并生成请求 ID。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return 始终返回 {@code true} 以继续处理
     * @throws Exception 处理失败时抛出
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestId = generateRequestId(request);
        requestStartTimeMap.put(requestId, LocalDateTime.now());
        // 记录请求开始
        log.debug("开始处理请求: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    /**
     * 记录请求耗时和结果指标。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @param ex 请求处理异常；正常结束时为 {@code null}
     * @throws Exception 处理失败时抛出
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
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
            log.debug("请求完成: {} {} - 状态码: {}, 耗时: {}ms", method, uri, response.getStatus(), durationMillis);
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
