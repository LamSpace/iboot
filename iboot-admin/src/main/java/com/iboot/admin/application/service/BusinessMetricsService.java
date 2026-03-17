package com.iboot.admin.application.service;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.BaseUnits;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务指标收集服务
 * <p>
 * 负责收集和记录各类业务相关的监控指标，通过 Micrometer 框架将指标暴露给 Prometheus。 支持的指标包括：用户登录（成功/失败/在线数）、业务流程（耗时/成功/失败）、 缓存命中率、文件操作（上传/下载）等。
 * 所有指标均带有 application=iboot-admin 标签，便于 Grafana 仪表板进行数据展示。
 * </p>
 *
 * @author iBoot
 */
@Service
public class BusinessMetricsService {

    private static final Logger log = LoggerFactory.getLogger(BusinessMetricsService.class);

    @Autowired
    private MeterRegistry meterRegistry;

    // 用户相关指标
    private Counter userLoginSuccessCounter;

    private Counter userLoginFailedCounter;

    private AtomicInteger onlineUserCount;

    // 业务流程指标
    private Timer businessProcessTimer;

    private Counter businessProcessSuccessCounter;

    private Counter businessProcessFailedCounter;

    // 系统健康指标
    private Gauge cacheHitRateGauge;

    private AtomicInteger cacheHitCount;

    private AtomicInteger cacheTotalCount;

    // 数据库连接池指标由 DruidMetricsBinder 从 Druid 数据源直接获取
    // 文件操作指标
    private Counter fileUploadCounter;

    private Counter fileDownloadCounter;

    private DistributionSummary fileUploadSizeSummary;

    // 消息推送指标
    private AtomicInteger pushConnectionCount;

    private Counter pushEventSentCounter;

    private Counter pushEventFailedCounter;

