/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.application.service;

import com.iboot.admin.interfaces.dto.response.ServerInfoResponse;
import com.iboot.admin.interfaces.dto.response.ServerInfoResponse.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 服务器监控应用服务
 * <p>
 * 提供服务器系统信息采集、服务健康检查等功能。 监控阈值通过 sys_config 参数配置动态读取，检查项类型和状态通过 sys_dict 字典管理定义。 支持 CPU、内存、JVM、磁盘、操作系统等信息采集，以及数据库和 Redis
 * 连接健康检查。
 * </p>
 *
 * @author iBoot
 */
@Service
public class ServerMonitorApplicationService {

    @SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(ServerMonitorApplicationService.class);

    private static final String MONITOR_STATUS_UP = "UP";

    private static final String MONITOR_STATUS_WARN = "WARN";

    private static final String MONITOR_STATUS_DOWN = "DOWN";

    private static final String DICT_MONITOR_STATUS = "sys_monitor_status";

    private final ConfigApplicationService configApplicationService;

    private final DictApplicationService dictApplicationService;

    private final DataSource dataSource;

    private final RedisConnectionFactory redisConnectionFactory;

    @SuppressWarnings("all")
    public ServerMonitorApplicationService(final ConfigApplicationService configApplicationService,
                                           final DictApplicationService dictApplicationService, final DataSource dataSource,
                                           final RedisConnectionFactory redisConnectionFactory) {
        this.configApplicationService = configApplicationService;
        this.dictApplicationService = dictApplicationService;
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 保留两位小数
     *
     * @param value 原始值
     *
     * @return 四舍五入后的值
     */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 格式化字节数为人类可读格式
     *
     * @param bytes 字节数
     *
     * @return 格式化后的字符串，如 "1.23 MB"
     */
    private static String formatBytes(long bytes) {
        if (bytes <= 0)
            return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) (Math.log(bytes) / Math.log(1024));
        i = Math.min(i, units.length - 1);
        return String.format("%.2f %s", bytes / Math.pow(1024, i), units[i]);
    }

