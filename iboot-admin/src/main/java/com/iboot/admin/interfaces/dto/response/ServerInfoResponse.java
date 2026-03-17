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
 * 服务器监控信息响应
 *
 * @author iBoot
 */
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

    @SuppressWarnings("all")
    public ServerInfoResponse() {
    }

    /**
     * Creates a new {@code ServerInfoResponse} instance.
     *
     * @param cpu           CPU 信息
     * @param memory        内存信息
     * @param jvm           JVM 信息
     * @param disks         磁盘信息
     * @param os            操作系统信息
     * @param serviceChecks 服务健康检查
     * @param sbaUrl        Spring Boot Admin 控制台地址
     */
    @SuppressWarnings("all")
    public ServerInfoResponse(final CpuInfo cpu, final MemoryInfo memory, final JvmInfo jvm, final List<DiskInfo> disks,
                              final OsInfo os, final List<ServiceCheckInfo> serviceChecks, final String sbaUrl) {
        this.cpu = cpu;
        this.memory = memory;
        this.jvm = jvm;
        this.disks = disks;
        this.os = os;
        this.serviceChecks = serviceChecks;
        this.sbaUrl = sbaUrl;
    }

    @SuppressWarnings("all")
    public static ServerInfoResponse.ServerInfoResponseBuilder builder() {
        return new ServerInfoResponse.ServerInfoResponseBuilder();
    }

    /**
     * CPU 信息
     */
    @SuppressWarnings("all")
    public CpuInfo getCpu() {
        return this.cpu;
    }

    /**
     * CPU 信息
     */
    @SuppressWarnings("all")
    public void setCpu(final CpuInfo cpu) {
        this.cpu = cpu;
    }

    /**
     * 内存信息
     */
    @SuppressWarnings("all")
    public MemoryInfo getMemory() {
        return this.memory;
    }

    /**
     * 内存信息
     */
    @SuppressWarnings("all")
    public void setMemory(final MemoryInfo memory) {
        this.memory = memory;
    }

    /**
     * JVM 信息
     */
    @SuppressWarnings("all")
    public JvmInfo getJvm() {
        return this.jvm;
    }

    /**
     * JVM 信息
     */
    @SuppressWarnings("all")
    public void setJvm(final JvmInfo jvm) {
        this.jvm = jvm;
    }

    /**
     * 磁盘信息
     */
    @SuppressWarnings("all")
    public List<DiskInfo> getDisks() {
        return this.disks;
    }

    /**
     * 磁盘信息
     */
    @SuppressWarnings("all")
    public void setDisks(final List<DiskInfo> disks) {
        this.disks = disks;
    }

    /**
     * 操作系统信息
     */
    @SuppressWarnings("all")
    public OsInfo getOs() {
        return this.os;
    }

    /**
     * 操作系统信息
     */
    @SuppressWarnings("all")
    public void setOs(final OsInfo os) {
        this.os = os;
    }

    /**
     * 服务健康检查
     */
    @SuppressWarnings("all")
    public List<ServiceCheckInfo> getServiceChecks() {
        return this.serviceChecks;
    }

    /**
     * 服务健康检查
     */
    @SuppressWarnings("all")
    public void setServiceChecks(final List<ServiceCheckInfo> serviceChecks) {
        this.serviceChecks = serviceChecks;
    }

    /**
     * Spring Boot Admin 控制台地址
     */
    @SuppressWarnings("all")
    public String getSbaUrl() {
        return this.sbaUrl;
    }

    /**
     * Spring Boot Admin 控制台地址
     */
    @SuppressWarnings("all")
    public void setSbaUrl(final String sbaUrl) {
        this.sbaUrl = sbaUrl;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ServerInfoResponse))
            return false;
        final ServerInfoResponse other = (ServerInfoResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$cpu = this.getCpu();
        final java.lang.Object other$cpu = other.getCpu();
        if (this$cpu == null ? other$cpu != null : !this$cpu.equals(other$cpu))
            return false;
        final java.lang.Object this$memory = this.getMemory();
        final java.lang.Object other$memory = other.getMemory();
        if (this$memory == null ? other$memory != null : !this$memory.equals(other$memory))
            return false;
        final java.lang.Object this$jvm = this.getJvm();
        final java.lang.Object other$jvm = other.getJvm();
        if (this$jvm == null ? other$jvm != null : !this$jvm.equals(other$jvm))
            return false;
        final java.lang.Object this$disks = this.getDisks();
        final java.lang.Object other$disks = other.getDisks();
        if (this$disks == null ? other$disks != null : !this$disks.equals(other$disks))
            return false;
        final java.lang.Object this$os = this.getOs();
        final java.lang.Object other$os = other.getOs();
        if (this$os == null ? other$os != null : !this$os.equals(other$os))
            return false;
        final java.lang.Object this$serviceChecks = this.getServiceChecks();
        final java.lang.Object other$serviceChecks = other.getServiceChecks();
        if (this$serviceChecks == null ? other$serviceChecks != null : !this$serviceChecks.equals(other$serviceChecks))
            return false;
        final java.lang.Object this$sbaUrl = this.getSbaUrl();
        final java.lang.Object other$sbaUrl = other.getSbaUrl();
        if (this$sbaUrl == null ? other$sbaUrl != null : !this$sbaUrl.equals(other$sbaUrl))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ServerInfoResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $cpu = this.getCpu();
        result = result * PRIME + ($cpu == null ? 43 : $cpu.hashCode());
        final java.lang.Object $memory = this.getMemory();
        result = result * PRIME + ($memory == null ? 43 : $memory.hashCode());
        final java.lang.Object $jvm = this.getJvm();
        result = result * PRIME + ($jvm == null ? 43 : $jvm.hashCode());
        final java.lang.Object $disks = this.getDisks();
        result = result * PRIME + ($disks == null ? 43 : $disks.hashCode());
        final java.lang.Object $os = this.getOs();
        result = result * PRIME + ($os == null ? 43 : $os.hashCode());
        final java.lang.Object $serviceChecks = this.getServiceChecks();
        result = result * PRIME + ($serviceChecks == null ? 43 : $serviceChecks.hashCode());
        final java.lang.Object $sbaUrl = this.getSbaUrl();
        result = result * PRIME + ($sbaUrl == null ? 43 : $sbaUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "ServerInfoResponse(cpu=" + this.getCpu() + ", memory=" + this.getMemory() + ", jvm=" + this.getJvm()
                + ", disks=" + this.getDisks() + ", os=" + this.getOs() + ", serviceChecks=" + this.getServiceChecks()
                + ", sbaUrl=" + this.getSbaUrl() + ")";
    }

    // ======================== 内部 DTO ========================
    public static class CpuInfo {

        /**
         * CPU 核心数
         */
        private int coreCount;

        /**
         * CPU 型号
         */
        private String model;

        /**
         * CPU 系统使用率（百分比）
         */
        private double systemUsage;

        /**
         * CPU 用户使用率（百分比）
         */
        private double userUsage;

        /**
         * CPU 总使用率（百分比）
         */
        private double totalUsage;

        /**
         * CPU 空闲率（百分比）
         */
        private double idle;

        /**
         * 监控状态（UP/WARN/DOWN）
         */
        private String status;

        @SuppressWarnings("all")
        public CpuInfo() {
        }

        /**
         * Creates a new {@code CpuInfo} instance.
         *
         * @param coreCount   CPU 核心数
         * @param model       CPU 型号
         * @param systemUsage CPU 系统使用率（百分比）
         * @param userUsage   CPU 用户使用率（百分比）
         * @param totalUsage  CPU 总使用率（百分比）
         * @param idle        CPU 空闲率（百分比）
         * @param status      监控状态（UP/WARN/DOWN）
         */
        @SuppressWarnings("all")
        public CpuInfo(final int coreCount, final String model, final double systemUsage, final double userUsage,
                       final double totalUsage, final double idle, final String status) {
            this.coreCount = coreCount;
            this.model = model;
            this.systemUsage = systemUsage;
            this.userUsage = userUsage;
            this.totalUsage = totalUsage;
            this.idle = idle;
            this.status = status;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.CpuInfo.CpuInfoBuilder builder() {
            return new ServerInfoResponse.CpuInfo.CpuInfoBuilder();
        }

        /**
         * CPU 核心数
         */
        @SuppressWarnings("all")
        public int getCoreCount() {
            return this.coreCount;
        }

        /**
         * CPU 核心数
         */
        @SuppressWarnings("all")
        public void setCoreCount(final int coreCount) {
            this.coreCount = coreCount;
        }

        /**
         * CPU 型号
         */
        @SuppressWarnings("all")
        public String getModel() {
            return this.model;
        }

        /**
         * CPU 型号
         */
        @SuppressWarnings("all")
        public void setModel(final String model) {
            this.model = model;
        }

        /**
         * CPU 系统使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getSystemUsage() {
            return this.systemUsage;
        }

        /**
         * CPU 系统使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setSystemUsage(final double systemUsage) {
            this.systemUsage = systemUsage;
        }

        /**
         * CPU 用户使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getUserUsage() {
            return this.userUsage;
        }

        /**
         * CPU 用户使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setUserUsage(final double userUsage) {
            this.userUsage = userUsage;
        }

        /**
         * CPU 总使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getTotalUsage() {
            return this.totalUsage;
        }

        /**
         * CPU 总使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setTotalUsage(final double totalUsage) {
            this.totalUsage = totalUsage;
        }

        /**
         * CPU 空闲率（百分比）
         */
        @SuppressWarnings("all")
        public double getIdle() {
            return this.idle;
        }

        /**
         * CPU 空闲率（百分比）
         */
        @SuppressWarnings("all")
        public void setIdle(final double idle) {
            this.idle = idle;
        }

        /**
         * 监控状态（UP/WARN/DOWN）
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 监控状态（UP/WARN/DOWN）
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.CpuInfo))
                return false;
            final ServerInfoResponse.CpuInfo other = (ServerInfoResponse.CpuInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getCoreCount() != other.getCoreCount())
                return false;
            if (java.lang.Double.compare(this.getSystemUsage(), other.getSystemUsage()) != 0)
                return false;
            if (java.lang.Double.compare(this.getUserUsage(), other.getUserUsage()) != 0)
                return false;
            if (java.lang.Double.compare(this.getTotalUsage(), other.getTotalUsage()) != 0)
                return false;
            if (java.lang.Double.compare(this.getIdle(), other.getIdle()) != 0)
                return false;
            final java.lang.Object this$model = this.getModel();
            final java.lang.Object other$model = other.getModel();
            if (this$model == null ? other$model != null : !this$model.equals(other$model))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.CpuInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getCoreCount();
            final long $systemUsage = java.lang.Double.doubleToLongBits(this.getSystemUsage());
            result = result * PRIME + (int) ($systemUsage >>> 32 ^ $systemUsage);
            final long $userUsage = java.lang.Double.doubleToLongBits(this.getUserUsage());
            result = result * PRIME + (int) ($userUsage >>> 32 ^ $userUsage);
            final long $totalUsage = java.lang.Double.doubleToLongBits(this.getTotalUsage());
            result = result * PRIME + (int) ($totalUsage >>> 32 ^ $totalUsage);
            final long $idle = java.lang.Double.doubleToLongBits(this.getIdle());
            result = result * PRIME + (int) ($idle >>> 32 ^ $idle);
            final java.lang.Object $model = this.getModel();
            result = result * PRIME + ($model == null ? 43 : $model.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.CpuInfo(coreCount=" + this.getCoreCount() + ", model=" + this.getModel()
                    + ", systemUsage=" + this.getSystemUsage() + ", userUsage=" + this.getUserUsage() + ", totalUsage="
                    + this.getTotalUsage() + ", idle=" + this.getIdle() + ", status=" + this.getStatus() + ")";
        }

        @SuppressWarnings("all")
        public static class CpuInfoBuilder {

            @SuppressWarnings("all")
            private int coreCount;

            @SuppressWarnings("all")
            private String model;

            @SuppressWarnings("all")
            private double systemUsage;

            @SuppressWarnings("all")
            private double userUsage;

            @SuppressWarnings("all")
            private double totalUsage;

            @SuppressWarnings("all")
            private double idle;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            CpuInfoBuilder() {
            }

            /**
             * CPU 核心数
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder coreCount(final int coreCount) {
                this.coreCount = coreCount;
                return this;
            }

            /**
             * CPU 型号
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder model(final String model) {
                this.model = model;
                return this;
            }

            /**
             * CPU 系统使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder systemUsage(final double systemUsage) {
                this.systemUsage = systemUsage;
                return this;
            }

            /**
             * CPU 用户使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder userUsage(final double userUsage) {
                this.userUsage = userUsage;
                return this;
            }

            /**
             * CPU 总使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder totalUsage(final double totalUsage) {
                this.totalUsage = totalUsage;
                return this;
            }

            /**
             * CPU 空闲率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder idle(final double idle) {
                this.idle = idle;
                return this;
            }

            /**
             * 监控状态（UP/WARN/DOWN）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo.CpuInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.CpuInfo build() {
                return new ServerInfoResponse.CpuInfo(this.coreCount, this.model, this.systemUsage, this.userUsage,
                        this.totalUsage, this.idle, this.status);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.CpuInfo.CpuInfoBuilder(coreCount=" + this.coreCount + ", model=" + this.model
                        + ", systemUsage=" + this.systemUsage + ", userUsage=" + this.userUsage + ", totalUsage="
                        + this.totalUsage + ", idle=" + this.idle + ", status=" + this.status + ")";
            }

        }

    }

    public static class MemoryInfo {

        /**
         * 总内存（字节）
         */
        private long total;

        /**
         * 已使用内存（字节）
         */
        private long used;

        /**
         * 空闲内存（字节）
         */
        private long free;

        /**
         * 使用率（百分比）
         */
        private double usageRate;

        /**
         * 总内存（可读格式）
         */
        private String totalDisplay;

        /**
         * 已使用内存（可读格式）
         */
        private String usedDisplay;

        /**
         * 空闲内存（可读格式）
         */
        private String freeDisplay;

        /**
         * 监控状态
         */
        private String status;

        @SuppressWarnings("all")
        public MemoryInfo() {
        }

        /**
         * Creates a new {@code MemoryInfo} instance.
         *
         * @param total        总内存（字节）
         * @param used         已使用内存（字节）
         * @param free         空闲内存（字节）
         * @param usageRate    使用率（百分比）
         * @param totalDisplay 总内存（可读格式）
         * @param usedDisplay  已使用内存（可读格式）
         * @param freeDisplay  空闲内存（可读格式）
         * @param status       监控状态
         */
        @SuppressWarnings("all")
        public MemoryInfo(final long total, final long used, final long free, final double usageRate,
                          final String totalDisplay, final String usedDisplay, final String freeDisplay, final String status) {
            this.total = total;
            this.used = used;
            this.free = free;
            this.usageRate = usageRate;
            this.totalDisplay = totalDisplay;
            this.usedDisplay = usedDisplay;
            this.freeDisplay = freeDisplay;
            this.status = status;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.MemoryInfo.MemoryInfoBuilder builder() {
            return new ServerInfoResponse.MemoryInfo.MemoryInfoBuilder();
        }

        /**
         * 总内存（字节）
         */
        @SuppressWarnings("all")
        public long getTotal() {
            return this.total;
        }

        /**
         * 总内存（字节）
         */
        @SuppressWarnings("all")
        public void setTotal(final long total) {
            this.total = total;
        }

        /**
         * 已使用内存（字节）
         */
        @SuppressWarnings("all")
        public long getUsed() {
            return this.used;
        }

        /**
         * 已使用内存（字节）
         */
        @SuppressWarnings("all")
        public void setUsed(final long used) {
            this.used = used;
        }

        /**
         * 空闲内存（字节）
         */
        @SuppressWarnings("all")
        public long getFree() {
            return this.free;
        }

        /**
         * 空闲内存（字节）
         */
        @SuppressWarnings("all")
        public void setFree(final long free) {
            this.free = free;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getUsageRate() {
            return this.usageRate;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setUsageRate(final double usageRate) {
            this.usageRate = usageRate;
        }

        /**
         * 总内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getTotalDisplay() {
            return this.totalDisplay;
        }

        /**
         * 总内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setTotalDisplay(final String totalDisplay) {
            this.totalDisplay = totalDisplay;
        }

        /**
         * 已使用内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getUsedDisplay() {
            return this.usedDisplay;
        }

        /**
         * 已使用内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setUsedDisplay(final String usedDisplay) {
            this.usedDisplay = usedDisplay;
        }

        /**
         * 空闲内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getFreeDisplay() {
            return this.freeDisplay;
        }

        /**
         * 空闲内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setFreeDisplay(final String freeDisplay) {
            this.freeDisplay = freeDisplay;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.MemoryInfo))
                return false;
            final ServerInfoResponse.MemoryInfo other = (ServerInfoResponse.MemoryInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTotal() != other.getTotal())
                return false;
            if (this.getUsed() != other.getUsed())
                return false;
            if (this.getFree() != other.getFree())
                return false;
            if (java.lang.Double.compare(this.getUsageRate(), other.getUsageRate()) != 0)
                return false;
            final java.lang.Object this$totalDisplay = this.getTotalDisplay();
            final java.lang.Object other$totalDisplay = other.getTotalDisplay();
            if (this$totalDisplay == null ? other$totalDisplay != null : !this$totalDisplay.equals(other$totalDisplay))
                return false;
            final java.lang.Object this$usedDisplay = this.getUsedDisplay();
            final java.lang.Object other$usedDisplay = other.getUsedDisplay();
            if (this$usedDisplay == null ? other$usedDisplay != null : !this$usedDisplay.equals(other$usedDisplay))
                return false;
            final java.lang.Object this$freeDisplay = this.getFreeDisplay();
            final java.lang.Object other$freeDisplay = other.getFreeDisplay();
            if (this$freeDisplay == null ? other$freeDisplay != null : !this$freeDisplay.equals(other$freeDisplay))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.MemoryInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $total = this.getTotal();
            result = result * PRIME + (int) ($total >>> 32 ^ $total);
            final long $used = this.getUsed();
            result = result * PRIME + (int) ($used >>> 32 ^ $used);
            final long $free = this.getFree();
            result = result * PRIME + (int) ($free >>> 32 ^ $free);
            final long $usageRate = java.lang.Double.doubleToLongBits(this.getUsageRate());
            result = result * PRIME + (int) ($usageRate >>> 32 ^ $usageRate);
            final java.lang.Object $totalDisplay = this.getTotalDisplay();
            result = result * PRIME + ($totalDisplay == null ? 43 : $totalDisplay.hashCode());
            final java.lang.Object $usedDisplay = this.getUsedDisplay();
            result = result * PRIME + ($usedDisplay == null ? 43 : $usedDisplay.hashCode());
            final java.lang.Object $freeDisplay = this.getFreeDisplay();
            result = result * PRIME + ($freeDisplay == null ? 43 : $freeDisplay.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.MemoryInfo(total=" + this.getTotal() + ", used=" + this.getUsed() + ", free="
                    + this.getFree() + ", usageRate=" + this.getUsageRate() + ", totalDisplay=" + this.getTotalDisplay()
                    + ", usedDisplay=" + this.getUsedDisplay() + ", freeDisplay=" + this.getFreeDisplay() + ", status="
                    + this.getStatus() + ")";
        }

        @SuppressWarnings("all")
        public static class MemoryInfoBuilder {

            @SuppressWarnings("all")
            private long total;

            @SuppressWarnings("all")
            private long used;

            @SuppressWarnings("all")
            private long free;

            @SuppressWarnings("all")
            private double usageRate;

            @SuppressWarnings("all")
            private String totalDisplay;

            @SuppressWarnings("all")
            private String usedDisplay;

            @SuppressWarnings("all")
            private String freeDisplay;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            MemoryInfoBuilder() {
            }

            /**
             * 总内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder total(final long total) {
                this.total = total;
                return this;
            }

            /**
             * 已使用内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder used(final long used) {
                this.used = used;
                return this;
            }

            /**
             * 空闲内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder free(final long free) {
                this.free = free;
                return this;
            }

            /**
             * 使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder usageRate(final double usageRate) {
                this.usageRate = usageRate;
                return this;
            }

            /**
             * 总内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder totalDisplay(final String totalDisplay) {
                this.totalDisplay = totalDisplay;
                return this;
            }

            /**
             * 已使用内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder usedDisplay(final String usedDisplay) {
                this.usedDisplay = usedDisplay;
                return this;
            }

            /**
             * 空闲内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder freeDisplay(final String freeDisplay) {
                this.freeDisplay = freeDisplay;
                return this;
            }

            /**
             * 监控状态
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo.MemoryInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.MemoryInfo build() {
                return new ServerInfoResponse.MemoryInfo(this.total, this.used, this.free, this.usageRate,
                        this.totalDisplay, this.usedDisplay, this.freeDisplay, this.status);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.MemoryInfo.MemoryInfoBuilder(total=" + this.total + ", used=" + this.used
                        + ", free=" + this.free + ", usageRate=" + this.usageRate + ", totalDisplay="
                        + this.totalDisplay + ", usedDisplay=" + this.usedDisplay + ", freeDisplay=" + this.freeDisplay
                        + ", status=" + this.status + ")";
            }

        }

    }

    public static class JvmInfo {

        /**
         * JVM 最大内存（字节）
         */
        private long maxMemory;

        /**
         * JVM 已分配内存（字节）
         */
        private long totalMemory;

        /**
         * JVM 已使用内存（字节）
         */
        private long usedMemory;

        /**
         * JVM 空闲内存（字节）
         */
        private long freeMemory;

        /**
         * 使用率（百分比）
         */
        private double usageRate;

        /**
         * JVM 最大内存（可读格式）
         */
        private String maxMemoryDisplay;

        /**
         * JVM 已分配内存（可读格式）
         */
        private String totalMemoryDisplay;

        /**
         * JVM 已使用内存（可读格式）
         */
        private String usedMemoryDisplay;

        /**
         * JVM 空闲内存（可读格式）
         */
        private String freeMemoryDisplay;

        /**
         * Java 版本
         */
        private String javaVersion;

        /**
         * JVM 名称
         */
        private String jvmName;

        /**
         * JVM 启动时间
         */
        private String startTime;

        /**
         * JVM 运行时长
         */
        private String runTime;

        /**
         * 监控状态
         */
        private String status;

        @SuppressWarnings("all")
        public JvmInfo() {
        }

        /**
         * Creates a new {@code JvmInfo} instance.
         *
         * @param maxMemory          JVM 最大内存（字节）
         * @param totalMemory        JVM 已分配内存（字节）
         * @param usedMemory         JVM 已使用内存（字节）
         * @param freeMemory         JVM 空闲内存（字节）
         * @param usageRate          使用率（百分比）
         * @param maxMemoryDisplay   JVM 最大内存（可读格式）
         * @param totalMemoryDisplay JVM 已分配内存（可读格式）
         * @param usedMemoryDisplay  JVM 已使用内存（可读格式）
         * @param freeMemoryDisplay  JVM 空闲内存（可读格式）
         * @param javaVersion        Java 版本
         * @param jvmName            JVM 名称
         * @param startTime          JVM 启动时间
         * @param runTime            JVM 运行时长
         * @param status             监控状态
         */
        @SuppressWarnings("all")
        public JvmInfo(final long maxMemory, final long totalMemory, final long usedMemory, final long freeMemory,
                       final double usageRate, final String maxMemoryDisplay, final String totalMemoryDisplay,
                       final String usedMemoryDisplay, final String freeMemoryDisplay, final String javaVersion,
                       final String jvmName, final String startTime, final String runTime, final String status) {
            this.maxMemory = maxMemory;
            this.totalMemory = totalMemory;
            this.usedMemory = usedMemory;
            this.freeMemory = freeMemory;
            this.usageRate = usageRate;
            this.maxMemoryDisplay = maxMemoryDisplay;
            this.totalMemoryDisplay = totalMemoryDisplay;
            this.usedMemoryDisplay = usedMemoryDisplay;
            this.freeMemoryDisplay = freeMemoryDisplay;
            this.javaVersion = javaVersion;
            this.jvmName = jvmName;
            this.startTime = startTime;
            this.runTime = runTime;
            this.status = status;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.JvmInfo.JvmInfoBuilder builder() {
            return new ServerInfoResponse.JvmInfo.JvmInfoBuilder();
        }

        /**
         * JVM 最大内存（字节）
         */
        @SuppressWarnings("all")
        public long getMaxMemory() {
            return this.maxMemory;
        }

        /**
         * JVM 最大内存（字节）
         */
        @SuppressWarnings("all")
        public void setMaxMemory(final long maxMemory) {
            this.maxMemory = maxMemory;
        }

        /**
         * JVM 已分配内存（字节）
         */
        @SuppressWarnings("all")
        public long getTotalMemory() {
            return this.totalMemory;
        }

        /**
         * JVM 已分配内存（字节）
         */
        @SuppressWarnings("all")
        public void setTotalMemory(final long totalMemory) {
            this.totalMemory = totalMemory;
        }

        /**
         * JVM 已使用内存（字节）
         */
        @SuppressWarnings("all")
        public long getUsedMemory() {
            return this.usedMemory;
        }

        /**
         * JVM 已使用内存（字节）
         */
        @SuppressWarnings("all")
        public void setUsedMemory(final long usedMemory) {
            this.usedMemory = usedMemory;
        }

        /**
         * JVM 空闲内存（字节）
         */
        @SuppressWarnings("all")
        public long getFreeMemory() {
            return this.freeMemory;
        }

        /**
         * JVM 空闲内存（字节）
         */
        @SuppressWarnings("all")
        public void setFreeMemory(final long freeMemory) {
            this.freeMemory = freeMemory;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getUsageRate() {
            return this.usageRate;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setUsageRate(final double usageRate) {
            this.usageRate = usageRate;
        }

        /**
         * JVM 最大内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getMaxMemoryDisplay() {
            return this.maxMemoryDisplay;
        }

        /**
         * JVM 最大内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setMaxMemoryDisplay(final String maxMemoryDisplay) {
            this.maxMemoryDisplay = maxMemoryDisplay;
        }

        /**
         * JVM 已分配内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getTotalMemoryDisplay() {
            return this.totalMemoryDisplay;
        }

        /**
         * JVM 已分配内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setTotalMemoryDisplay(final String totalMemoryDisplay) {
            this.totalMemoryDisplay = totalMemoryDisplay;
        }

        /**
         * JVM 已使用内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getUsedMemoryDisplay() {
            return this.usedMemoryDisplay;
        }

        /**
         * JVM 已使用内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setUsedMemoryDisplay(final String usedMemoryDisplay) {
            this.usedMemoryDisplay = usedMemoryDisplay;
        }

        /**
         * JVM 空闲内存（可读格式）
         */
        @SuppressWarnings("all")
        public String getFreeMemoryDisplay() {
            return this.freeMemoryDisplay;
        }

        /**
         * JVM 空闲内存（可读格式）
         */
        @SuppressWarnings("all")
        public void setFreeMemoryDisplay(final String freeMemoryDisplay) {
            this.freeMemoryDisplay = freeMemoryDisplay;
        }

        /**
         * Java 版本
         */
        @SuppressWarnings("all")
        public String getJavaVersion() {
            return this.javaVersion;
        }

        /**
         * Java 版本
         */
        @SuppressWarnings("all")
        public void setJavaVersion(final String javaVersion) {
            this.javaVersion = javaVersion;
        }

        /**
         * JVM 名称
         */
        @SuppressWarnings("all")
        public String getJvmName() {
            return this.jvmName;
        }

        /**
         * JVM 名称
         */
        @SuppressWarnings("all")
        public void setJvmName(final String jvmName) {
            this.jvmName = jvmName;
        }

        /**
         * JVM 启动时间
         */
        @SuppressWarnings("all")
        public String getStartTime() {
            return this.startTime;
        }

        /**
         * JVM 启动时间
         */
        @SuppressWarnings("all")
        public void setStartTime(final String startTime) {
            this.startTime = startTime;
        }

        /**
         * JVM 运行时长
         */
        @SuppressWarnings("all")
        public String getRunTime() {
            return this.runTime;
        }

        /**
         * JVM 运行时长
         */
        @SuppressWarnings("all")
        public void setRunTime(final String runTime) {
            this.runTime = runTime;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.JvmInfo))
                return false;
            final ServerInfoResponse.JvmInfo other = (ServerInfoResponse.JvmInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getMaxMemory() != other.getMaxMemory())
                return false;
            if (this.getTotalMemory() != other.getTotalMemory())
                return false;
            if (this.getUsedMemory() != other.getUsedMemory())
                return false;
            if (this.getFreeMemory() != other.getFreeMemory())
                return false;
            if (java.lang.Double.compare(this.getUsageRate(), other.getUsageRate()) != 0)
                return false;
            final java.lang.Object this$maxMemoryDisplay = this.getMaxMemoryDisplay();
            final java.lang.Object other$maxMemoryDisplay = other.getMaxMemoryDisplay();
            if (this$maxMemoryDisplay == null ? other$maxMemoryDisplay != null
                    : !this$maxMemoryDisplay.equals(other$maxMemoryDisplay))
                return false;
            final java.lang.Object this$totalMemoryDisplay = this.getTotalMemoryDisplay();
            final java.lang.Object other$totalMemoryDisplay = other.getTotalMemoryDisplay();
            if (this$totalMemoryDisplay == null ? other$totalMemoryDisplay != null
                    : !this$totalMemoryDisplay.equals(other$totalMemoryDisplay))
                return false;
            final java.lang.Object this$usedMemoryDisplay = this.getUsedMemoryDisplay();
            final java.lang.Object other$usedMemoryDisplay = other.getUsedMemoryDisplay();
            if (this$usedMemoryDisplay == null ? other$usedMemoryDisplay != null
                    : !this$usedMemoryDisplay.equals(other$usedMemoryDisplay))
                return false;
            final java.lang.Object this$freeMemoryDisplay = this.getFreeMemoryDisplay();
            final java.lang.Object other$freeMemoryDisplay = other.getFreeMemoryDisplay();
            if (this$freeMemoryDisplay == null ? other$freeMemoryDisplay != null
                    : !this$freeMemoryDisplay.equals(other$freeMemoryDisplay))
                return false;
            final java.lang.Object this$javaVersion = this.getJavaVersion();
            final java.lang.Object other$javaVersion = other.getJavaVersion();
            if (this$javaVersion == null ? other$javaVersion != null : !this$javaVersion.equals(other$javaVersion))
                return false;
            final java.lang.Object this$jvmName = this.getJvmName();
            final java.lang.Object other$jvmName = other.getJvmName();
            if (this$jvmName == null ? other$jvmName != null : !this$jvmName.equals(other$jvmName))
                return false;
            final java.lang.Object this$startTime = this.getStartTime();
            final java.lang.Object other$startTime = other.getStartTime();
            if (this$startTime == null ? other$startTime != null : !this$startTime.equals(other$startTime))
                return false;
            final java.lang.Object this$runTime = this.getRunTime();
            final java.lang.Object other$runTime = other.getRunTime();
            if (this$runTime == null ? other$runTime != null : !this$runTime.equals(other$runTime))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.JvmInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $maxMemory = this.getMaxMemory();
            result = result * PRIME + (int) ($maxMemory >>> 32 ^ $maxMemory);
            final long $totalMemory = this.getTotalMemory();
            result = result * PRIME + (int) ($totalMemory >>> 32 ^ $totalMemory);
            final long $usedMemory = this.getUsedMemory();
            result = result * PRIME + (int) ($usedMemory >>> 32 ^ $usedMemory);
            final long $freeMemory = this.getFreeMemory();
            result = result * PRIME + (int) ($freeMemory >>> 32 ^ $freeMemory);
            final long $usageRate = java.lang.Double.doubleToLongBits(this.getUsageRate());
            result = result * PRIME + (int) ($usageRate >>> 32 ^ $usageRate);
            final java.lang.Object $maxMemoryDisplay = this.getMaxMemoryDisplay();
            result = result * PRIME + ($maxMemoryDisplay == null ? 43 : $maxMemoryDisplay.hashCode());
            final java.lang.Object $totalMemoryDisplay = this.getTotalMemoryDisplay();
            result = result * PRIME + ($totalMemoryDisplay == null ? 43 : $totalMemoryDisplay.hashCode());
            final java.lang.Object $usedMemoryDisplay = this.getUsedMemoryDisplay();
            result = result * PRIME + ($usedMemoryDisplay == null ? 43 : $usedMemoryDisplay.hashCode());
            final java.lang.Object $freeMemoryDisplay = this.getFreeMemoryDisplay();
            result = result * PRIME + ($freeMemoryDisplay == null ? 43 : $freeMemoryDisplay.hashCode());
            final java.lang.Object $javaVersion = this.getJavaVersion();
            result = result * PRIME + ($javaVersion == null ? 43 : $javaVersion.hashCode());
            final java.lang.Object $jvmName = this.getJvmName();
            result = result * PRIME + ($jvmName == null ? 43 : $jvmName.hashCode());
            final java.lang.Object $startTime = this.getStartTime();
            result = result * PRIME + ($startTime == null ? 43 : $startTime.hashCode());
            final java.lang.Object $runTime = this.getRunTime();
            result = result * PRIME + ($runTime == null ? 43 : $runTime.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.JvmInfo(maxMemory=" + this.getMaxMemory() + ", totalMemory="
                    + this.getTotalMemory() + ", usedMemory=" + this.getUsedMemory() + ", freeMemory="
                    + this.getFreeMemory() + ", usageRate=" + this.getUsageRate() + ", maxMemoryDisplay="
                    + this.getMaxMemoryDisplay() + ", totalMemoryDisplay=" + this.getTotalMemoryDisplay()
                    + ", usedMemoryDisplay=" + this.getUsedMemoryDisplay() + ", freeMemoryDisplay="
                    + this.getFreeMemoryDisplay() + ", javaVersion=" + this.getJavaVersion() + ", jvmName="
                    + this.getJvmName() + ", startTime=" + this.getStartTime() + ", runTime=" + this.getRunTime()
                    + ", status=" + this.getStatus() + ")";
        }

        @SuppressWarnings("all")
        public static class JvmInfoBuilder {

            @SuppressWarnings("all")
            private long maxMemory;

            @SuppressWarnings("all")
            private long totalMemory;

            @SuppressWarnings("all")
            private long usedMemory;

            @SuppressWarnings("all")
            private long freeMemory;

            @SuppressWarnings("all")
            private double usageRate;

            @SuppressWarnings("all")
            private String maxMemoryDisplay;

            @SuppressWarnings("all")
            private String totalMemoryDisplay;

            @SuppressWarnings("all")
            private String usedMemoryDisplay;

            @SuppressWarnings("all")
            private String freeMemoryDisplay;

            @SuppressWarnings("all")
            private String javaVersion;

            @SuppressWarnings("all")
            private String jvmName;

            @SuppressWarnings("all")
            private String startTime;

            @SuppressWarnings("all")
            private String runTime;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            JvmInfoBuilder() {
            }

            /**
             * JVM 最大内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder maxMemory(final long maxMemory) {
                this.maxMemory = maxMemory;
                return this;
            }

            /**
             * JVM 已分配内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder totalMemory(final long totalMemory) {
                this.totalMemory = totalMemory;
                return this;
            }

            /**
             * JVM 已使用内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder usedMemory(final long usedMemory) {
                this.usedMemory = usedMemory;
                return this;
            }

            /**
             * JVM 空闲内存（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder freeMemory(final long freeMemory) {
                this.freeMemory = freeMemory;
                return this;
            }

            /**
             * 使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder usageRate(final double usageRate) {
                this.usageRate = usageRate;
                return this;
            }

            /**
             * JVM 最大内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder maxMemoryDisplay(final String maxMemoryDisplay) {
                this.maxMemoryDisplay = maxMemoryDisplay;
                return this;
            }

            /**
             * JVM 已分配内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder totalMemoryDisplay(final String totalMemoryDisplay) {
                this.totalMemoryDisplay = totalMemoryDisplay;
                return this;
            }

            /**
             * JVM 已使用内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder usedMemoryDisplay(final String usedMemoryDisplay) {
                this.usedMemoryDisplay = usedMemoryDisplay;
                return this;
            }

            /**
             * JVM 空闲内存（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder freeMemoryDisplay(final String freeMemoryDisplay) {
                this.freeMemoryDisplay = freeMemoryDisplay;
                return this;
            }

            /**
             * Java 版本
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder javaVersion(final String javaVersion) {
                this.javaVersion = javaVersion;
                return this;
            }

            /**
             * JVM 名称
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder jvmName(final String jvmName) {
                this.jvmName = jvmName;
                return this;
            }

            /**
             * JVM 启动时间
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder startTime(final String startTime) {
                this.startTime = startTime;
                return this;
            }

            /**
             * JVM 运行时长
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder runTime(final String runTime) {
                this.runTime = runTime;
                return this;
            }

            /**
             * 监控状态
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo.JvmInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.JvmInfo build() {
                return new ServerInfoResponse.JvmInfo(this.maxMemory, this.totalMemory, this.usedMemory,
                        this.freeMemory, this.usageRate, this.maxMemoryDisplay, this.totalMemoryDisplay,
                        this.usedMemoryDisplay, this.freeMemoryDisplay, this.javaVersion, this.jvmName, this.startTime,
                        this.runTime, this.status);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.JvmInfo.JvmInfoBuilder(maxMemory=" + this.maxMemory + ", totalMemory="
                        + this.totalMemory + ", usedMemory=" + this.usedMemory + ", freeMemory=" + this.freeMemory
                        + ", usageRate=" + this.usageRate + ", maxMemoryDisplay=" + this.maxMemoryDisplay
                        + ", totalMemoryDisplay=" + this.totalMemoryDisplay + ", usedMemoryDisplay="
                        + this.usedMemoryDisplay + ", freeMemoryDisplay=" + this.freeMemoryDisplay + ", javaVersion="
                        + this.javaVersion + ", jvmName=" + this.jvmName + ", startTime=" + this.startTime
                        + ", runTime=" + this.runTime + ", status=" + this.status + ")";
            }

        }

    }

    public static class DiskInfo {

        /**
         * 挂载路径
         */
        private String mountPoint;

        /**
         * 文件系统类型
         */
        private String fsType;

        /**
         * 总大小（字节）
         */
        private long total;

        /**
         * 已使用（字节）
         */
        private long used;

        /**
         * 可用（字节）
         */
        private long free;

        /**
         * 使用率（百分比）
         */
        private double usageRate;

        /**
         * 总大小（可读格式）
         */
        private String totalDisplay;

        /**
         * 已使用（可读格式）
         */
        private String usedDisplay;

        /**
         * 可用（可读格式）
         */
        private String freeDisplay;

        /**
         * 监控状态
         */
        private String status;

        @SuppressWarnings("all")
        public DiskInfo() {
        }

        /**
         * Creates a new {@code DiskInfo} instance.
         *
         * @param mountPoint   挂载路径
         * @param fsType       文件系统类型
         * @param total        总大小（字节）
         * @param used         已使用（字节）
         * @param free         可用（字节）
         * @param usageRate    使用率（百分比）
         * @param totalDisplay 总大小（可读格式）
         * @param usedDisplay  已使用（可读格式）
         * @param freeDisplay  可用（可读格式）
         * @param status       监控状态
         */
        @SuppressWarnings("all")
        public DiskInfo(final String mountPoint, final String fsType, final long total, final long used,
                        final long free, final double usageRate, final String totalDisplay, final String usedDisplay,
                        final String freeDisplay, final String status) {
            this.mountPoint = mountPoint;
            this.fsType = fsType;
            this.total = total;
            this.used = used;
            this.free = free;
            this.usageRate = usageRate;
            this.totalDisplay = totalDisplay;
            this.usedDisplay = usedDisplay;
            this.freeDisplay = freeDisplay;
            this.status = status;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.DiskInfo.DiskInfoBuilder builder() {
            return new ServerInfoResponse.DiskInfo.DiskInfoBuilder();
        }

        /**
         * 挂载路径
         */
        @SuppressWarnings("all")
        public String getMountPoint() {
            return this.mountPoint;
        }

        /**
         * 挂载路径
         */
        @SuppressWarnings("all")
        public void setMountPoint(final String mountPoint) {
            this.mountPoint = mountPoint;
        }

        /**
         * 文件系统类型
         */
        @SuppressWarnings("all")
        public String getFsType() {
            return this.fsType;
        }

        /**
         * 文件系统类型
         */
        @SuppressWarnings("all")
        public void setFsType(final String fsType) {
            this.fsType = fsType;
        }

        /**
         * 总大小（字节）
         */
        @SuppressWarnings("all")
        public long getTotal() {
            return this.total;
        }

        /**
         * 总大小（字节）
         */
        @SuppressWarnings("all")
        public void setTotal(final long total) {
            this.total = total;
        }

        /**
         * 已使用（字节）
         */
        @SuppressWarnings("all")
        public long getUsed() {
            return this.used;
        }

        /**
         * 已使用（字节）
         */
        @SuppressWarnings("all")
        public void setUsed(final long used) {
            this.used = used;
        }

        /**
         * 可用（字节）
         */
        @SuppressWarnings("all")
        public long getFree() {
            return this.free;
        }

        /**
         * 可用（字节）
         */
        @SuppressWarnings("all")
        public void setFree(final long free) {
            this.free = free;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public double getUsageRate() {
            return this.usageRate;
        }

        /**
         * 使用率（百分比）
         */
        @SuppressWarnings("all")
        public void setUsageRate(final double usageRate) {
            this.usageRate = usageRate;
        }

        /**
         * 总大小（可读格式）
         */
        @SuppressWarnings("all")
        public String getTotalDisplay() {
            return this.totalDisplay;
        }

        /**
         * 总大小（可读格式）
         */
        @SuppressWarnings("all")
        public void setTotalDisplay(final String totalDisplay) {
            this.totalDisplay = totalDisplay;
        }

        /**
         * 已使用（可读格式）
         */
        @SuppressWarnings("all")
        public String getUsedDisplay() {
            return this.usedDisplay;
        }

        /**
         * 已使用（可读格式）
         */
        @SuppressWarnings("all")
        public void setUsedDisplay(final String usedDisplay) {
            this.usedDisplay = usedDisplay;
        }

        /**
         * 可用（可读格式）
         */
        @SuppressWarnings("all")
        public String getFreeDisplay() {
            return this.freeDisplay;
        }

        /**
         * 可用（可读格式）
         */
        @SuppressWarnings("all")
        public void setFreeDisplay(final String freeDisplay) {
            this.freeDisplay = freeDisplay;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 监控状态
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.DiskInfo))
                return false;
            final ServerInfoResponse.DiskInfo other = (ServerInfoResponse.DiskInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTotal() != other.getTotal())
                return false;
            if (this.getUsed() != other.getUsed())
                return false;
            if (this.getFree() != other.getFree())
                return false;
            if (java.lang.Double.compare(this.getUsageRate(), other.getUsageRate()) != 0)
                return false;
            final java.lang.Object this$mountPoint = this.getMountPoint();
            final java.lang.Object other$mountPoint = other.getMountPoint();
            if (this$mountPoint == null ? other$mountPoint != null : !this$mountPoint.equals(other$mountPoint))
                return false;
            final java.lang.Object this$fsType = this.getFsType();
            final java.lang.Object other$fsType = other.getFsType();
            if (this$fsType == null ? other$fsType != null : !this$fsType.equals(other$fsType))
                return false;
            final java.lang.Object this$totalDisplay = this.getTotalDisplay();
            final java.lang.Object other$totalDisplay = other.getTotalDisplay();
            if (this$totalDisplay == null ? other$totalDisplay != null : !this$totalDisplay.equals(other$totalDisplay))
                return false;
            final java.lang.Object this$usedDisplay = this.getUsedDisplay();
            final java.lang.Object other$usedDisplay = other.getUsedDisplay();
            if (this$usedDisplay == null ? other$usedDisplay != null : !this$usedDisplay.equals(other$usedDisplay))
                return false;
            final java.lang.Object this$freeDisplay = this.getFreeDisplay();
            final java.lang.Object other$freeDisplay = other.getFreeDisplay();
            if (this$freeDisplay == null ? other$freeDisplay != null : !this$freeDisplay.equals(other$freeDisplay))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.DiskInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $total = this.getTotal();
            result = result * PRIME + (int) ($total >>> 32 ^ $total);
            final long $used = this.getUsed();
            result = result * PRIME + (int) ($used >>> 32 ^ $used);
            final long $free = this.getFree();
            result = result * PRIME + (int) ($free >>> 32 ^ $free);
            final long $usageRate = java.lang.Double.doubleToLongBits(this.getUsageRate());
            result = result * PRIME + (int) ($usageRate >>> 32 ^ $usageRate);
            final java.lang.Object $mountPoint = this.getMountPoint();
            result = result * PRIME + ($mountPoint == null ? 43 : $mountPoint.hashCode());
            final java.lang.Object $fsType = this.getFsType();
            result = result * PRIME + ($fsType == null ? 43 : $fsType.hashCode());
            final java.lang.Object $totalDisplay = this.getTotalDisplay();
            result = result * PRIME + ($totalDisplay == null ? 43 : $totalDisplay.hashCode());
            final java.lang.Object $usedDisplay = this.getUsedDisplay();
            result = result * PRIME + ($usedDisplay == null ? 43 : $usedDisplay.hashCode());
            final java.lang.Object $freeDisplay = this.getFreeDisplay();
            result = result * PRIME + ($freeDisplay == null ? 43 : $freeDisplay.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.DiskInfo(mountPoint=" + this.getMountPoint() + ", fsType=" + this.getFsType()
                    + ", total=" + this.getTotal() + ", used=" + this.getUsed() + ", free=" + this.getFree()
                    + ", usageRate=" + this.getUsageRate() + ", totalDisplay=" + this.getTotalDisplay()
                    + ", usedDisplay=" + this.getUsedDisplay() + ", freeDisplay=" + this.getFreeDisplay() + ", status="
                    + this.getStatus() + ")";
        }

        @SuppressWarnings("all")
        public static class DiskInfoBuilder {

            @SuppressWarnings("all")
            private String mountPoint;

            @SuppressWarnings("all")
            private String fsType;

            @SuppressWarnings("all")
            private long total;

            @SuppressWarnings("all")
            private long used;

            @SuppressWarnings("all")
            private long free;

            @SuppressWarnings("all")
            private double usageRate;

            @SuppressWarnings("all")
            private String totalDisplay;

            @SuppressWarnings("all")
            private String usedDisplay;

            @SuppressWarnings("all")
            private String freeDisplay;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            DiskInfoBuilder() {
            }

            /**
             * 挂载路径
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder mountPoint(final String mountPoint) {
                this.mountPoint = mountPoint;
                return this;
            }

            /**
             * 文件系统类型
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder fsType(final String fsType) {
                this.fsType = fsType;
                return this;
            }

            /**
             * 总大小（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder total(final long total) {
                this.total = total;
                return this;
            }

            /**
             * 已使用（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder used(final long used) {
                this.used = used;
                return this;
            }

            /**
             * 可用（字节）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder free(final long free) {
                this.free = free;
                return this;
            }

            /**
             * 使用率（百分比）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder usageRate(final double usageRate) {
                this.usageRate = usageRate;
                return this;
            }

            /**
             * 总大小（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder totalDisplay(final String totalDisplay) {
                this.totalDisplay = totalDisplay;
                return this;
            }

            /**
             * 已使用（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder usedDisplay(final String usedDisplay) {
                this.usedDisplay = usedDisplay;
                return this;
            }

            /**
             * 可用（可读格式）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder freeDisplay(final String freeDisplay) {
                this.freeDisplay = freeDisplay;
                return this;
            }

            /**
             * 监控状态
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo.DiskInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.DiskInfo build() {
                return new ServerInfoResponse.DiskInfo(this.mountPoint, this.fsType, this.total, this.used, this.free,
                        this.usageRate, this.totalDisplay, this.usedDisplay, this.freeDisplay, this.status);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.DiskInfo.DiskInfoBuilder(mountPoint=" + this.mountPoint + ", fsType="
                        + this.fsType + ", total=" + this.total + ", used=" + this.used + ", free=" + this.free
                        + ", usageRate=" + this.usageRate + ", totalDisplay=" + this.totalDisplay + ", usedDisplay="
                        + this.usedDisplay + ", freeDisplay=" + this.freeDisplay + ", status=" + this.status + ")";
            }

        }

    }

    public static class OsInfo {

        /**
         * 操作系统名称
         */
        private String name;

        /**
         * 系统架构
         */
        private String arch;

        /**
         * 系统版本
         */
        private String version;

        /**
         * 主机名
         */
        private String hostName;

        /**
         * 系统运行时长
         */
        private String uptime;

        @SuppressWarnings("all")
        public OsInfo() {
        }

        /**
         * Creates a new {@code OsInfo} instance.
         *
         * @param name     操作系统名称
         * @param arch     系统架构
         * @param version  系统版本
         * @param hostName 主机名
         * @param uptime   系统运行时长
         */
        @SuppressWarnings("all")
        public OsInfo(final String name, final String arch, final String version, final String hostName,
                      final String uptime) {
            this.name = name;
            this.arch = arch;
            this.version = version;
            this.hostName = hostName;
            this.uptime = uptime;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.OsInfo.OsInfoBuilder builder() {
            return new ServerInfoResponse.OsInfo.OsInfoBuilder();
        }

        /**
         * 操作系统名称
         */
        @SuppressWarnings("all")
        public String getName() {
            return this.name;
        }

        /**
         * 操作系统名称
         */
        @SuppressWarnings("all")
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * 系统架构
         */
        @SuppressWarnings("all")
        public String getArch() {
            return this.arch;
        }

        /**
         * 系统架构
         */
        @SuppressWarnings("all")
        public void setArch(final String arch) {
            this.arch = arch;
        }

        /**
         * 系统版本
         */
        @SuppressWarnings("all")
        public String getVersion() {
            return this.version;
        }

        /**
         * 系统版本
         */
        @SuppressWarnings("all")
        public void setVersion(final String version) {
            this.version = version;
        }

        /**
         * 主机名
         */
        @SuppressWarnings("all")
        public String getHostName() {
            return this.hostName;
        }

        /**
         * 主机名
         */
        @SuppressWarnings("all")
        public void setHostName(final String hostName) {
            this.hostName = hostName;
        }

        /**
         * 系统运行时长
         */
        @SuppressWarnings("all")
        public String getUptime() {
            return this.uptime;
        }

        /**
         * 系统运行时长
         */
        @SuppressWarnings("all")
        public void setUptime(final String uptime) {
            this.uptime = uptime;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.OsInfo))
                return false;
            final ServerInfoResponse.OsInfo other = (ServerInfoResponse.OsInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name))
                return false;
            final java.lang.Object this$arch = this.getArch();
            final java.lang.Object other$arch = other.getArch();
            if (this$arch == null ? other$arch != null : !this$arch.equals(other$arch))
                return false;
            final java.lang.Object this$version = this.getVersion();
            final java.lang.Object other$version = other.getVersion();
            if (this$version == null ? other$version != null : !this$version.equals(other$version))
                return false;
            final java.lang.Object this$hostName = this.getHostName();
            final java.lang.Object other$hostName = other.getHostName();
            if (this$hostName == null ? other$hostName != null : !this$hostName.equals(other$hostName))
                return false;
            final java.lang.Object this$uptime = this.getUptime();
            final java.lang.Object other$uptime = other.getUptime();
            if (this$uptime == null ? other$uptime != null : !this$uptime.equals(other$uptime))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.OsInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $arch = this.getArch();
            result = result * PRIME + ($arch == null ? 43 : $arch.hashCode());
            final java.lang.Object $version = this.getVersion();
            result = result * PRIME + ($version == null ? 43 : $version.hashCode());
            final java.lang.Object $hostName = this.getHostName();
            result = result * PRIME + ($hostName == null ? 43 : $hostName.hashCode());
            final java.lang.Object $uptime = this.getUptime();
            result = result * PRIME + ($uptime == null ? 43 : $uptime.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.OsInfo(name=" + this.getName() + ", arch=" + this.getArch() + ", version="
                    + this.getVersion() + ", hostName=" + this.getHostName() + ", uptime=" + this.getUptime() + ")";
        }

        @SuppressWarnings("all")
        public static class OsInfoBuilder {

            @SuppressWarnings("all")
            private String name;

            @SuppressWarnings("all")
            private String arch;

            @SuppressWarnings("all")
            private String version;

            @SuppressWarnings("all")
            private String hostName;

            @SuppressWarnings("all")
            private String uptime;

            @SuppressWarnings("all")
            OsInfoBuilder() {
            }

            /**
             * 操作系统名称
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo.OsInfoBuilder name(final String name) {
                this.name = name;
                return this;
            }

            /**
             * 系统架构
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo.OsInfoBuilder arch(final String arch) {
                this.arch = arch;
                return this;
            }

            /**
             * 系统版本
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo.OsInfoBuilder version(final String version) {
                this.version = version;
                return this;
            }

            /**
             * 主机名
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo.OsInfoBuilder hostName(final String hostName) {
                this.hostName = hostName;
                return this;
            }

            /**
             * 系统运行时长
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo.OsInfoBuilder uptime(final String uptime) {
                this.uptime = uptime;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.OsInfo build() {
                return new ServerInfoResponse.OsInfo(this.name, this.arch, this.version, this.hostName, this.uptime);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.OsInfo.OsInfoBuilder(name=" + this.name + ", arch=" + this.arch
                        + ", version=" + this.version + ", hostName=" + this.hostName + ", uptime=" + this.uptime + ")";
            }

        }

    }

    public static class ServiceCheckInfo {

        /**
         * 检查项名称
         */
        private String name;

        /**
         * 检查项类型（字典值：sys_monitor_type）
         */
        private String type;

        /**
         * 状态（字典值：sys_monitor_status）
         */
        private String status;

        /**
         * 状态标签（字典标签）
         */
        private String statusLabel;

        /**
         * 响应时间（毫秒）
         */
        private long responseTime;

        /**
         * 详细信息
         */
        private String detail;

        @SuppressWarnings("all")
        public ServiceCheckInfo() {
        }

        /**
         * Creates a new {@code ServiceCheckInfo} instance.
         *
         * @param name         检查项名称
         * @param type         检查项类型（字典值：sys_monitor_type）
         * @param status       状态（字典值：sys_monitor_status）
         * @param statusLabel  状态标签（字典标签）
         * @param responseTime 响应时间（毫秒）
         * @param detail       详细信息
         */
        @SuppressWarnings("all")
        public ServiceCheckInfo(final String name, final String type, final String status, final String statusLabel,
                                final long responseTime, final String detail) {
            this.name = name;
            this.type = type;
            this.status = status;
            this.statusLabel = statusLabel;
            this.responseTime = responseTime;
            this.detail = detail;
        }

        @SuppressWarnings("all")
        public static ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder builder() {
            return new ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder();
        }

        /**
         * 检查项名称
         */
        @SuppressWarnings("all")
        public String getName() {
            return this.name;
        }

        /**
         * 检查项名称
         */
        @SuppressWarnings("all")
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * 检查项类型（字典值：sys_monitor_type）
         */
        @SuppressWarnings("all")
        public String getType() {
            return this.type;
        }

        /**
         * 检查项类型（字典值：sys_monitor_type）
         */
        @SuppressWarnings("all")
        public void setType(final String type) {
            this.type = type;
        }

        /**
         * 状态（字典值：sys_monitor_status）
         */
        @SuppressWarnings("all")
        public String getStatus() {
            return this.status;
        }

        /**
         * 状态（字典值：sys_monitor_status）
         */
        @SuppressWarnings("all")
        public void setStatus(final String status) {
            this.status = status;
        }

        /**
         * 状态标签（字典标签）
         */
        @SuppressWarnings("all")
        public String getStatusLabel() {
            return this.statusLabel;
        }

        /**
         * 状态标签（字典标签）
         */
        @SuppressWarnings("all")
        public void setStatusLabel(final String statusLabel) {
            this.statusLabel = statusLabel;
        }

        /**
         * 响应时间（毫秒）
         */
        @SuppressWarnings("all")
        public long getResponseTime() {
            return this.responseTime;
        }

        /**
         * 响应时间（毫秒）
         */
        @SuppressWarnings("all")
        public void setResponseTime(final long responseTime) {
            this.responseTime = responseTime;
        }

        /**
         * 详细信息
         */
        @SuppressWarnings("all")
        public String getDetail() {
            return this.detail;
        }

        /**
         * 详细信息
         */
        @SuppressWarnings("all")
        public void setDetail(final String detail) {
            this.detail = detail;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ServerInfoResponse.ServiceCheckInfo))
                return false;
            final ServerInfoResponse.ServiceCheckInfo other = (ServerInfoResponse.ServiceCheckInfo) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getResponseTime() != other.getResponseTime())
                return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name))
                return false;
            final java.lang.Object this$type = this.getType();
            final java.lang.Object other$type = other.getType();
            if (this$type == null ? other$type != null : !this$type.equals(other$type))
                return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status))
                return false;
            final java.lang.Object this$statusLabel = this.getStatusLabel();
            final java.lang.Object other$statusLabel = other.getStatusLabel();
            if (this$statusLabel == null ? other$statusLabel != null : !this$statusLabel.equals(other$statusLabel))
                return false;
            final java.lang.Object this$detail = this.getDetail();
            final java.lang.Object other$detail = other.getDetail();
            if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof ServerInfoResponse.ServiceCheckInfo;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $responseTime = this.getResponseTime();
            result = result * PRIME + (int) ($responseTime >>> 32 ^ $responseTime);
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $type = this.getType();
            result = result * PRIME + ($type == null ? 43 : $type.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            final java.lang.Object $statusLabel = this.getStatusLabel();
            result = result * PRIME + ($statusLabel == null ? 43 : $statusLabel.hashCode());
            final java.lang.Object $detail = this.getDetail();
            result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.ServiceCheckInfo(name=" + this.getName() + ", type=" + this.getType()
                    + ", status=" + this.getStatus() + ", statusLabel=" + this.getStatusLabel() + ", responseTime="
                    + this.getResponseTime() + ", detail=" + this.getDetail() + ")";
        }

        @SuppressWarnings("all")
        public static class ServiceCheckInfoBuilder {

            @SuppressWarnings("all")
            private String name;

            @SuppressWarnings("all")
            private String type;

            @SuppressWarnings("all")
            private String status;

            @SuppressWarnings("all")
            private String statusLabel;

            @SuppressWarnings("all")
            private long responseTime;

            @SuppressWarnings("all")
            private String detail;

            @SuppressWarnings("all")
            ServiceCheckInfoBuilder() {
            }

            /**
             * 检查项名称
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder name(final String name) {
                this.name = name;
                return this;
            }

            /**
             * 检查项类型（字典值：sys_monitor_type）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder type(final String type) {
                this.type = type;
                return this;
            }

            /**
             * 状态（字典值：sys_monitor_status）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder status(final String status) {
                this.status = status;
                return this;
            }

            /**
             * 状态标签（字典标签）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder statusLabel(final String statusLabel) {
                this.statusLabel = statusLabel;
                return this;
            }

            /**
             * 响应时间（毫秒）
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder responseTime(final long responseTime) {
                this.responseTime = responseTime;
                return this;
            }

            /**
             * 详细信息
             *
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder detail(final String detail) {
                this.detail = detail;
                return this;
            }

            @SuppressWarnings("all")
            public ServerInfoResponse.ServiceCheckInfo build() {
                return new ServerInfoResponse.ServiceCheckInfo(this.name, this.type, this.status, this.statusLabel,
                        this.responseTime, this.detail);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "ServerInfoResponse.ServiceCheckInfo.ServiceCheckInfoBuilder(name=" + this.name + ", type="
                        + this.type + ", status=" + this.status + ", statusLabel=" + this.statusLabel
                        + ", responseTime=" + this.responseTime + ", detail=" + this.detail + ")";
            }

        }

    }

    @SuppressWarnings("all")
    public static class ServerInfoResponseBuilder {

        @SuppressWarnings("all")
        private CpuInfo cpu;

        @SuppressWarnings("all")
        private MemoryInfo memory;

        @SuppressWarnings("all")
        private JvmInfo jvm;

        @SuppressWarnings("all")
        private List<DiskInfo> disks;

        @SuppressWarnings("all")
        private OsInfo os;

        @SuppressWarnings("all")
        private List<ServiceCheckInfo> serviceChecks;

        @SuppressWarnings("all")
        private String sbaUrl;

        @SuppressWarnings("all")
        ServerInfoResponseBuilder() {
        }

        /**
         * CPU 信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder cpu(final CpuInfo cpu) {
            this.cpu = cpu;
            return this;
        }

        /**
         * 内存信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder memory(final MemoryInfo memory) {
            this.memory = memory;
            return this;
        }

        /**
         * JVM 信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder jvm(final JvmInfo jvm) {
            this.jvm = jvm;
            return this;
        }

        /**
         * 磁盘信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder disks(final List<DiskInfo> disks) {
            this.disks = disks;
            return this;
        }

        /**
         * 操作系统信息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder os(final OsInfo os) {
            this.os = os;
            return this;
        }

        /**
         * 服务健康检查
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder serviceChecks(final List<ServiceCheckInfo> serviceChecks) {
            this.serviceChecks = serviceChecks;
            return this;
        }

        /**
         * Spring Boot Admin 控制台地址
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ServerInfoResponse.ServerInfoResponseBuilder sbaUrl(final String sbaUrl) {
            this.sbaUrl = sbaUrl;
            return this;
        }

        @SuppressWarnings("all")
        public ServerInfoResponse build() {
            return new ServerInfoResponse(this.cpu, this.memory, this.jvm, this.disks, this.os, this.serviceChecks,
                    this.sbaUrl);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ServerInfoResponse.ServerInfoResponseBuilder(cpu=" + this.cpu + ", memory=" + this.memory + ", jvm="
                    + this.jvm + ", disks=" + this.disks + ", os=" + this.os + ", serviceChecks=" + this.serviceChecks
                    + ", sbaUrl=" + this.sbaUrl + ")";
        }

    }

}
