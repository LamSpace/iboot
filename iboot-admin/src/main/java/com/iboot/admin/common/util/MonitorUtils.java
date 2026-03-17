package com.iboot.admin.common.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 监控工具类
 */
@Component
public class MonitorUtils {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private Counter requestCounter;

    private Timer requestTimer;

    @PostConstruct
    public void init() {
        if (meterRegistry != null) {
            requestCounter = Counter.builder("api.requests.total")
                    .description("Total number of API requests")
                    .register(meterRegistry);

            requestTimer = Timer.builder("api.request.duration")
                    .description("Duration of API requests")
                    .register(meterRegistry);
        }
    }

    /**
     * 增加API请求计数
     *
     * @param uri     请求URI
     * @param method  HTTP方法
     * @param outcome 请求结果
     */
    public void incrementRequest(String uri, String method, String outcome) {
        if (meterRegistry != null) {
            Counter.builder("api.requests.total")
                    .description("Total number of API requests")
                    .tag("uri", uri)
                    .tag("method", method)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * 记录API请求耗时
     *
     * @param uri      请求URI
     * @param method   HTTP方法
     * @param duration 请求耗时
     */
    public void recordRequestTime(String uri, String method, long duration, TimeUnit unit) {
        if (meterRegistry != null) {
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("api.request.duration")
                    .description("Duration of API requests")
                    .tag("uri", uri)
                    .tag("method", method)
                    .register(meterRegistry));
        }
    }

    /**
     * 记录API请求耗时（秒）
     *
     * @param uri             请求URI
     * @param method          HTTP方法
     * @param durationSeconds 请求耗时（秒）
     */
    public void recordRequestTimeSeconds(String uri, String method, double durationSeconds) {
        if (meterRegistry != null) {
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("api.request.duration")
                    .description("Duration of API requests")
                    .tag("uri", uri)
                    .tag("method", method)
                    .register(meterRegistry));
        }
    }

    /**
     * 获取MeterRegistry实例
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

}