    /**
     * 格式化时长为人类可读格式
     *
     * @param duration 时长对象
     *
     * @return 格式化后的字符串，如 "1 天 2 小时 3 分钟 4 秒"
     */
    private static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("天 ");
        if (hours > 0)
            sb.append(hours).append("小时 ");
        if (minutes > 0)
            sb.append(minutes).append("分钟 ");
        sb.append(seconds).append("秒");
        return sb.toString();
    }

    /**
     * 获取完整的服务器监控信息
     * <p>
     * 综合采集 CPU、内存、JVM、磁盘、操作系统信息，并执行服务健康检查。
     * </p>
     *
     * @return 服务器信息响应对象，包含所有监控数据
     */
    public ServerInfoResponse getServerInfo() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();
        return ServerInfoResponse.builder()
                .cpu(getCpuInfo(hardware.getProcessor()))
                .memory(getMemoryInfo(hardware.getMemory()))
                .jvm(getJvmInfo())
                .disks(getDiskInfo(os.getFileSystem()))
                .os(getOsInfo(os))
                .serviceChecks(getServiceChecks())
                .sbaUrl(getSbaUrl())
                .build();
    }

    /**
     * 获取 CPU 信息
     * <p>
     * 通过 OSHI 采集 CPU 核心数、型号和使用率等信息。 CPU 使用率需要通过两次采样计算差值获得。
     * </p>
     *
     * @param processor CPU 处理器对象
     *
     * @return CPU 信息对象，包含使用率和状态
     */
    private CpuInfo getCpuInfo(CentralProcessor processor) {
        // 获取 CPU 使用率（需要两次采样计算差值）
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long[] ticks = processor.getSystemCpuLoadTicks();
        long user = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long totalCpu = user + nice + sys + idle + iowait + irq + softirq + steal;
        double systemUsage = totalCpu == 0 ? 0 : round((sys * 100.0) / totalCpu);
        double userUsage = totalCpu == 0 ? 0 : round(((user + nice) * 100.0) / totalCpu);
        double totalUsage = totalCpu == 0 ? 0 : round(((totalCpu - idle) * 100.0) / totalCpu);
        double idleRate = totalCpu == 0 ? 0 : round((idle * 100.0) / totalCpu);
        return CpuInfo.builder()
                .coreCount(processor.getLogicalProcessorCount())
                .model(processor.getProcessorIdentifier().getName())
                .systemUsage(systemUsage)
                .userUsage(userUsage)
                .totalUsage(totalUsage)
                .idle(idleRate)
                .status(evaluateStatus(totalUsage, "monitor.cpu.warn.threshold", "monitor.cpu.error.threshold"))
                .build();
    }

    /**
     * 获取物理内存信息
     * <p>
     * 通过 OSHI 采集物理内存总量、已用、空闲和使用率。
     * </p>
     *
     * @param memory 全局内存对象
     *
     * @return 内存信息对象，包含使用率和状态
     */
    private MemoryInfo getMemoryInfo(GlobalMemory memory) {
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        double usageRate = total == 0 ? 0 : round((used * 100.0) / total);
        return MemoryInfo.builder()
                .total(total)
                .used(used)
                .free(available)
                .usageRate(usageRate)
                .totalDisplay(formatBytes(total))
                .usedDisplay(formatBytes(used))
                .freeDisplay(formatBytes(available))
                .status(evaluateStatus(usageRate, "monitor.memory.warn.threshold", "monitor.memory.error.threshold"))
                .build();
    }

    /**
     * 获取 JVM 信息
     * <p>
     * 通过 Java Management API 采集 JVM 内存、版本、启动时间和运行时长。
     * </p>
     *
     * @return JVM 信息对象，包含内存使用率和运行状态
     */
    private JvmInfo getJvmInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usageRate = maxMemory == 0 ? 0 : round((usedMemory * 100.0) / maxMemory);
        // 计算运行时长
        long startTime = runtimeMXBean.getStartTime();
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
        Duration uptime = Duration.ofMillis(System.currentTimeMillis() - startTime);
        return JvmInfo.builder()
                .maxMemory(maxMemory)
                .totalMemory(totalMemory)
                .usedMemory(usedMemory)
                .freeMemory(freeMemory)
                .usageRate(usageRate)
                .maxMemoryDisplay(formatBytes(maxMemory))
                .totalMemoryDisplay(formatBytes(totalMemory))
                .usedMemoryDisplay(formatBytes(usedMemory))
                .freeMemoryDisplay(formatBytes(freeMemory))
                .javaVersion(System.getProperty("java.version"))
                .jvmName(runtimeMXBean.getVmName() + " " + runtimeMXBean.getVmVersion())
                .startTime(startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .runTime(formatDuration(uptime))
                .status(evaluateStatus(usageRate, "monitor.jvm.warn.threshold", "monitor.jvm.error.threshold"))
                .build();
    }

    /**
     * 获取磁盘信息
     * <p>
     * 遍历所有文件系统，采集各挂载点的磁盘总量、已用、空闲和使用率。
     * </p>
     *
     * @param fileSystem 文件系统对象
     *
     * @return 磁盘信息列表，每个磁盘包含使用率和状态
     */
    private List<DiskInfo> getDiskInfo(FileSystem fileSystem) {
        List<DiskInfo> diskInfoList = new ArrayList<>();
        double diskWarnThreshold = getThreshold("monitor.disk.warn.threshold", 80);
        double diskErrorThreshold = getThreshold("monitor.disk.error.threshold", 90);
        for (OSFileStore fs : fileSystem.getFileStores()) {
            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = total - free;
            double usageRate = total == 0 ? 0 : round((used * 100.0) / total);
            String status;
            if (usageRate >= diskErrorThreshold) {
                status = MONITOR_STATUS_DOWN;
            } else if (usageRate >= diskWarnThreshold) {
                status = MONITOR_STATUS_WARN;
            } else {
                status = MONITOR_STATUS_UP;
            }
            diskInfoList.add(DiskInfo.builder()
                    .mountPoint(fs.getMount())
                    .fsType(fs.getType())
                    .total(total)
                    .used(used)
                    .free(free)
                    .usageRate(usageRate)
                    .totalDisplay(formatBytes(total))
                    .usedDisplay(formatBytes(used))
                    .freeDisplay(formatBytes(free))
                    .status(status)
                    .build());
        }
        return diskInfoList;
    }

    /**
     * 获取操作系统信息
     * <p>
     * 采集操作系统名称、架构、版本、主机名和运行时长。
     * </p>
     *
     * @param os 操作系统对象
     *
     * @return 操作系统信息对象
     */
    private OsInfo getOsInfo(OperatingSystem os) {
        long uptimeSeconds = os.getSystemUptime();
        Duration uptime = Duration.ofSeconds(uptimeSeconds);
        return OsInfo.builder()
                .name(os.toString())
                .arch(System.getProperty("os.arch"))
                .version(os.getVersionInfo().toString())
                .hostName(os.getNetworkParams().getHostName())
                .uptime(formatDuration(uptime))
                .build();
    }

    /**
     * 执行各项服务健康检查
     * <p>
     * 检查数据库连接和 Redis 连接状态。 检查项类型由字典 sys_monitor_type 定义。
     * </p>
     *
     * @return 服务检查信息列表，包含每个服务的健康状态
     */
    private List<ServiceCheckInfo> getServiceChecks() {
        List<ServiceCheckInfo> checks = new ArrayList<>();
        // 数据库连接检查
        checks.add(checkDatabase());
        // Redis 连接检查
        checks.add(checkRedis());
        return checks;
    }

    /**
     * 检查数据库连接
     * <p>
     * 通过获取数据库连接并验证其有效性来检查数据库健康状态。
     * </p>
     *
     * @return 数据库服务检查信息对象
     */
    private ServiceCheckInfo checkDatabase() {
        long start = System.currentTimeMillis();
        String status;
        String detail;
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            status = valid ? MONITOR_STATUS_UP : MONITOR_STATUS_DOWN;
            detail = valid ? "数据库连接正常" : "数据库连接验证失败";
        } catch (Exception e) {
            status = MONITOR_STATUS_DOWN;
            detail = "数据库连接异常：" + e.getMessage();
            log.warn("数据库健康检查失败", e);
        }
        long responseTime = System.currentTimeMillis() - start;
        return ServiceCheckInfo.builder()
                .name("MySQL 数据库")
                .type("db")
                .status(status)
                .statusLabel(getStatusLabel(status))
                .responseTime(responseTime)
                .detail(detail)
                .build();
    }

    /**
     * 检查 Redis 连接
     * <p>
     * 通过连接 Redis 并执行 PING 命令来检查 Redis 健康状态。
     * </p>
     *
     * @return Redis 服务检查信息对象
     */
    private ServiceCheckInfo checkRedis() {
        long start = System.currentTimeMillis();
        String status;
        String detail;
        try {
            var connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            connection.close();
            status = "PONG".equals(pong) ? MONITOR_STATUS_UP : MONITOR_STATUS_DOWN;
            detail = "PONG".equals(pong) ? "Redis 连接正常" : "Redis 响应异常：" + pong;
        } catch (Exception e) {
            status = MONITOR_STATUS_DOWN;
            detail = "Redis 连接异常：" + e.getMessage();
            log.warn("Redis 健康检查失败", e);
        }
        long responseTime = System.currentTimeMillis() - start;
        return ServiceCheckInfo.builder()
                .name("Redis 缓存")
                .type("redis")
                .status(status)
                .statusLabel(getStatusLabel(status))
                .responseTime(responseTime)
                .detail(detail)
                .build();
    }

    // ======================== 工具方法 ========================

    /**
     * 根据使用率和可配置阈值评估监控状态
     * <p>
     * 从 sys_config 参数配置中动态读取警告和错误阈值。
     * </p>
     *
     * @param usageRate         当前使用率（百分比）
     * @param warnThresholdKey  警告阈值配置键
     * @param errorThresholdKey 错误阈值配置键
     *
     * @return 监控状态：UP（正常）、WARN（警告）、DOWN（异常）
     */
    private String evaluateStatus(double usageRate, String warnThresholdKey, String errorThresholdKey) {
        double warnThreshold = getThreshold(warnThresholdKey, 80);
        double errorThreshold = getThreshold(errorThresholdKey, 90);
        if (usageRate >= errorThreshold) {
            return MONITOR_STATUS_DOWN;
        } else if (usageRate >= warnThreshold) {
            return MONITOR_STATUS_WARN;
        }
        return MONITOR_STATUS_UP;
    }

    /**
     * 从参数配置读取阈值，若配置不存在则使用默认值
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     *
     * @return 阈值
     */
    private double getThreshold(String configKey, double defaultValue) {
        try {
            String value = configApplicationService.getConfigValue(configKey);
            if (value != null && !value.isEmpty()) {
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            log.debug("读取监控阈值配置 [{}] 失败，使用默认值：{}", configKey, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 获取状态标签（从字典 sys_monitor_status 获取）
     *
     * @param statusValue 状态值
     *
     * @return 状态标签，获取失败则返回原状态值
     */
    private String getStatusLabel(String statusValue) {
        String label = dictApplicationService.getDictLabel(DICT_MONITOR_STATUS, statusValue);
        return (label == null || label.isEmpty()) ? statusValue : label;
    }

    /**
     * 获取 Spring Boot Admin 控制台地址
     * <p>
     * 从系统配置读取，若未配置则返回默认路径。
     * </p>
     *
     * @return SBA 控制台地址
     */
    private String getSbaUrl() {
        String sbaPath = configApplicationService.getConfigValue("monitor.sba.url");
        return (sbaPath != null && !sbaPath.isEmpty()) ? sbaPath : "/sba";
    }

}
