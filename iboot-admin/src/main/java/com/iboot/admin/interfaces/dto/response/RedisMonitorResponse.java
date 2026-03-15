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

package com.iboot.admin.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Redis 缓存监控信息响应
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisMonitorResponse {

    /**
     * Redis 服务器信息
     */
    private ServerInfo serverInfo;

    /**
     * 内存信息
     */
    private MemoryInfo memoryInfo;

    /**
     * 统计信息
     */
    private StatsInfo statsInfo;

    /**
     * 键空间信息（按数据库）
     */
    private List<KeyspaceInfo> keyspaceInfo;

    /**
     * 命令统计（Top10 热点命令）
     */
    private List<CommandStat> commandStats;

    // ======================== 内部 DTO ========================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerInfo {
        /** Redis 版本 */
        private String version;
        /** 运行模式（standalone/cluster/sentinel） */
        private String mode;
        /** 操作系统 */
        private String os;
        /** TCP 端口 */
        private int tcpPort;
        /** 运行时长（秒） */
        private long uptimeInSeconds;
        /** 运行时长（可读格式） */
        private String uptimeDisplay;
        /** 已连接客户端数 */
        private int connectedClients;
        /** 进程 ID */
        private long processId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        /** 已用内存（字节） */
        private long usedMemory;
        /** 已用内存（可读格式） */
        private String usedMemoryHuman;
        /** 最大内存（字节） */
        private long maxMemory;
        /** 最大内存（可读格式） */
        private String maxMemoryHuman;
        /** 内存使用率（百分比） */
        private double memoryUsageRate;
        /** 内存监控状态（UP/WARN/DOWN） */
        private String status;
        /** 状态标签（从字典获取） */
        private String statusLabel;
        /** 内存碎片率 */
        private double fragmentationRatio;
        /** 警告阈值（从配置读取） */
        private int warnThreshold;
        /** 异常阈值（从配置读取） */
        private int errorThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsInfo {
        /** 总连接数 */
        private long totalConnectionsReceived;
        /** 总命令处理数 */
        private long totalCommandsProcessed;
        /** 每秒操作数（QPS） */
        private long instantaneousOpsPerSec;
        /** 总网络输入字节 */
        private long totalNetInputBytes;
        /** 总网络输出字节 */
        private long totalNetOutputBytes;
        /** 键空间命中数 */
        private long keyspaceHits;
        /** 键空间未命中数 */
        private long keyspaceMisses;
        /** 命中率（百分比） */
        private double hitRate;
        /** 已过期键数 */
        private long expiredKeys;
        /** 已驱逐键数 */
        private long evictedKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyspaceInfo {
        /** 数据库索引 */
        private int dbIndex;
        /** 键总数 */
        private long keys;
        /** 设置过期的键数 */
        private long expires;
        /** 平均 TTL（毫秒） */
        private long avgTtl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandStat {
        /** 命令名 */
        private String command;
        /** 调用次数 */
        private long calls;
        /** 总耗时（微秒） */
        private long usec;
        /** 平均耗时（微秒/次） */
        private double usecPerCall;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheKeyInfo {
        /** 键名 */
        private String key;
        /** 类型（string/hash/list/set/zset） */
        private String type;
        /** 剩余 TTL（秒，-1 永久，-2 不存在） */
        private long ttl;
        /** TTL 显示（可读格式） */
        private String ttlDisplay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheKeyDetail {
        /** 键名 */
        private String key;
        /** 类型 */
        private String type;
        /** 剩余 TTL（秒） */
        private long ttl;
        /** 键值内容 */
        private Object value;
    }
}
