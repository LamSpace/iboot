package com.iboot.admin.infrastructure.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Druid 连接池指标绑定器 将 Druid 连接池的监控指标注册到 Micrometer，供 Prometheus 采集
 */
@Component
@ConditionalOnBean(DruidDataSource.class)
public class DruidMetricsBinder implements MeterBinder {

    private static final Logger log = LoggerFactory.getLogger(DruidMetricsBinder.class);

    @Autowired
    private DruidDataSource druidDataSource;

    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        log.info("初始化 Druid 连接池监控指标...");
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;
        // 活跃连接数
        Gauge.builder("db_connection_active", druidDataSource, ds -> (double) ds.getActiveCount())
                .description("数据库活跃连接数")
                .tag("application", "iboot-admin")
                .tag("datasource", "primary")
                .register(registry);
        // 空闲连接数 = 总连接数 - 活跃连接数
        Gauge.builder("db_connection_idle", druidDataSource, ds -> {
            int total = ds.getPoolingCount();
            int active = ds.getActiveCount();
            return (double) Math.max(0, total - active);
        }).description("数据库空闲连接数").tag("application", "iboot-admin").tag("datasource", "primary").register(registry);
        // 总连接数（当前连接池中的连接总数）
        Gauge.builder("db_connection_total", druidDataSource, ds -> (double) ds.getPoolingCount())
                .description("数据库总连接数")
                .tag("application", "iboot-admin")
                .tag("datasource", "primary")
                .register(registry);
        // 活跃连接数峰值
        Gauge.builder("db_connection_active_max", druidDataSource, ds -> (double) ds.getMaxActive())
                .description("数据库最大活跃连接数")
                .tag("application", "iboot-admin")
                .tag("datasource", "primary")
                .register(registry);
        // 等待线程数
        Gauge.builder("db_connection_wait_threads", druidDataSource, ds -> (double) ds.getWaitThreadCount())
                .description("等待获取连接的线程数")
                .tag("application", "iboot-admin")
                .tag("datasource", "primary")
                .register(registry);
        // 连接池使用率
        Gauge.builder("db_connection_pool_usage", druidDataSource, ds -> {
                    int max = ds.getMaxActive();
                    if (max == 0)
                        return 0.0;
                    return (double) ds.getActiveCount() / max * 100;
                })
                .description("数据库连接池使用率 (%)")
                .tag("application", "iboot-admin")
                .tag("datasource", "primary")
                .register(registry);
        log.info("Druid 连接池监控指标注册完成");
    }

}
