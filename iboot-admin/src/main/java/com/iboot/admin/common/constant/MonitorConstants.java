package com.iboot.admin.common.constant;

/**
 * 监控相关常量
 */
public class MonitorConstants {

    /**
     * 应用监控指标前缀
     */
    public static final String METRICS_PREFIX = "iboot.admin";

    /**
     * HTTP请求监控指标名
     */
    public static final String HTTP_REQUEST_METRIC = "http.server.requests";

    /**
     * 数据库连接池监控指标名
     */
    public static final String DB_CONNECTION_POOL_METRIC = "jdbc.connections.active";

    /**
     * Redis连接监控指标名
     */
    public static final String REDIS_CONNECTION_METRIC = "lettuce.connection.count";

    /**
     * JVM监控指标前缀
     */
    public static final String JVM_METRICS_PREFIX = "jvm";

    /**
     * 系统监控指标前缀
     */
    public static final String SYSTEM_METRICS_PREFIX = "system";

    /**
     * 认证监控端点用户名
     */
    public static final String ACTUATOR_USERNAME = "actuator";

    /**
     * 监控端点路径
     */
    public static final String ACTUATOR_PATH = "/actuator";

    /**
     * 健康检查端点
     */
    public static final String HEALTH_ENDPOINT = "/actuator/health";

    /**
     * 指标端点
     */
    public static final String METRICS_ENDPOINT = "/actuator/metrics";

    /**
     * 应用信息端点
     */
    public static final String INFO_ENDPOINT = "/actuator/info";

}