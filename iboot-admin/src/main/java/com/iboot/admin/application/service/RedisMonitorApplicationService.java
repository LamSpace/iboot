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

import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.interfaces.dto.response.RedisMonitorResponse;
import com.iboot.admin.interfaces.dto.response.RedisMonitorResponse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Redis 缓存监控应用服务
 * <p>
 * 提供 Redis 服务器信息采集、缓存键管理等功能。 监控阈值通过 sys_config 参数配置动态读取，状态标签通过 sys_dict 字典管理定义。 支持采集 Redis
 * 服务器信息、内存使用、命令统计、键空间信息，以及缓存键的浏览、查询和删除操作。
 * </p>
 *
 * @author iBoot
 */
@Service
public class RedisMonitorApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RedisMonitorApplicationService.class);

    private static final String MONITOR_STATUS_UP = "UP";

    private static final String MONITOR_STATUS_WARN = "WARN";

    private static final String MONITOR_STATUS_DOWN = "DOWN";

    private static final String DICT_MONITOR_STATUS = "sys_monitor_status";

    private final RedisConnectionFactory redisConnectionFactory;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ConfigApplicationService configApplicationService;

    private final DictApplicationService dictApplicationService;

    @SuppressWarnings("all")
    public RedisMonitorApplicationService(final RedisConnectionFactory redisConnectionFactory,
                                          final RedisTemplate<String, Object> redisTemplate, final ConfigApplicationService configApplicationService,
                                          final DictApplicationService dictApplicationService) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisTemplate = redisTemplate;
        this.configApplicationService = configApplicationService;
        this.dictApplicationService = dictApplicationService;
    }

    /**
     * 从 Properties 中获取字符串属性
     *
     * @param info Properties 对象
     * @param key  属性键
     *
     * @return 属性值，不存在则返回空字符串
     */
    private static String getProperty(Properties info, String key) {
        String value = info.getProperty(key);
        return value != null ? value : "";
    }

    /**
     * 从 Properties 中解析 int 值
     *
     * @param info Properties 对象
     * @param key  属性键
     *
     * @return 整数值，解析失败则返回 0
     */
    private static int parseInt(Properties info, String key) {
        try {
            String value = info.getProperty(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从 Properties 中解析 long 值
     *
     * @param info Properties 对象
     * @param key  属性键
     *
     * @return 长整数值，解析失败则返回 0
     */
    private static long parseLong(Properties info, String key) {
        try {
            String value = info.getProperty(key);
            return value != null ? Long.parseLong(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从 Properties 中解析 double 值
     *
     * @param info Properties 对象
     * @param key  属性键
     *
     * @return 双精度浮点数值，解析失败则返回 0
     */
    private static double parseDouble(Properties info, String key) {
        try {
            String value = info.getProperty(key);
            return value != null ? Double.parseDouble(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ======================== 构建监控数据 ========================

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
     * 格式化 TTL 为人类可读格式
     *
     * @param ttl 剩余生存时间（秒）
     *
     * @return 格式化后的字符串，如 "5 分钟 30 秒"
     */
    private static String formatTtl(Long ttl) {
        if (ttl == null || ttl == -2)
            return "已过期";
        if (ttl == -1)
            return "永久";
        if (ttl < 60)
            return ttl + "秒";
        if (ttl < 3600)
            return (ttl / 60) + "分钟" + (ttl % 60 > 0 ? (ttl % 60) + "秒" : "");
        if (ttl < 86400)
            return (ttl / 3600) + "小时" + ((ttl % 3600) / 60 > 0 ? ((ttl % 3600) / 60) + "分钟" : "");
        return (ttl / 86400) + "天" + ((ttl % 86400) / 3600 > 0 ? ((ttl % 86400) / 3600) + "小时" : "");
    }

    /**
     * 获取 Redis 完整监控信息
     * <p>
     * 通过 Redis INFO 命令采集服务器、内存、统计、键空间和命令统计信息。
     * </p>
     *
     * @return Redis 监控响应对象，包含所有监控数据
     */
    public RedisMonitorResponse getRedisMonitorInfo() {
        Properties info = getRedisInfo();
        return RedisMonitorResponse.builder()
                .serverInfo(buildServerInfo(info))
                .memoryInfo(buildMemoryInfo(info))
                .statsInfo(buildStatsInfo(info))
                .keyspaceInfo(buildKeyspaceInfo(info))
                .commandStats(buildCommandStats(info))
                .build();
    }

    /**
     * 分页查询缓存键列表
     * <p>
     * 使用 SCAN 命令遍历匹配指定模式的缓存键，避免使用 KEYS 命令导致阻塞。
     * </p>
     *
     * @param pattern  键名匹配模式，支持通配符，为空则匹配所有键
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 分页结果，包含缓存键信息列表和总数
     */
    public PageResult<CacheKeyInfo> getCacheKeys(String pattern, int pageNum, int pageSize) {
        List<String> allKeys = scanKeys(pattern);
        long total = allKeys.size();
        // 手动分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allKeys.size());
        List<String> pageKeys = start < allKeys.size() ? allKeys.subList(start, end) : Collections.emptyList();
        List<CacheKeyInfo> keyInfos = pageKeys.stream().map(this::buildCacheKeyInfo).collect(Collectors.toList());
        return new PageResult<>(keyInfos, total, pageNum, pageSize);
    }

    /**
     * 获取缓存键值详情
     * <p>
     * 根据键名获取其数据类型、TTL 和值。
     * </p>
     *
     * @param key 缓存键名
     *
     * @return 缓存键详情对象，不存在则返回 null
     */
    public CacheKeyDetail getCacheKeyValue(String key) {
        DataType dataType = redisTemplate.type(key);
        if (dataType == null || dataType == DataType.NONE) {
            return null;
        }
        String type = dataType.code();
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        Object value = getValueByType(key, dataType);
        return CacheKeyDetail.builder().key(key).type(type).ttl(ttl != null ? ttl : -2).value(value).build();
    }

    // ======================== 缓存键操作 ========================

    /**
     * 删除缓存键
     *
     * @param key 缓存键名
     *
     * @return 是否删除成功
     */
    public boolean deleteCacheKey(String key) {
        Boolean result = redisTemplate.delete(key);
        return result;
    }

    /**
     * 清空当前数据库所有缓存
     * <p>
     * 执行 FLUSHDB 命令清空当前选中的 Redis 数据库。 注意：此操作不可逆，请谨慎使用。
     * </p>
     */
    public void clearAllKeys() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        try {
            connection.serverCommands().flushDb();
        } finally {
            connection.close();
        }
    }

    /**
     * 获取 Redis INFO 信息
     * <p>
     * 通过 RedisConnection 执行 INFO 命令获取服务器信息。
     * </p>
     *
     * @return INFO 命令返回的 Properties 对象
     */
    private Properties getRedisInfo() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        try {
            Properties info = connection.serverCommands().info();
            return info != null ? info : new Properties();
        } finally {
            connection.close();
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 构建服务器信息对象
     *
     * @param info Redis INFO 信息
     *
     * @return 服务器信息对象
     */
    private ServerInfo buildServerInfo(Properties info) {
        long uptimeInSeconds = parseLong(info, "uptime_in_seconds");
        return ServerInfo.builder()
                .version(getProperty(info, "redis_version"))
                .mode(getProperty(info, "redis_mode"))
                .os(getProperty(info, "os"))
                .tcpPort(parseInt(info, "tcp_port"))
                .uptimeInSeconds(uptimeInSeconds)
                .uptimeDisplay(formatDuration(Duration.ofSeconds(uptimeInSeconds)))
                .connectedClients(parseInt(info, "connected_clients"))
                .processId(parseLong(info, "process_id"))
                .build();
    }

    /**
     * 构建内存信息对象
     * <p>
     * 计算内存使用率并从参数配置读取阈值评估状态。 maxmemory=0 表示无限制，视为正常状态。
     * </p>
     *
     * @param info Redis INFO 信息
     *
     * @return 内存信息对象，包含使用率和状态
     */
    private MemoryInfo buildMemoryInfo(Properties info) {
        long usedMemory = parseLong(info, "used_memory");
        long maxMemory = parseLong(info, "maxmemory");
        String usedMemoryHuman = getProperty(info, "used_memory_human");
        String maxMemoryHuman = getProperty(info, "maxmemory_human");
        double fragmentationRatio = parseDouble(info, "mem_fragmentation_ratio");
        // 计算内存使用率
        double memoryUsageRate = 0;
        if (maxMemory > 0) {
            memoryUsageRate = Math.round((usedMemory * 100.0) / maxMemory * 100.0) / 100.0;
        }
        // 从参数配置读取阈值
        int warnThreshold = getThresholdInt("monitor.redis.memory.warn.threshold", 70);
        int errorThreshold = getThresholdInt("monitor.redis.memory.error.threshold", 85);
        // 计算状态
        String status;
        if (maxMemory <= 0) {
            // maxmemory=0 表示无限制，视为正常
            status = MONITOR_STATUS_UP;
        } else if (memoryUsageRate >= errorThreshold) {
            status = MONITOR_STATUS_DOWN;
        } else if (memoryUsageRate >= warnThreshold) {
            status = MONITOR_STATUS_WARN;
        } else {
            status = MONITOR_STATUS_UP;
        }
        String statusLabel = dictApplicationService.getDictLabel(DICT_MONITOR_STATUS, status);
        if (statusLabel == null || statusLabel.isEmpty()) {
            statusLabel = status;
        }
        return MemoryInfo.builder()
                .usedMemory(usedMemory)
                .usedMemoryHuman(usedMemoryHuman != null ? usedMemoryHuman : formatBytes(usedMemory))
                .maxMemory(maxMemory)
                .maxMemoryHuman(maxMemoryHuman != null ? maxMemoryHuman : (maxMemory > 0 ? formatBytes(maxMemory) : "无限制"))
                .memoryUsageRate(memoryUsageRate)
                .status(status)
                .statusLabel(statusLabel)
                .fragmentationRatio(fragmentationRatio)
                .warnThreshold(warnThreshold)
                .errorThreshold(errorThreshold)
                .build();
    }

    /**
     * 构建统计信息对象
     * <p>
     * 计算缓存命中率 = keyspace_hits / (keyspace_hits + keyspace_misses)。
     * </p>
     *
     * @param info Redis INFO 信息
     *
     * @return 统计信息对象
     */
    private StatsInfo buildStatsInfo(Properties info) {
        long keyspaceHits = parseLong(info, "keyspace_hits");
        long keyspaceMisses = parseLong(info, "keyspace_misses");
        long totalHitMiss = keyspaceHits + keyspaceMisses;
        double hitRate = totalHitMiss > 0 ? Math.round((keyspaceHits * 100.0) / totalHitMiss * 100.0) / 100.0 : 0;
        return StatsInfo.builder()
                .totalConnectionsReceived(parseLong(info, "total_connections_received"))
                .totalCommandsProcessed(parseLong(info, "total_commands_processed"))
                .instantaneousOpsPerSec(parseLong(info, "instantaneous_ops_per_sec"))
                .totalNetInputBytes(parseLong(info, "total_net_input_bytes"))
                .totalNetOutputBytes(parseLong(info, "total_net_output_bytes"))
                .keyspaceHits(keyspaceHits)
                .keyspaceMisses(keyspaceMisses)
                .hitRate(hitRate)
                .expiredKeys(parseLong(info, "expired_keys"))
                .evictedKeys(parseLong(info, "evicted_keys"))
                .build();
    }

    /**
     * 解析键空间信息
     * <p>
     * Redis INFO 格式：db0:keys=10,expires=2,avg_ttl=1000
     * </p>
     *
     * @param info Redis INFO 信息
     *
     * @return 键空间信息列表，按数据库索引排序
     */
    private List<KeyspaceInfo> buildKeyspaceInfo(Properties info) {
        List<KeyspaceInfo> list = new ArrayList<>();
        Pattern dbPattern = Pattern.compile("^db(\\d+)$");
        Pattern valuePattern = Pattern.compile("keys=(\\d+),expires=(\\d+),avg_ttl=(\\d+)");
        for (String key : info.stringPropertyNames()) {
            Matcher dbMatcher = dbPattern.matcher(key);
            if (dbMatcher.matches()) {
                int dbIndex = Integer.parseInt(dbMatcher.group(1));
                String value = info.getProperty(key);
                Matcher valueMatcher = valuePattern.matcher(value);
                if (valueMatcher.matches()) {
                    list.add(KeyspaceInfo.builder()
                            .dbIndex(dbIndex)
                            .keys(Long.parseLong(valueMatcher.group(1)))
                            .expires(Long.parseLong(valueMatcher.group(2)))
                            .avgTtl(Long.parseLong(valueMatcher.group(3)))
                            .build());
                }
            }
        }
        list.sort(Comparator.comparingInt(KeyspaceInfo::getDbIndex));
        return list;
    }

    /**
     * 解析命令统计信息（Top10）
     * <p>
     * Redis INFO 格式：cmdstat_get:calls=100,usec=2000,usec_per_call=20.00 按调用次数降序取前 10 个命令。
     * </p>
     *
     * @param info Redis INFO 信息
     *
     * @return 命令统计列表，Top10
     */
    private List<CommandStat> buildCommandStats(Properties info) {
        List<CommandStat> list = new ArrayList<>();
        Pattern cmdPattern = Pattern.compile("^cmdstat_(.+)$");
        Pattern valuePattern = Pattern.compile("calls=(\\d+),usec=(\\d+),usec_per_call=([\\d.]+)");
        for (String key : info.stringPropertyNames()) {
            Matcher cmdMatcher = cmdPattern.matcher(key);
            if (cmdMatcher.matches()) {
                String command = cmdMatcher.group(1);
                String value = info.getProperty(key);
                Matcher valueMatcher = valuePattern.matcher(value);
                if (valueMatcher.matches()) {
                    list.add(CommandStat.builder()
                            .command(command.toUpperCase())
                            .calls(Long.parseLong(valueMatcher.group(1)))
                            .usec(Long.parseLong(valueMatcher.group(2)))
                            .usecPerCall(Double.parseDouble(valueMatcher.group(3)))
                            .build());
                }
            }
        }
        // 按调用次数降序，取 Top10
        list.sort((a, b) -> Long.compare(b.getCalls(), a.getCalls()));
        return list.size() > 10 ? list.subList(0, 10) : list;
    }

    /**
     * 使用 SCAN 命令遍历匹配键（避免 KEYS 命令阻塞）
     * <p>
     * SCAN 命令是迭代器，不会阻塞 Redis 服务器，适合生产环境使用。
     * </p>
     *
     * @param pattern 匹配模式，为空则匹配所有键
     *
     * @return 所有匹配的键名列表
     */
    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern != null && !pattern.isEmpty() ? pattern : "*")
                .count(1000)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        keys.sort(String::compareTo);
        return keys;
    }

    /**
     * 构建缓存键信息对象
     *
     * @param key 缓存键名
     *
     * @return 缓存键信息对象
     */
    private CacheKeyInfo buildCacheKeyInfo(String key) {
        DataType dataType = redisTemplate.type(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return CacheKeyInfo.builder()
                .key(key)
                .type(dataType != null ? dataType.code() : "unknown")
                .ttl(ttl != null ? ttl : -2)
                .ttlDisplay(formatTtl(ttl))
                .build();
    }

    /**
     * 根据键类型获取值
     * <p>
     * 支持 STRING、HASH、LIST、SET、ZSET 五种 Redis 数据类型。
     * </p>
     *
     * @param key      缓存键名
     * @param dataType 数据类型
     *
     * @return 键值对象，获取失败则返回错误信息
     */
    private Object getValueByType(String key, DataType dataType) {
        try {
            return switch (dataType) {
                case STRING -> redisTemplate.opsForValue().get(key);
                case HASH -> redisTemplate.opsForHash().entries(key);
                case LIST -> redisTemplate.opsForList().range(key, 0, -1);
                case SET -> redisTemplate.opsForSet().members(key);
                case ZSET -> redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
                default -> "不支持的数据类型：" + dataType.code();
            };
        } catch (Exception e) {
            log.warn("获取键 [{}] 的值失败：{}", key, e.getMessage());
            return "获取值失败：" + e.getMessage();
        }
    }

    /**
     * 从系统配置读取阈值，若配置不存在或格式错误则使用默认值
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     *
     * @return 阈值
     */
    private int getThresholdInt(String configKey, int defaultValue) {
        try {
            String value = configApplicationService.getConfigValue(configKey);
            if (value != null && !value.isEmpty()) {
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.debug("读取监控阈值配置 [{}] 失败，使用默认值：{}", configKey, defaultValue);
        }
        return defaultValue;
    }

}
