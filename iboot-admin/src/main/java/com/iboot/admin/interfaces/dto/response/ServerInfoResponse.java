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
 * 服务器监控信息响应
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInfoResponse {

    /**
     * CPU 信息
     */
    private CpuInfo cpu;

    /**
     * 内存信息
     */
    private MemoryInfo memory;

    /**
     * JVM 信息
     */
    private JvmInfo jvm;

    /**
     * 磁盘信息
     */
    private List<DiskInfo> disks;

    /**
     * 操作系统信息
     */
    private OsInfo os;

    /**
     * 服务健康检查
     */
    private List<ServiceCheckInfo> serviceChecks;

    /**
     * Spring Boot Admin 控制台地址
     */
    private String sbaUrl;

    // ======================== 内部 DTO ========================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuInfo {
        /** CPU 核心数 */
        private int coreCount;
        /** CPU 型号 */
        private String model;
        /** CPU 系统使用率（百分比） */
        private double systemUsage;
        /** CPU 用户使用率（百分比） */
        private double userUsage;
        /** CPU 总使用率（百分比） */
        private double totalUsage;
        /** CPU 空闲率（百分比） */
        private double idle;
        /** 监控状态（UP/WARN/DOWN） */
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        /** 总内存（字节） */
        private long total;
        /** 已使用内存（字节） */
        private long used;
        /** 空闲内存（字节） */
        private long free;
        /** 使用率（百分比） */
        private double usageRate;
        /** 总内存（可读格式） */
        private String totalDisplay;
        /** 已使用内存（可读格式） */
        private String usedDisplay;
        /** 空闲内存（可读格式） */
        private String freeDisplay;
        /** 监控状态 */
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmInfo {
        /** JVM 最大内存（字节） */
        private long maxMemory;
        /** JVM 已分配内存（字节） */
        private long totalMemory;
        /** JVM 已使用内存（字节） */
        private long usedMemory;
        /** JVM 空闲内存（字节） */
        private long freeMemory;
        /** 使用率（百分比） */
        private double usageRate;
        /** JVM 最大内存（可读格式） */
        private String maxMemoryDisplay;
        /** JVM 已分配内存（可读格式） */
        private String totalMemoryDisplay;
        /** JVM 已使用内存（可读格式） */
        private String usedMemoryDisplay;
        /** JVM 空闲内存（可读格式） */
        private String freeMemoryDisplay;
        /** Java 版本 */
        private String javaVersion;
        /** JVM 名称 */
        private String jvmName;
        /** JVM 启动时间 */
        private String startTime;
        /** JVM 运行时长 */
        private String runTime;
        /** 监控状态 */
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfo {
        /** 挂载路径 */
        private String mountPoint;
        /** 文件系统类型 */
        private String fsType;
        /** 总大小（字节） */
        private long total;
        /** 已使用（字节） */
        private long used;
        /** 可用（字节） */
        private long free;
        /** 使用率（百分比） */
        private double usageRate;
        /** 总大小（可读格式） */
        private String totalDisplay;
        /** 已使用（可读格式） */
        private String usedDisplay;
        /** 可用（可读格式） */
        private String freeDisplay;
        /** 监控状态 */
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OsInfo {
        /** 操作系统名称 */
        private String name;
        /** 系统架构 */
        private String arch;
        /** 系统版本 */
        private String version;
        /** 主机名 */
        private String hostName;
        /** 系统运行时长 */
        private String uptime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceCheckInfo {
        /** 检查项名称 */
        private String name;
        /** 检查项类型（字典值：sys_monitor_type） */
        private String type;
        /** 状态（字典值：sys_monitor_status） */
        private String status;
        /** 状态标签（字典标签） */
        private String statusLabel;
        /** 响应时间（毫秒） */
        private long responseTime;
        /** 详细信息 */
        private String detail;
    }
}