    /**
     * 初始化业务监控指标
     * <p>
     * 在 Spring 容器启动后自动执行，注册各类指标到 MeterRegistry。 指标名称遵循 Grafana 仪表板期望的命名规范。
     * </p>
     */
    @PostConstruct
    public void init() {
        log.info("初始化业务监控指标...");
        // 用户登录指标 - 使用 Grafana 仪表板期望的指标名称
        userLoginSuccessCounter = Counter.builder("user_login_success")
                .description("用户登录成功次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        userLoginFailedCounter = Counter.builder("user_login_failed")
                .description("用户登录失败次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        onlineUserCount = new AtomicInteger(0);
        Gauge.builder("user_online_count", onlineUserCount, AtomicInteger::get)
                .description("在线用户数量")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        // 业务流程指标
        businessProcessTimer = Timer.builder("business_process_duration")
                .description("业务流程处理耗时")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        businessProcessSuccessCounter = Counter.builder("business_process_success")
                .description("业务流程成功次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        businessProcessFailedCounter = Counter.builder("business_process_failed")
                .description("业务流程失败次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        // 缓存指标
        cacheHitCount = new AtomicInteger(0);
        cacheTotalCount = new AtomicInteger(0);
        cacheHitRateGauge = Gauge.builder("cache_hit_rate_percent", this, BusinessMetricsService::calculateCacheHitRate)
                .description("缓存命中率")
                .tag("application", "iboot-admin")
                .baseUnit(BaseUnits.PERCENT)
                .register(meterRegistry);
        // 文件操作指标
        fileUploadCounter = Counter.builder("file_upload_count")
                .description("文件上传次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        fileDownloadCounter = Counter.builder("file_download_count")
                .description("文件下载次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        fileUploadSizeSummary = DistributionSummary.builder("file_upload_size")
                .description("文件上传大小分布")
                .tag("application", "iboot-admin")
                .baseUnit(BaseUnits.BYTES)
                .register(meterRegistry);
        // 消息推送指标
        pushConnectionCount = new AtomicInteger(0);
        Gauge.builder("push_connection_active", pushConnectionCount, AtomicInteger::get)
                .description("当前活跃的 SSE 推送连接数")
                .tag("application", "iboot-admin")
                .baseUnit(BaseUnits.CONNECTIONS)
                .register(meterRegistry);
        pushEventSentCounter = Counter.builder("push_event_sent")
                .description("推送事件发送成功次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        pushEventFailedCounter = Counter.builder("push_event_failed")
                .description("推送事件发送失败次数")
                .tag("application", "iboot-admin")
                .register(meterRegistry);
        log.info("业务监控指标初始化完成");
    }

    /**
     * 记录用户登录成功
     * <p>
     * 同时递增登录成功计数器和在线用户数量。
     * </p>
     */
    public void recordUserLoginSuccess() {
        userLoginSuccessCounter.increment();
        onlineUserCount.incrementAndGet();
    }

    /**
     * 记录用户登录失败
     * <p>
     * 仅递增登录失败计数器，不影响在线用户数量。
     * </p>
     */
    public void recordUserLoginFailed() {
        userLoginFailedCounter.increment();
    }

    /**
     * 记录用户登出
     * <p>
     * 递减在线用户数量。
     * </p>
     */
    public void recordUserLogout() {
        onlineUserCount.decrementAndGet();
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return onlineUserCount.get();
    }

    /**
     * 开始业务流程计时
     * <p>
     * 用于记录业务流程的处理耗时，配合 recordBusinessProcessSuccess 或 recordBusinessProcessFailed 使用。
     * </p>
     *
     * @return Timer.Sample 计时样本
     */
    public Timer.Sample startBusinessProcess() {
        return Timer.start(meterRegistry);
    }

    /**
     * 记录业务流程成功
     * <p>
     * 停止计时并记录成功次数，支持按流程类型打标签。
     * </p>
     *
     * @param sample      计时样本
     * @param processType 流程类型，用于标签区分
     */
    public void recordBusinessProcessSuccess(Timer.Sample sample, String processType) {
        sample.stop(Timer.builder("business_process_duration")
                .tag("process_type", processType)
                .tag("result", "success")
                .register(meterRegistry));
        businessProcessSuccessCounter.increment();
    }

    /**
     * 记录业务流程失败
     * <p>
     * 停止计时并记录失败次数，支持按流程类型打标签。
     * </p>
     *
     * @param sample      计时样本
     * @param processType 流程类型，用于标签区分
     */
    public void recordBusinessProcessFailed(Timer.Sample sample, String processType) {
        sample.stop(Timer.builder("business_process_duration")
                .tag("process_type", processType)
                .tag("result", "failed")
                .register(meterRegistry));
        businessProcessFailedCounter.increment();
    }

    /**
     * 记录缓存命中
     * <p>
     * 同时递增命中计数和总请求计数，用于计算命中率。
     * </p>
     */
    public void recordCacheHit() {
        cacheHitCount.incrementAndGet();
        cacheTotalCount.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     * <p>
     * 仅递增总请求计数，用于计算命中率。
     * </p>
     */
    public void recordCacheMiss() {
        cacheTotalCount.incrementAndGet();
    }

    /**
     * 计算缓存命中率
     * <p>
     * 命中率 = 命中次数 / 总请求次数 * 100%
     * </p>
     *
     * @return 缓存命中率（百分比）
     */
    private double calculateCacheHitRate() {
        int total = cacheTotalCount.get();
        if (total == 0)
            return 0.0;
        return (double) cacheHitCount.get() / total * 100;
    }

    /**
     * 记录文件上传
     * <p>
     * 递增上传计数器并记录文件大小分布。
     * </p>
     *
     * @param fileSizeBytes 文件大小（字节）
     */
    public void recordFileUpload(long fileSizeBytes) {
        fileUploadCounter.increment();
        fileUploadSizeSummary.record(fileSizeBytes);
    }

    /**
     * 记录文件下载
     * <p>
     * 递增下载计数器。
     * </p>
     */
    public void recordFileDownload() {
        fileDownloadCounter.increment();
    }

    /**
     * 更新推送连接数
     *
     * @param count 连接数
     */
    public void updatePushConnectionCount(int count) {
        pushConnectionCount.set(count);
    }

    /**
     * 记录推送事件发送成功
     */
    public void recordPushEventSent() {
        pushEventSentCounter.increment();
    }

    /**
     * 记录推送事件发送失败
     */
    public void recordPushEventFailed() {
        pushEventFailedCounter.increment();
    }

    /**
     * 获取推送连接数
     *
     * @return 推送连接数
     */
    public int getPushConnectionCount() {
        return pushConnectionCount.get();
    }

    /**
     * 获取指标统计信息
     * <p>
     * 返回简化的指标汇总字符串，用于日志或快速查看。
     * </p>
     *
     * @return 指标汇总字符串
     */
    public String getMetricsSummary() {
        return String.format("在线用户：%d, 缓存命中率：%.2f%%, 推送连接：%d", getOnlineUserCount(), calculateCacheHitRate(),
                getPushConnectionCount());
    }

}
