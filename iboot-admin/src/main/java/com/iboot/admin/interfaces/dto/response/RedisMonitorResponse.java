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

import java.util.List;

/**
 * Redis 缓存监控信息响应
 *
 * @author iBoot
 */
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

    @SuppressWarnings("all")
    public RedisMonitorResponse() {
    }

    /**
     * Creates a new {@code RedisMonitorResponse} instance.
     *
     * @param serverInfo   Redis 服务器信息
     * @param memoryInfo   内存信息
     * @param statsInfo    统计信息
     * @param keyspaceInfo 键空间信息（按数据库）
     * @param commandStats 命令统计（Top10 热点命令）
     */
    @SuppressWarnings("all")
    public RedisMonitorResponse(final ServerInfo serverInfo, final MemoryInfo memoryInfo, final StatsInfo statsInfo,
                                final List<KeyspaceInfo> keyspaceInfo, final List<CommandStat> commandStats) {
        this.serverInfo = serverInfo;
        this.memoryInfo = memoryInfo;
        this.statsInfo = statsInfo;
        this.keyspaceInfo = keyspaceInfo;
        this.commandStats = commandStats;
    }

    @SuppressWarnings("all")
    public static RedisMonitorResponse.RedisMonitorResponseBuilder builder() {
        return new RedisMonitorResponse.RedisMonitorResponseBuilder();
    }

    /**
     * Redis 服务器信息
     */
    @SuppressWarnings("all")
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    /**
     * Redis 服务器信息
     */
    @SuppressWarnings("all")
    public void setServerInfo(final ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    /**
     * 内存信息
     */
    @SuppressWarnings("all")
    public MemoryInfo getMemoryInfo() {
        return this.memoryInfo;
    }

    /**
     * 内存信息
     */
    @SuppressWarnings("all")
    public void setMemoryInfo(final MemoryInfo memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    /**
     * 统计信息
     */
    @SuppressWarnings("all")
    public StatsInfo getStatsInfo() {
        return this.statsInfo;
    }

    /**
     * 统计信息
     */
    @SuppressWarnings("all")
    public void setStatsInfo(final StatsInfo statsInfo) {
        this.statsInfo = statsInfo;
    }

    /**
     * 键空间信息（按数据库）
     */
    @SuppressWarnings("all")
    public List<KeyspaceInfo> getKeyspaceInfo() {
        return this.keyspaceInfo;
    }

    /**
     * 键空间信息（按数据库）
     */
    @SuppressWarnings("all")
    public void setKeyspaceInfo(final List<KeyspaceInfo> keyspaceInfo) {
        this.keyspaceInfo = keyspaceInfo;
    }

    /**
     * 命令统计（Top10 热点命令）
     */
    @SuppressWarnings("all")
    public List<CommandStat> getCommandStats() {
        return this.commandStats;
    }

    /**
     * 命令统计（Top10 热点命令）
     */
    @SuppressWarnings("all")
    public void setCommandStats(final List<CommandStat> commandStats) {
        this.commandStats = commandStats;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RedisMonitorResponse))
            return false;
        final RedisMonitorResponse other = (RedisMonitorResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$serverInfo = this.getServerInfo();
        final java.lang.Object other$serverInfo = other.getServerInfo();
        if (this$serverInfo == null ? other$serverInfo != null : !this$serverInfo.equals(other$serverInfo))
            return false;
        final java.lang.Object this$memoryInfo = this.getMemoryInfo();
        final java.lang.Object other$memoryInfo = other.getMemoryInfo();
        if (this$memoryInfo == null ? other$memoryInfo != null : !this$memoryInfo.equals(other$memoryInfo))
            return false;
        final java.lang.Object this$statsInfo = this.getStatsInfo();
        final java.lang.Object other$statsInfo = other.getStatsInfo();
        if (this$statsInfo == null ? other$statsInfo != null : !this$statsInfo.equals(other$statsInfo))
            return false;
        final java.lang.Object this$keyspaceInfo = this.getKeyspaceInfo();
        final java.lang.Object other$keyspaceInfo = other.getKeyspaceInfo();
        if (this$keyspaceInfo == null ? other$keyspaceInfo != null : !this$keyspaceInfo.equals(other$keyspaceInfo))
            return false;
        final java.lang.Object this$commandStats = this.getCommandStats();
        final java.lang.Object other$commandStats = other.getCommandStats();
        if (this$commandStats == null ? other$commandStats != null : !this$commandStats.equals(other$commandStats))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RedisMonitorResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $serverInfo = this.getServerInfo();
        result = result * PRIME + ($serverInfo == null ? 43 : $serverInfo.hashCode());
        final java.lang.Object $memoryInfo = this.getMemoryInfo();
        result = result * PRIME + ($memoryInfo == null ? 43 : $memoryInfo.hashCode());
        final java.lang.Object $statsInfo = this.getStatsInfo();
        result = result * PRIME + ($statsInfo == null ? 43 : $statsInfo.hashCode());
        final java.lang.Object $keyspaceInfo = this.getKeyspaceInfo();
        result = result * PRIME + ($keyspaceInfo == null ? 43 : $keyspaceInfo.hashCode());
        final java.lang.Object $commandStats = this.getCommandStats();
        result = result * PRIME + ($commandStats == null ? 43 : $commandStats.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "RedisMonitorResponse(serverInfo=" + this.getServerInfo() + ", memoryInfo=" + this.getMemoryInfo()
                + ", statsInfo=" + this.getStatsInfo() + ", keyspaceInfo=" + this.getKeyspaceInfo() + ", commandStats="
                + this.getCommandStats() + ")";
    }

    // ======================== 内部 DTO ========================
    public static class ServerInfo {

        /**
         * Redis 版本
         */
        private String version;

        /**
         * 运行模式（standalone/cluster/sentinel）
         */
        private String mode;

        /**
         * 操作系统
         */
        private String os;

        /**
         * TCP 端口
         */
        private int tcpPort;

        /**
         * 运行时长（秒）
         */
        private long uptimeInSeconds;

        /**
         * 运行时长（可读格式）
         */
        private String uptimeDisplay;

        /**
         * 已连接客户端数
         */
        private int connectedClients;

        /**
         * 进程 ID
         */
        private long processId;

        @SuppressWarnings("all")
        public ServerInfo() {
        }

        /**
         * Creates a new {@code ServerInfo} instance.
         *
         * @param version          Redis 版本
         * @param mode             运行模式（standalone/cluster/sentinel）
         * @param os               操作系统
         * @param tcpPort          TCP 端口
         * @param uptimeInSeconds  运行时长（秒）
         * @param uptimeDisplay    运行时长（可读格式）
         * @param connectedClients 已连接客户端数
         * @param processId        进程 ID
         */
        @SuppressWarnings("all")
        public ServerInfo(final String version, final String mode, final String os, final int tcpPort,
                          final long uptimeInSeconds, final String uptimeDisplay, final int connectedClients,
                          final long processId) {
            this.version = version;
            this.mode = mode;
            this.os = os;
            this.tcpPort = tcpPort;
            this.uptimeInSeconds = uptimeInSeconds;
            this.uptimeDisplay = uptimeDisplay;
            this.connectedClients = connectedClients;
            this.processId = processId;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.ServerInfo.ServerInfoBuilder builder() {
            return new RedisMonitorResponse.ServerInfo.ServerInfoBuilder();
        }

        /**
         * Redis 版本
         */
        @SuppressWarnings("all")
        public String getVersion() {
            return this.version;
        }

        /**
         * Redis 版本
         */
        @SuppressWarnings("all")
        public void setVersion(final String version) {
            this.version = version;
        }

        /**
         * 运行模式（standalone/cluster/sentinel）
         */
        @SuppressWarnings("all")
        public String getMode() {
            return this.mode;
        }

        /**
         * 运行模式（standalone/cluster/sentinel）
         */
        @SuppressWarnings("all")
        public void setMode(final String mode) {
            this.mode = mode;
        }

        /**
         * 操作系统
         */
        @SuppressWarnings("all")
        public String getOs() {
            return this.os;
        }

        /**
         * 操作系统
         */
        @SuppressWarnings("all")
        public void setOs(final String os) {
            this.os = os;
        }

        /**
         * TCP 端口
         */
        @SuppressWarnings("all")
        public int getTcpPort() {
            return this.tcpPort;
        }

        /**
         * TCP 端口
         */
        @SuppressWarnings("all")
        public void setTcpPort(final int tcpPort) {
            this.tcpPort = tcpPort;
        }

        /**
         * 运行时长（秒）
         */
        @SuppressWarnings("all")
        public long getUptimeInSeconds() {
            return this.uptimeInSeconds;
        }

        /**
         * 运行时长（秒）
         */
        @SuppressWarnings("all")
        public void setUptimeInSeconds(final long uptimeInSeconds) {
            this.uptimeInSeconds = uptimeInSeconds;
        }

        /**
         * 运行时长（可读格式）
         */
        @SuppressWarnings("all")
        public String getUptimeDisplay() {
            return this.uptimeDisplay;
        }

        /**
         * 运行时长（可读格式）
         */
        @SuppressWarnings("all")
        public void setUptimeDisplay(final String uptimeDisplay) {
            this.uptimeDisplay = uptimeDisplay;
        }

        /**
         * 已连接客户端数
         */
        @SuppressWarnings("all")
        public int getConnectedClients() {
            return this.connectedClients;
        }

        /**
         * 已连接客户端数
         */
        @SuppressWarnings("all")
        public void setConnectedClients(final int connectedClients) {
            this.connectedClients = connectedClients;
        }

        /**
         * 进程 ID
         */
        @SuppressWarnings("all")
        public long getProcessId() {
            return this.processId;
        }

        /**
         * 进程 ID
         */
        @SuppressWarnings("all")
        public void setProcessId(final long processId) {
            this.processId = processId;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.ServerInfo))
                return false;
            final RedisMonitorResponse.ServerInfo other = (RedisMonitorResponse.ServerInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTcpPort() != other.getTcpPort())
                return false;
            if (this.getUptimeInSeconds() != other.getUptimeInSeconds())
                return false;
            if (this.getConnectedClients() != other.getConnectedClients())
                return false;
            if (this.getProcessId() != other.getProcessId())
                return false;
            final java.lang.Object this$version = this.getVersion();
            final java.lang.Object other$version = other.getVersion();
            if (this$version == null ? other$version != null : !this$version.equals(other$version))
                return false;
            final java.lang.Object this$mode = this.getMode();
            final java.lang.Object other$mode = other.getMode();
            if (this$mode == null ? other$mode != null : !this$mode.equals(other$mode))
                return false;
            final java.lang.Object this$os = this.getOs();
            final java.lang.Object other$os = other.getOs();
            if (this$os == null ? other$os != null : !this$os.equals(other$os))
                return false;
            final java.lang.Object this$uptimeDisplay = this.getUptimeDisplay();
            final java.lang.Object other$uptimeDisplay = other.getUptimeDisplay();
            if (this$uptimeDisplay == null ? other$uptimeDisplay != null
                    : !this$uptimeDisplay.equals(other$uptimeDisplay))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.ServerInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getTcpPort();
            final long $uptimeInSeconds = this.getUptimeInSeconds();
            result = result * PRIME + (int) ($uptimeInSeconds >>> 32 ^ $uptimeInSeconds);
            result = result * PRIME + this.getConnectedClients();
            final long $processId = this.getProcessId();
            result = result * PRIME + (int) ($processId >>> 32 ^ $processId);
            final java.lang.Object $version = this.getVersion();
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            final java.lang.Object $mode = this.getMode();
            result = result * PRIME + ($mode == null ? 43 : $mode.hashCode());
            final java.lang.Object $os = this.getOs();
            result = result * PRIME + ($os == null ? 43 : $os.hashCode());
            final java.lang.Object $uptimeDisplay = this.getUptimeDisplay();
            result = result * PRIME + ($uptimeDisplay == null ? 43 : $uptimeDisplay.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.ServerInfo(version=" + this.getVersion() + ", mode=" + this.getMode() + ", os="
                    + this.getOs() + ", tcpPort=" + this.getTcpPort() + ", uptimeInSeconds=" + this.getUptimeInSeconds()
                    + ", uptimeDisplay=" + this.getUptimeDisplay() + ", connectedClients=" + this.getConnectedClients()
                    + ", processId=" + this.getProcessId() + ")";
        }

        @SuppressWarnings("all")
        public static class ServerInfoBuilder {

            @SuppressWarnings("all")
            private String version;

            @SuppressWarnings("all")
            private String mode;

            @SuppressWarnings("all")
            private String os;

            @SuppressWarnings("all")
            private int tcpPort;

            @SuppressWarnings("all")
            private long uptimeInSeconds;

            @SuppressWarnings("all")
            private String uptimeDisplay;

            @SuppressWarnings("all")
            private int connectedClients;

            @SuppressWarnings("all")
            private long processId;

            @SuppressWarnings("all")
            ServerInfoBuilder() {
            }

            /**
             * Redis 版本
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder version(final String version) {
                this.version = version;
                return this;
            }

            /**
             * 运行模式（standalone/cluster/sentinel）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder mode(final String mode) {
                this.mode = mode;
                return this;
            }

            /**
             * 操作系统
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder os(final String os) {
                this.os = os;
                return this;
            }

            /**
             * TCP 端口
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder tcpPort(final int tcpPort) {
                this.tcpPort = tcpPort;
                return this;
            }

            /**
             * 运行时长（秒）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder uptimeInSeconds(final long uptimeInSeconds) {
                this.uptimeInSeconds = uptimeInSeconds;
                return this;
            }

            /**
             * 运行时长（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder uptimeDisplay(final String uptimeDisplay) {
                this.uptimeDisplay = uptimeDisplay;
                return this;
            }

            /**
             * 已连接客户端数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder connectedClients(final int connectedClients) {
                this.connectedClients = connectedClients;
                return this;
            }

            /**
             * 进程 ID
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo.ServerInfoBuilder processId(final long processId) {
                this.processId = processId;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.ServerInfo build() {
                return new RedisMonitorResponse.ServerInfo(this.version, this.mode, this.os, this.tcpPort,
                        this.uptimeInSeconds, this.uptimeDisplay, this.connectedClients, this.processId);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.ServerInfo.ServerInfoBuilder(version=" + this.version + ", mode="
                        + this.mode + ", os=" + this.os + ", tcpPort=" + this.tcpPort + ", uptimeInSeconds="
                        + this.uptimeInSeconds + ", uptimeDisplay=" + this.uptimeDisplay + ", connectedClients="
                        + this.connectedClients + ", processId=" + this.processId + ")";
            }

        }

    }

    public static class MemoryInfo {

        /**
         * 已用内存（字节）
         */
        private long usedMemory;

        /**
         * 已用内存（可读格式）
         */
        private String usedMemoryHuman;

        /**
         * 最大内存（字节）
         */
        private long maxMemory;

        /**
         * 最大内存（可读格式）
         */
        private String maxMemoryHuman;

        /**
         * 内存使用率（百分比）
         */
        private double memoryUsageRate;

        /**
         * 内存监控状态（UP/WARN/DOWN）
         */
        private String status;

        /**
         * 状态标签（从字典获取）
         */
        private String statusLabel;

        /**
         * 内存碎片率
         */
        private double fragmentationRatio;

        /**
         * 警告阈值（从配置读取）
         */
        private int warnThreshold;

        /**
         * 异常阈值（从配置读取）
         */
        private int errorThreshold;

        @SuppressWarnings("all")
        public MemoryInfo() {
        }

        /**
         * Creates a new {@code MemoryInfo} instance.
         *
         * @param usedMemory         已用内存（字节）
         * @param usedMemoryHuman    已用内存（可读格式）
         * @param maxMemory          最大内存（字节）
         * @param maxMemoryHuman     最大内存（可读格式）
         * @param memoryUsageRate    内存使用率（百分比）
         * @param status             内存监控状态（UP/WARN/DOWN）
         * @param statusLabel        状态标签（从字典获取）
         * @param fragmentationRatio 内存碎片率
         * @param warnThreshold      警告阈值（从配置读取）
         * @param errorThreshold     异常阈值（从配置读取）
         */
        @SuppressWarnings("all")
        public MemoryInfo(final long usedMemory, final String usedMemoryHuman, final long maxMemory,
                          final String maxMemoryHuman, final double memoryUsageRate, final String status,
                          final String statusLabel, final double fragmentationRatio, final int warnThreshold,
                          final int errorThreshold) {
            this.usedMemory = usedMemory;
            this.usedMemoryHuman = usedMemoryHuman;
            this.maxMemory = maxMemory;
            this.maxMemoryHuman = maxMemoryHuman;
            this.memoryUsageRate = memoryUsageRate;
            this.status = status;
            this.statusLabel = statusLabel;
            this.fragmentationRatio = fragmentationRatio;
            this.warnThreshold = warnThreshold;
            this.errorThreshold = errorThreshold;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder builder() {
            return new RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder();
        }

        /**
         * 已用内存（字节）
         */
        @SuppressWarnings("all")
        public long getUsedMemory() {
            return this.usedMemory;
        }

        /**
         * 已用内存（字节）
         */
        @SuppressWarnings("all")
        public void setUsedMemory(final long usedMemory) {
            this.usedMemory = usedMemory;
        }

        /**
         * 已用内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getUsedMemoryHuman() {
            return this.usedMemoryHuman;
        }

        /**
         * 已用内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setUsedMemoryHuman(final String usedMemoryHuman) {
            this.usedMemoryHuman = usedMemoryHuman;
        }

        /**
         * 最大内存（字节）
         */
        @SuppressWarnings("all")
        public long getMaxMemory() {
            return this.maxMemory;
        }

        /**
         * 最大内存（字节）
         */
        @SuppressWarnings("all")
        public void setMaxMemory(final long maxMemory) {
            this.maxMemory = maxMemory;
        }

        /**
         * 最大内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getMaxMemoryHuman() {
            return this.maxMemoryHuman;
        }

        /**
         * 最大内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setMaxMemoryHuman(final String maxMemoryHuman) {
            this.maxMemoryHuman = maxMemoryHuman;
        }

        /**
         * 内存使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getMemoryUsageRate() {
            return this.memoryUsageRate;
        }

        /**
         * 内存使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setMemoryUsageRate(final double memoryUsageRate) {
            this.memoryUsageRate = memoryUsageRate;
        }

        /**
         * 内存监控状态（UP/WARN/DOWN）
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 内存监控状态（UP/WARN/DOWN）
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        /**
         * 状态标签（从字典获取）
         */
        @SuppressWarnings("all")
        public String getStatusLabel() {
            return this.statusLabel;
        }

        /**
         * 状态标签（从字典获取）
         */
        @SuppressWarnings("all")
        public void setStatusLabel(final String statusLabel) {
            this.statusLabel = statusLabel;
        }

        /**
         * 内存碎片率
         */
        @SuppressWarnings("all")
        public double getFragmentationRatio() {
            return this.fragmentationRatio;
        }

        /**
         * 内存碎片率
         */
        @SuppressWarnings("all")
        public void setFragmentationRatio(final double fragmentationRatio) {
            this.fragmentationRatio = fragmentationRatio;
        }

        /**
         * 警告阈值（从配置读取）
         */
        @SuppressWarnings("all")
        public int getWarnThreshold() {
            return this.warnThreshold;
        }

        /**
         * 警告阈值（从配置读取）
         */
        @SuppressWarnings("all")
        public void setWarnThreshold(final int warnThreshold) {
            this.warnThreshold = warnThreshold;
        }

        /**
         * 异常阈值（从配置读取）
         */
        @SuppressWarnings("all")
        public int getErrorThreshold() {
            return this.errorThreshold;
        }

        /**
         * 异常阈值（从配置读取）
         */
        @SuppressWarnings("all")
        public void setErrorThreshold(final int errorThreshold) {
            this.errorThreshold = errorThreshold;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.MemoryInfo))
                return false;
            final RedisMonitorResponse.MemoryInfo other = (RedisMonitorResponse.MemoryInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getUsedMemory() != other.getUsedMemory())
                return false;
            if (this.getMaxMemory() != other.getMaxMemory())
                return false;
            if (java.lang.Double.compare(this.getMemoryUsageRate(), other.getMemoryUsageRate()) != 0)
                return false;
            if (java.lang.Double.compare(this.getFragmentationRatio(), other.getFragmentationRatio()) != 0)
                return false;
            if (this.getWarnThreshold() != other.getWarnThreshold())
                return false;
            if (this.getErrorThreshold() != other.getErrorThreshold())
                return false;
            final java.lang.Object this$usedMemoryHuman = this.getUsedMemoryHuman();
            final java.lang.Object other$usedMemoryHuman = other.getUsedMemoryHuman();
            if (this$usedMemoryHuman == null ? other$usedMemoryHuman != null
                    : !this$usedMemoryHuman.equals(other$usedMemoryHuman))
                return false;
            final java.lang.Object this$maxMemoryHuman = this.getMaxMemoryHuman();
            final java.lang.Object other$maxMemoryHuman = other.getMaxMemoryHuman();
            if (this$maxMemoryHuman == null ? other$maxMemoryHuman != null
                    : !this$maxMemoryHuman.equals(other$maxMemoryHuman))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            final java.lang.Object this$statusLabel = this.getStatusLabel();
            final java.lang.Object other$statusLabel = other.getStatusLabel();
            if (this$statusLabel == null ? other$statusLabel != null : !this$statusLabel.equals(other$statusLabel))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.MemoryInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $usedMemory = this.getUsedMemory();
            result = result * PRIME + (int) ($usedMemory >>> 32 ^ $usedMemory);
            final long $maxMemory = this.getMaxMemory();
            result = result * PRIME + (int) ($maxMemory >>> 32 ^ $maxMemory);
            final long $memoryUsageRate = java.lang.Double.doubleToLongBits(this.getMemoryUsageRate());
            result = result * PRIME + (int) ($memoryUsageRate >>> 32 ^ $memoryUsageRate);
            final long $fragmentationRatio = java.lang.Double.doubleToLongBits(this.getFragmentationRatio());
            result = result * PRIME + (int) ($fragmentationRatio >>> 32 ^ $fragmentationRatio);
            result = result * PRIME + this.getWarnThreshold();
            result = result * PRIME + this.getErrorThreshold();
            final java.lang.Object $usedMemoryHuman = this.getUsedMemoryHuman();
            result = result * PRIME + ($usedMemoryHuman == null ? 43 : $usedMemoryHuman.hashCode());
            final java.lang.Object $maxMemoryHuman = this.getMaxMemoryHuman();
            result = result * PRIME + ($maxMemoryHuman == null ? 43 : $maxMemoryHuman.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            final java.lang.Object $statusLabel = this.getStatusLabel();
            result = result * PRIME + ($statusLabel == null ? 43 : $statusLabel.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.MemoryInfo(usedMemory=" + this.getUsedMemory() + ", usedMemoryHuman="
                    + this.getUsedMemoryHuman() + ", maxMemory=" + this.getMaxMemory() + ", maxMemoryHuman="
                    + this.getMaxMemoryHuman() + ", memoryUsageRate=" + this.getMemoryUsageRate() + ", status="
                    + this.getStatus() + ", statusLabel=" + this.getStatusLabel() + ", fragmentationRatio="
                    + this.getFragmentationRatio() + ", warnThreshold=" + this.getWarnThreshold() + ", errorThreshold="
                    + this.getErrorThreshold() + ")";
        }

        @SuppressWarnings("all")
        public static class MemoryInfoBuilder {

            @SuppressWarnings("all")
            private long usedMemory;

            @SuppressWarnings("all")
            private String usedMemoryHuman;

            @SuppressWarnings("all")
            private long maxMemory;

            @SuppressWarnings("all")
            private String maxMemoryHuman;

            @SuppressWarnings("all")
            private double memoryUsageRate;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            private String statusLabel;

            @SuppressWarnings("all")
            private double fragmentationRatio;

            @SuppressWarnings("all")
            private int warnThreshold;

            @SuppressWarnings("all")
            private int errorThreshold;

            @SuppressWarnings("all")
            MemoryInfoBuilder() {
            }

            /**
             * 已用内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder usedMemory(final long usedMemory) {
                this.usedMemory = usedMemory;
                return this;
            }

            /**
             * 已用内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder usedMemoryHuman(final String usedMemoryHuman) {
                this.usedMemoryHuman = usedMemoryHuman;
                return this;
            }

            /**
             * 最大内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder maxMemory(final long maxMemory) {
                this.maxMemory = maxMemory;
                return this;
            }

            /**
             * 最大内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder maxMemoryHuman(final String maxMemoryHuman) {
                this.maxMemoryHuman = maxMemoryHuman;
                return this;
            }

            /**
             * 内存使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder memoryUsageRate(final double memoryUsageRate) {
                this.memoryUsageRate = memoryUsageRate;
                return this;
            }

            /**
             * 内存监控状态（UP/WARN/DOWN）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            /**
             * 状态标签（从字典获取）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder statusLabel(final String statusLabel) {
                this.statusLabel = statusLabel;
                return this;
            }

            /**
             * 内存碎片率
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder fragmentationRatio(
                    final double fragmentationRatio) {
                this.fragmentationRatio = fragmentationRatio;
                return this;
            }

            /**
             * 警告阈值（从配置读取）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder warnThreshold(final int warnThreshold) {
                this.warnThreshold = warnThreshold;
                return this;
            }

            /**
             * 异常阈值（从配置读取）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder errorThreshold(final int errorThreshold) {
                this.errorThreshold = errorThreshold;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.MemoryInfo build() {
                return new RedisMonitorResponse.MemoryInfo(this.usedMemory, this.usedMemoryHuman, this.maxMemory,
                        this.maxMemoryHuman, this.memoryUsageRate, this.status, this.statusLabel,
                        this.fragmentationRatio, this.warnThreshold, this.errorThreshold);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.MemoryInfo.MemoryInfoBuilder(usedMemory=" + this.usedMemory
                        + ", usedMemoryHuman=" + this.usedMemoryHuman + ", maxMemory=" + this.maxMemory
                        + ", maxMemoryHuman=" + this.maxMemoryHuman + ", memoryUsageRate=" + this.memoryUsageRate
                        + ", status=" + this.status + ", statusLabel=" + this.statusLabel + ", fragmentationRatio="
                        + this.fragmentationRatio + ", warnThreshold=" + this.warnThreshold + ", errorThreshold="
                        + this.errorThreshold + ")";
            }

        }

    }

    public static class StatsInfo {

        /**
         * 总连接数
         */
        private long totalConnectionsReceived;

        /**
         * 总命令处理数
         */
        private long totalCommandsProcessed;

        /**
         * 每秒操作数（QPS）
         */
        private long instantaneousOpsPerSec;

        /**
         * 总网络输入字节
         */
        private long totalNetInputBytes;

        /**
         * 总网络输出字节
         */
        private long totalNetOutputBytes;

        /**
         * 键空间命中数
         */
        private long keyspaceHits;

        /**
         * 键空间未命中数
         */
        private long keyspaceMisses;

        /**
         * 命中率（百分比）
         */
        private double hitRate;

        /**
         * 已过期键数
         */
        private long expiredKeys;

        /**
         * 已驱逐键数
         */
        private long evictedKeys;

        @SuppressWarnings("all")
        public StatsInfo() {
        }

        /**
         * Creates a new {@code StatsInfo} instance.
         *
         * @param totalConnectionsReceived 总连接数
         * @param totalCommandsProcessed   总命令处理数
         * @param instantaneousOpsPerSec   每秒操作数（QPS）
         * @param totalNetInputBytes       总网络输入字节
         * @param totalNetOutputBytes      总网络输出字节
         * @param keyspaceHits             键空间命中数
         * @param keyspaceMisses           键空间未命中数
         * @param hitRate                  命中率（百分比）
         * @param expiredKeys              已过期键数
         * @param evictedKeys              已驱逐键数
         */
        @SuppressWarnings("all")
        public StatsInfo(final long totalConnectionsReceived, final long totalCommandsProcessed,
                         final long instantaneousOpsPerSec, final long totalNetInputBytes, final long totalNetOutputBytes,
                         final long keyspaceHits, final long keyspaceMisses, final double hitRate, final long expiredKeys,
                         final long evictedKeys) {
            this.totalConnectionsReceived = totalConnectionsReceived;
            this.totalCommandsProcessed = totalCommandsProcessed;
            this.instantaneousOpsPerSec = instantaneousOpsPerSec;
            this.totalNetInputBytes = totalNetInputBytes;
            this.totalNetOutputBytes = totalNetOutputBytes;
            this.keyspaceHits = keyspaceHits;
            this.keyspaceMisses = keyspaceMisses;
            this.hitRate = hitRate;
            this.expiredKeys = expiredKeys;
            this.evictedKeys = evictedKeys;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.StatsInfo.StatsInfoBuilder builder() {
            return new RedisMonitorResponse.StatsInfo.StatsInfoBuilder();
        }

        /**
         * 总连接数
         */
        @SuppressWarnings("all")
        public long getTotalConnectionsReceived() {
            return this.totalConnectionsReceived;
        }

        /**
         * 总连接数
         */
        @SuppressWarnings("all")
        public void setTotalConnectionsReceived(final long totalConnectionsReceived) {
            this.totalConnectionsReceived = totalConnectionsReceived;
        }

        /**
         * 总命令处理数
         */
        @SuppressWarnings("all")
        public long getTotalCommandsProcessed() {
            return this.totalCommandsProcessed;
        }

        /**
         * 总命令处理数
         */
        @SuppressWarnings("all")
        public void setTotalCommandsProcessed(final long totalCommandsProcessed) {
            this.totalCommandsProcessed = totalCommandsProcessed;
        }

        /**
         * 每秒操作数（QPS）
         */
        @SuppressWarnings("all")
        public long getInstantaneousOpsPerSec() {
            return this.instantaneousOpsPerSec;
        }

        /**
         * 每秒操作数（QPS）
         */
        @SuppressWarnings("all")
        public void setInstantaneousOpsPerSec(final long instantaneousOpsPerSec) {
            this.instantaneousOpsPerSec = instantaneousOpsPerSec;
        }

        /**
         * 总网络输入字节
         */
        @SuppressWarnings("all")
        public long getTotalNetInputBytes() {
            return this.totalNetInputBytes;
        }

        /**
         * 总网络输入字节
         */
        @SuppressWarnings("all")
        public void setTotalNetInputBytes(final long totalNetInputBytes) {
            this.totalNetInputBytes = totalNetInputBytes;
        }

        /**
         * 总网络输出字节
         */
        @SuppressWarnings("all")
        public long getTotalNetOutputBytes() {
            return this.totalNetOutputBytes;
        }

        /**
         * 总网络输出字节
         */
        @SuppressWarnings("all")
        public void setTotalNetOutputBytes(final long totalNetOutputBytes) {
            this.totalNetOutputBytes = totalNetOutputBytes;
        }

        /**
         * 键空间命中数
         */
        @SuppressWarnings("all")
        public long getKeyspaceHits() {
            return this.keyspaceHits;
        }

        /**
         * 键空间命中数
         */
        @SuppressWarnings("all")
        public void setKeyspaceHits(final long keyspaceHits) {
            this.keyspaceHits = keyspaceHits;
        }

        /**
         * 键空间未命中数
         */
        @SuppressWarnings("all")
        public long getKeyspaceMisses() {
            return this.keyspaceMisses;
        }

        /**
         * 键空间未命中数
         */
        @SuppressWarnings("all")
        public void setKeyspaceMisses(final long keyspaceMisses) {
            this.keyspaceMisses = keyspaceMisses;
        }

        /**
         * 命中率（百分比）
         */
        @SuppressWarnings("all")
        public double getHitRate() {
            return this.hitRate;
        }

        /**
         * 命中率（百分比）
         */
        @SuppressWarnings("all")
        public void setHitRate(final double hitRate) {
            this.hitRate = hitRate;
        }

        /**
         * 已过期键数
         */
        @SuppressWarnings("all")
        public long getExpiredKeys() {
            return this.expiredKeys;
        }

        /**
         * 已过期键数
         */
        @SuppressWarnings("all")
        public void setExpiredKeys(final long expiredKeys) {
            this.expiredKeys = expiredKeys;
        }

        /**
         * 已驱逐键数
         */
        @SuppressWarnings("all")
        public long getEvictedKeys() {
            return this.evictedKeys;
        }

        /**
         * 已驱逐键数
         */
        @SuppressWarnings("all")
        public void setEvictedKeys(final long evictedKeys) {
            this.evictedKeys = evictedKeys;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.StatsInfo))
                return false;
            final RedisMonitorResponse.StatsInfo other = (RedisMonitorResponse.StatsInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTotalConnectionsReceived() != other.getTotalConnectionsReceived())
                return false;
            if (this.getTotalCommandsProcessed() != other.getTotalCommandsProcessed())
                return false;
            if (this.getInstantaneousOpsPerSec() != other.getInstantaneousOpsPerSec())
                return false;
            if (this.getTotalNetInputBytes() != other.getTotalNetInputBytes())
                return false;
            if (this.getTotalNetOutputBytes() != other.getTotalNetOutputBytes())
                return false;
            if (this.getKeyspaceHits() != other.getKeyspaceHits())
                return false;
            if (this.getKeyspaceMisses() != other.getKeyspaceMisses())
                return false;
            if (java.lang.Double.compare(this.getHitRate(), other.getHitRate()) != 0)
                return false;
            if (this.getExpiredKeys() != other.getExpiredKeys())
                return false;
            if (this.getEvictedKeys() != other.getEvictedKeys())
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.StatsInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $totalConnectionsReceived = this.getTotalConnectionsReceived();
            result = result * PRIME + (int) ($totalConnectionsReceived >>> 32 ^ $totalConnectionsReceived);
            final long $totalCommandsProcessed = this.getTotalCommandsProcessed();
            result = result * PRIME + (int) ($totalCommandsProcessed >>> 32 ^ $totalCommandsProcessed);
            final long $instantaneousOpsPerSec = this.getInstantaneousOpsPerSec();
            result = result * PRIME + (int) ($instantaneousOpsPerSec >>> 32 ^ $instantaneousOpsPerSec);
            final long $totalNetInputBytes = this.getTotalNetInputBytes();
            result = result * PRIME + (int) ($totalNetInputBytes >>> 32 ^ $totalNetInputBytes);
            final long $totalNetOutputBytes = this.getTotalNetOutputBytes();
            result = result * PRIME + (int) ($totalNetOutputBytes >>> 32 ^ $totalNetOutputBytes);
            final long $keyspaceHits = this.getKeyspaceHits();
            result = result * PRIME + (int) ($keyspaceHits >>> 32 ^ $keyspaceHits);
            final long $keyspaceMisses = this.getKeyspaceMisses();
            result = result * PRIME + (int) ($keyspaceMisses >>> 32 ^ $keyspaceMisses);
            final long $hitRate = java.lang.Double.doubleToLongBits(this.getHitRate());
            result = result * PRIME + (int) ($hitRate >>> 32 ^ $hitRate);
            final long $expiredKeys = this.getExpiredKeys();
            result = result * PRIME + (int) ($expiredKeys >>> 32 ^ $expiredKeys);
            final long $evictedKeys = this.getEvictedKeys();
            result = result * PRIME + (int) ($evictedKeys >>> 32 ^ $evictedKeys);
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.StatsInfo(totalConnectionsReceived=" + this.getTotalConnectionsReceived()
                    + ", totalCommandsProcessed=" + this.getTotalCommandsProcessed() + ", instantaneousOpsPerSec="
                    + this.getInstantaneousOpsPerSec() + ", totalNetInputBytes=" + this.getTotalNetInputBytes()
                    + ", totalNetOutputBytes=" + this.getTotalNetOutputBytes() + ", keyspaceHits="
                    + this.getKeyspaceHits() + ", keyspaceMisses=" + this.getKeyspaceMisses() + ", hitRate="
                    + this.getHitRate() + ", expiredKeys=" + this.getExpiredKeys() + ", evictedKeys="
                    + this.getEvictedKeys() + ")";
        }

        @SuppressWarnings("all")
        public static class StatsInfoBuilder {

            @SuppressWarnings("all")
            private long totalConnectionsReceived;

            @SuppressWarnings("all")
            private long totalCommandsProcessed;

            @SuppressWarnings("all")
            private long instantaneousOpsPerSec;

            @SuppressWarnings("all")
            private long totalNetInputBytes;

            @SuppressWarnings("all")
            private long totalNetOutputBytes;

            @SuppressWarnings("all")
            private long keyspaceHits;

            @SuppressWarnings("all")
            private long keyspaceMisses;

            @SuppressWarnings("all")
            private double hitRate;

            @SuppressWarnings("all")
            private long expiredKeys;

            @SuppressWarnings("all")
            private long evictedKeys;

            @SuppressWarnings("all")
            StatsInfoBuilder() {
            }

            /**
             * 总连接数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder totalConnectionsReceived(
                    final long totalConnectionsReceived) {
                this.totalConnectionsReceived = totalConnectionsReceived;
                return this;
            }

            /**
             * 总命令处理数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder totalCommandsProcessed(
                    final long totalCommandsProcessed) {
                this.totalCommandsProcessed = totalCommandsProcessed;
                return this;
            }

            /**
             * 每秒操作数（QPS）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder instantaneousOpsPerSec(
                    final long instantaneousOpsPerSec) {
                this.instantaneousOpsPerSec = instantaneousOpsPerSec;
                return this;
            }

            /**
             * 总网络输入字节
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder totalNetInputBytes(final long totalNetInputBytes) {
                this.totalNetInputBytes = totalNetInputBytes;
                return this;
            }

            /**
             * 总网络输出字节
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder totalNetOutputBytes(final long totalNetOutputBytes) {
                this.totalNetOutputBytes = totalNetOutputBytes;
                return this;
            }

            /**
             * 键空间命中数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder keyspaceHits(final long keyspaceHits) {
                this.keyspaceHits = keyspaceHits;
                return this;
            }

            /**
             * 键空间未命中数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder keyspaceMisses(final long keyspaceMisses) {
                this.keyspaceMisses = keyspaceMisses;
                return this;
            }

            /**
             * 命中率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder hitRate(final double hitRate) {
                this.hitRate = hitRate;
                return this;
            }

            /**
             * 已过期键数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder expiredKeys(final long expiredKeys) {
                this.expiredKeys = expiredKeys;
                return this;
            }

            /**
             * 已驱逐键数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo.StatsInfoBuilder evictedKeys(final long evictedKeys) {
                this.evictedKeys = evictedKeys;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.StatsInfo build() {
                return new RedisMonitorResponse.StatsInfo(this.totalConnectionsReceived, this.totalCommandsProcessed,
                        this.instantaneousOpsPerSec, this.totalNetInputBytes, this.totalNetOutputBytes,
                        this.keyspaceHits, this.keyspaceMisses, this.hitRate, this.expiredKeys, this.evictedKeys);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.StatsInfo.StatsInfoBuilder(totalConnectionsReceived="
                        + this.totalConnectionsReceived + ", totalCommandsProcessed=" + this.totalCommandsProcessed
                        + ", instantaneousOpsPerSec=" + this.instantaneousOpsPerSec + ", totalNetInputBytes="
                        + this.totalNetInputBytes + ", totalNetOutputBytes=" + this.totalNetOutputBytes
                        + ", keyspaceHits=" + this.keyspaceHits + ", keyspaceMisses=" + this.keyspaceMisses
                        + ", hitRate=" + this.hitRate + ", expiredKeys=" + this.expiredKeys + ", evictedKeys="
                        + this.evictedKeys + ")";
            }

        }

    }

    public static class KeyspaceInfo {

        /**
         * 数据库索引
         */
        private int dbIndex;

        /**
         * 键总数
         */
        private long keys;

        /**
         * 设置过期的键数
         */
        private long expires;

        /**
         * 平均 TTL（毫秒）
         */
        private long avgTtl;

        @SuppressWarnings("all")
        public KeyspaceInfo() {
        }

        /**
         * Creates a new {@code KeyspaceInfo} instance.
         *
         * @param dbIndex 数据库索引
         * @param keys    键总数
         * @param expires 设置过期的键数
         * @param avgTtl  平均 TTL（毫秒）
         */
        @SuppressWarnings("all")
        public KeyspaceInfo(final int dbIndex, final long keys, final long expires, final long avgTtl) {
            this.dbIndex = dbIndex;
            this.keys = keys;
            this.expires = expires;
            this.avgTtl = avgTtl;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder builder() {
            return new RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder();
        }

        /**
         * 数据库索引
         */
        @SuppressWarnings("all")
        public int getDbIndex() {
            return this.dbIndex;
        }

        /**
         * 数据库索引
         */
        @SuppressWarnings("all")
        public void setDbIndex(final int dbIndex) {
            this.dbIndex = dbIndex;
        }

        /**
         * 键总数
         */
        @SuppressWarnings("all")
        public long getKeys() {
            return this.keys;
        }

        /**
         * 键总数
         */
        @SuppressWarnings("all")
        public void setKeys(final long keys) {
            this.keys = keys;
        }

        /**
         * 设置过期的键数
         */
        @SuppressWarnings("all")
        public long getExpires() {
            return this.expires;
        }

        /**
         * 设置过期的键数
         */
        @SuppressWarnings("all")
        public void setExpires(final long expires) {
            this.expires = expires;
        }

        /**
         * 平均 TTL（毫秒）
         */
        @SuppressWarnings("all")
        public long getAvgTtl() {
            return this.avgTtl;
        }

        /**
         * 平均 TTL（毫秒）
         */
        @SuppressWarnings("all")
        public void setAvgTtl(final long avgTtl) {
            this.avgTtl = avgTtl;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.KeyspaceInfo))
                return false;
            final RedisMonitorResponse.KeyspaceInfo other = (RedisMonitorResponse.KeyspaceInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getDbIndex() != other.getDbIndex())
                return false;
            if (this.getKeys() != other.getKeys())
                return false;
            if (this.getExpires() != other.getExpires())
                return false;
            if (this.getAvgTtl() != other.getAvgTtl())
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.KeyspaceInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getDbIndex();
            final long $keys = this.getKeys();
            result = result * PRIME + (int) ($keys >>> 32 ^ $keys);
            final long $expires = this.getExpires();
            result = result * PRIME + (int) ($expires >>> 32 ^ $expires);
            final long $avgTtl = this.getAvgTtl();
            result = result * PRIME + (int) ($avgTtl >>> 32 ^ $avgTtl);
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.KeyspaceInfo(dbIndex=" + this.getDbIndex() + ", keys=" + this.getKeys()
                    + ", expires=" + this.getExpires() + ", avgTtl=" + this.getAvgTtl() + ")";
        }

        @SuppressWarnings("all")
        public static class KeyspaceInfoBuilder {

            @SuppressWarnings("all")
            private int dbIndex;

            @SuppressWarnings("all")
            private long keys;

            @SuppressWarnings("all")
            private long expires;

            @SuppressWarnings("all")
            private long avgTtl;

            @SuppressWarnings("all")
            KeyspaceInfoBuilder() {
            }

            /**
             * 数据库索引
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder dbIndex(final int dbIndex) {
                this.dbIndex = dbIndex;
                return this;
            }

            /**
             * 键总数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder keys(final long keys) {
                this.keys = keys;
                return this;
            }

            /**
             * 设置过期的键数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder expires(final long expires) {
                this.expires = expires;
                return this;
            }

            /**
             * 平均 TTL（毫秒）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder avgTtl(final long avgTtl) {
                this.avgTtl = avgTtl;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.KeyspaceInfo build() {
                return new RedisMonitorResponse.KeyspaceInfo(this.dbIndex, this.keys, this.expires, this.avgTtl);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.KeyspaceInfo.KeyspaceInfoBuilder(dbIndex=" + this.dbIndex + ", keys="
                        + this.keys + ", expires=" + this.expires + ", avgTtl=" + this.avgTtl + ")";
            }

        }

    }

    public static class CommandStat {

        /**
         * 命令名
         */
        private String command;

        /**
         * 调用次数
         */
        private long calls;

        /**
         * 总耗时（微秒）
         */
        private long usec;

        /**
         * 平均耗时（微秒/次）
         */
        private double usecPerCall;

        @SuppressWarnings("all")
        public CommandStat() {
        }

        /**
         * Creates a new {@code CommandStat} instance.
         *
         * @param command     命令名
         * @param calls       调用次数
         * @param usec        总耗时（微秒）
         * @param usecPerCall 平均耗时（微秒/次）
         */
        @SuppressWarnings("all")
        public CommandStat(final String command, final long calls, final long usec, final double usecPerCall) {
            this.command = command;
            this.calls = calls;
            this.usec = usec;
            this.usecPerCall = usecPerCall;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.CommandStat.CommandStatBuilder builder() {
            return new RedisMonitorResponse.CommandStat.CommandStatBuilder();
        }

        /**
         * 命令名
         */
        @SuppressWarnings("all")
        public String getCommand() {
            return this.command;
        }

        /**
         * 命令名
         */
        @SuppressWarnings("all")
        public void setCommand(final String command) {
            this.command = command;
        }

        /**
         * 调用次数
         */
        @SuppressWarnings("all")
        public long getCalls() {
            return this.calls;
        }

        /**
         * 调用次数
         */
        @SuppressWarnings("all")
        public void setCalls(final long calls) {
            this.calls = calls;
        }

        /**
         * 总耗时（微秒）
         */
        @SuppressWarnings("all")
        public long getUsec() {
            return this.usec;
        }

        /**
         * 总耗时（微秒）
         */
        @SuppressWarnings("all")
        public void setUsec(final long usec) {
            this.usec = usec;
        }

        /**
         * 平均耗时（微秒/次）
         */
        @SuppressWarnings("all")
        public double getUsecPerCall() {
            return this.usecPerCall;
        }

        /**
         * 平均耗时（微秒/次）
         */
        @SuppressWarnings("all")
        public void setUsecPerCall(final double usecPerCall) {
            this.usecPerCall = usecPerCall;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.CommandStat))
                return false;
            final RedisMonitorResponse.CommandStat other = (RedisMonitorResponse.CommandStat) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getCalls() != other.getCalls())
                return false;
            if (this.getUsec() != other.getUsec())
                return false;
            if (java.lang.Double.compare(this.getUsecPerCall(), other.getUsecPerCall()) != 0)
                return false;
            final java.lang.Object this$command = this.getCommand();
            final java.lang.Object other$command = other.getCommand();
            if (this$command == null ? other$command != null : !this$command.equals(other$command))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.CommandStat;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $calls = this.getCalls();
            result = result * PRIME + (int) ($calls >>> 32 ^ $calls);
            final long $usec = this.getUsec();
            result = result * PRIME + (int) ($usec >>> 32 ^ $usec);
            final long $usecPerCall = java.lang.Double.doubleToLongBits(this.getUsecPerCall());
            result = result * PRIME + (int) ($usecPerCall >>> 32 ^ $usecPerCall);
            final java.lang.Object $command = this.getCommand();
            result = result * PRIME + ($command == null ? 43 : $command.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.CommandStat(command=" + this.getCommand() + ", calls=" + this.getCalls()
                    + ", usec=" + this.getUsec() + ", usecPerCall=" + this.getUsecPerCall() + ")";
        }

        @SuppressWarnings("all")
        public static class CommandStatBuilder {

            @SuppressWarnings("all")
            private String command;

            @SuppressWarnings("all")
            private long calls;

            @SuppressWarnings("all")
            private long usec;

            @SuppressWarnings("all")
            private double usecPerCall;

            @SuppressWarnings("all")
            CommandStatBuilder() {
            }

            /**
             * 命令名
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CommandStat.CommandStatBuilder command(final String command) {
                this.command = command;
                return this;
            }

            /**
             * 调用次数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CommandStat.CommandStatBuilder calls(final long calls) {
                this.calls = calls;
                return this;
            }

            /**
             * 总耗时（微秒）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CommandStat.CommandStatBuilder usec(final long usec) {
                this.usec = usec;
                return this;
            }

            /**
             * 平均耗时（微秒/次）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CommandStat.CommandStatBuilder usecPerCall(final double usecPerCall) {
                this.usecPerCall = usecPerCall;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.CommandStat build() {
                return new RedisMonitorResponse.CommandStat(this.command, this.calls, this.usec, this.usecPerCall);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.CommandStat.CommandStatBuilder(command=" + this.command + ", calls="
                        + this.calls + ", usec=" + this.usec + ", usecPerCall=" + this.usecPerCall + ")";
            }

        }

    }

    public static class CacheKeyInfo {

        /**
         * 键名
         */
        private String key;

        /**
         * 类型（string/hash/list/set/zset）
         */
        private String type;

        /**
         * 剩余 TTL（秒，-1 永久，-2 不存在）
         */
        private long ttl;

        /**
         * TTL 显示（可读格式）
         */
        private String ttlDisplay;

        @SuppressWarnings("all")
        public CacheKeyInfo() {
        }

        /**
         * Creates a new {@code CacheKeyInfo} instance.
         *
         * @param key        键名
         * @param type       类型（string/hash/list/set/zset）
         * @param ttl        剩余 TTL（秒，-1 永久，-2 不存在）
         * @param ttlDisplay TTL 显示（可读格式）
         */
        @SuppressWarnings("all")
        public CacheKeyInfo(final String key, final String type, final long ttl, final String ttlDisplay) {
            this.key = key;
            this.type = type;
            this.ttl = ttl;
            this.ttlDisplay = ttlDisplay;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder builder() {
            return new RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder();
        }

        /**
         * 键名
         */
        @SuppressWarnings("all")
        public String getKey() {
            return this.key;
        }

        /**
         * 键名
         */
        @SuppressWarnings("all")
        public void setKey(final String key) {
            this.key = key;
        }

        /**
         * 类型（string/hash/list/set/zset）
         */
        @SuppressWarnings("all")
        public String getType() {
            return this.type;
        }

        /**
         * 类型（string/hash/list/set/zset）
         */
        @SuppressWarnings("all")
        public void setType(final String type) {
            this.type = type;
        }

        /**
         * 剩余 TTL（秒，-1 永久，-2 不存在）
         */
        @SuppressWarnings("all")
        public long getTtl() {
            return this.ttl;
        }

        /**
         * 剩余 TTL（秒，-1 永久，-2 不存在）
         */
        @SuppressWarnings("all")
        public void setTtl(final long ttl) {
            this.ttl = ttl;
        }

        /**
         * TTL 显示（可读格式）
         */
        @SuppressWarnings("all")
        public String getTtlDisplay() {
            return this.ttlDisplay;
        }

        /**
         * TTL 显示（可读格式）
         */
        @SuppressWarnings("all")
        public void setTtlDisplay(final String ttlDisplay) {
            this.ttlDisplay = ttlDisplay;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.CacheKeyInfo))
                return false;
            final RedisMonitorResponse.CacheKeyInfo other = (RedisMonitorResponse.CacheKeyInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTtl() != other.getTtl())
                return false;
            final java.lang.Object this$key = this.getKey();
            final java.lang.Object other$key = other.getKey();
            if (this$key == null ? other$key != null : !this$key.equals(other$key))
                return false;
            final java.lang.Object this$type = this.getType();
            final java.lang.Object other$type = other.getType();
            if (this$type == null ? other$type != null : !this$type.equals(other$type))
                return false;
            final java.lang.Object this$ttlDisplay = this.getTtlDisplay();
            final java.lang.Object other$ttlDisplay = other.getTtlDisplay();
            if (this$ttlDisplay == null ? other$ttlDisplay != null : !this$ttlDisplay.equals(other$ttlDisplay))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.CacheKeyInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $ttl = this.getTtl();
            result = result * PRIME + (int) ($ttl >>> 32 ^ $ttl);
            final java.lang.Object $key = this.getKey();
            result = result * PRIME + ($key == null ? 43 : $key.hashCode());
            final java.lang.Object $type = this.getType();
            result = result * PRIME + ($type == null ? 43 : $type.hashCode());
            final java.lang.Object $ttlDisplay = this.getTtlDisplay();
            result = result * PRIME + ($ttlDisplay == null ? 43 : $ttlDisplay.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.CacheKeyInfo(key=" + this.getKey() + ", type=" + this.getType() + ", ttl="
                    + this.getTtl() + ", ttlDisplay=" + this.getTtlDisplay() + ")";
        }

        @SuppressWarnings("all")
        public static class CacheKeyInfoBuilder {

            @SuppressWarnings("all")
            private String key;

            @SuppressWarnings("all")
            private String type;

            @SuppressWarnings("all")
            private long ttl;

            @SuppressWarnings("all")
            private String ttlDisplay;

            @SuppressWarnings("all")
            CacheKeyInfoBuilder() {
            }

            /**
             * 键名
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder key(final String key) {
                this.key = key;
                return this;
            }

            /**
             * 类型（string/hash/list/set/zset）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder type(final String type) {
                this.type = type;
                return this;
            }

            /**
             * 剩余 TTL（秒，-1 永久，-2 不存在）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder ttl(final long ttl) {
                this.ttl = ttl;
                return this;
            }

            /**
             * TTL 显示（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder ttlDisplay(final String ttlDisplay) {
                this.ttlDisplay = ttlDisplay;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyInfo build() {
                return new RedisMonitorResponse.CacheKeyInfo(this.key, this.type, this.ttl, this.ttlDisplay);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.CacheKeyInfo.CacheKeyInfoBuilder(key=" + this.key + ", type=" + this.type
                        + ", ttl=" + this.ttl + ", ttlDisplay=" + this.ttlDisplay + ")";
            }

        }

    }

    public static class CacheKeyDetail {

        /**
         * 键名
         */
        private String key;

        /**
         * 类型
         */
        private String type;

        /**
         * 剩余 TTL（秒）
         */
        private long ttl;

        /**
         * 键值内容
         */
        private Object value;

        @SuppressWarnings("all")
        public CacheKeyDetail() {
        }

        /**
         * Creates a new {@code CacheKeyDetail} instance.
         *
         * @param key   键名
         * @param type  类型
         * @param ttl   剩余 TTL（秒）
         * @param value 键值内容
         */
        @SuppressWarnings("all")
        public CacheKeyDetail(final String key, final String type, final long ttl, final Object value) {
            this.key = key;
            this.type = type;
            this.ttl = ttl;
            this.value = value;
        }

        @SuppressWarnings("all")
        public static RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder builder() {
            return new RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder();
        }

        /**
         * 键名
         */
        @SuppressWarnings("all")
        public String getKey() {
            return this.key;
        }

        /**
         * 键名
         */
        @SuppressWarnings("all")
        public void setKey(final String key) {
            this.key = key;
        }

        /**
         * 类型
         */
        @SuppressWarnings("all")
        public String getType() {
            return this.type;
        }

        /**
         * 类型
         */
        @SuppressWarnings("all")
        public void setType(final String type) {
            this.type = type;
        }

        /**
         * 剩余 TTL（秒）
         */
        @SuppressWarnings("all")
        public long getTtl() {
            return this.ttl;
        }

        /**
         * 剩余 TTL（秒）
         */
        @SuppressWarnings("all")
        public void setTtl(final long ttl) {
            this.ttl = ttl;
        }

        /**
         * 键值内容
         */
        @SuppressWarnings("all")
        public Object getValue() {
            return this.value;
        }

        /**
         * 键值内容
         */
        @SuppressWarnings("all")
        public void setValue(final Object value) {
            this.value = value;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof RedisMonitorResponse.CacheKeyDetail))
                return false;
            final RedisMonitorResponse.CacheKeyDetail other = (RedisMonitorResponse.CacheKeyDetail) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTtl() != other.getTtl())
                return false;
            final java.lang.Object this$key = this.getKey();
            final java.lang.Object other$key = other.getKey();
            if (this$key == null ? other$key != null : !this$key.equals(other$key))
                return false;
            final java.lang.Object this$type = this.getType();
            final java.lang.Object other$type = other.getType();
            if (this$type == null ? other$type != null : !this$type.equals(other$type))
                return false;
            final java.lang.Object this$value = this.getValue();
            final java.lang.Object other$value = other.getValue();
            if (this$value == null ? other$value != null : !this$value.equals(other$value))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof RedisMonitorResponse.CacheKeyDetail;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $ttl = this.getTtl();
            result = result * PRIME + (int) ($ttl >>> 32 ^ $ttl);
            final java.lang.Object $key = this.getKey();
            result = result * PRIME + ($key == null ? 43 : $key.hashCode());
            final java.lang.Object $type = this.getType();
            result = result * PRIME + ($type == null ? 43 : $type.hashCode());
            final java.lang.Object $value = this.getValue();
            result = result * PRIME + ($value == null ? 43 : $value.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.CacheKeyDetail(key=" + this.getKey() + ", type=" + this.getType() + ", ttl="
                    + this.getTtl() + ", value=" + this.getValue() + ")";
        }

        @SuppressWarnings("all")
        public static class CacheKeyDetailBuilder {

            @SuppressWarnings("all")
            private String key;

            @SuppressWarnings("all")
            private String type;

            @SuppressWarnings("all")
            private long ttl;

            @SuppressWarnings("all")
            private Object value;

            @SuppressWarnings("all")
            CacheKeyDetailBuilder() {
            }

            /**
             * 键名
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder key(final String key) {
                this.key = key;
                return this;
            }

            /**
             * 类型
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder type(final String type) {
                this.type = type;
                return this;
            }

            /**
             * 剩余 TTL（秒）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder ttl(final long ttl) {
                this.ttl = ttl;
                return this;
            }

            /**
             * 键值内容
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder value(final Object value) {
                this.value = value;
                return this;
            }

            @SuppressWarnings("all")
            public RedisMonitorResponse.CacheKeyDetail build() {
                return new RedisMonitorResponse.CacheKeyDetail(this.key, this.type, this.ttl, this.value);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "RedisMonitorResponse.CacheKeyDetail.CacheKeyDetailBuilder(key=" + this.key + ", type="
                        + this.type + ", ttl=" + this.ttl + ", value=" + this.value + ")";
            }

        }

    }

    @SuppressWarnings("all")
    public static class RedisMonitorResponseBuilder {

        @SuppressWarnings("all")
        private ServerInfo serverInfo;

        @SuppressWarnings("all")
        private MemoryInfo memoryInfo;

        @SuppressWarnings("all")
        private StatsInfo statsInfo;

        @SuppressWarnings("all")
        private List<KeyspaceInfo> keyspaceInfo;

        @SuppressWarnings("all")
        private List<CommandStat> commandStats;

        @SuppressWarnings("all")
        RedisMonitorResponseBuilder() {
        }

        /**
         * Redis 服务器信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RedisMonitorResponse.RedisMonitorResponseBuilder serverInfo(final ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
            return this;
        }

        /**
         * 内存信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RedisMonitorResponse.RedisMonitorResponseBuilder memoryInfo(final MemoryInfo memoryInfo) {
            this.memoryInfo = memoryInfo;
            return this;
        }

        /**
         * 统计信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RedisMonitorResponse.RedisMonitorResponseBuilder statsInfo(final StatsInfo statsInfo) {
            this.statsInfo = statsInfo;
            return this;
        }

        /**
         * 键空间信息（按数据库）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RedisMonitorResponse.RedisMonitorResponseBuilder keyspaceInfo(final List<KeyspaceInfo> keyspaceInfo) {
            this.keyspaceInfo = keyspaceInfo;
            return this;
        }

        /**
         * 命令统计（Top10 热点命令）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RedisMonitorResponse.RedisMonitorResponseBuilder commandStats(final List<CommandStat> commandStats) {
            this.commandStats = commandStats;
            return this;
        }

        @SuppressWarnings("all")
        public RedisMonitorResponse build() {
            return new RedisMonitorResponse(this.serverInfo, this.memoryInfo, this.statsInfo, this.keyspaceInfo,
                    this.commandStats);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RedisMonitorResponse.RedisMonitorResponseBuilder(serverInfo=" + this.serverInfo + ", memoryInfo="
                    + this.memoryInfo + ", statsInfo=" + this.statsInfo + ", keyspaceInfo=" + this.keyspaceInfo
                    + ", commandStats=" + this.commandStats + ")";
        }

    }

}
