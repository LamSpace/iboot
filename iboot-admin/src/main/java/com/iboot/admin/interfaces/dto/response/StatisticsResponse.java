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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 统计分析响应 DTO
 *
 * @author iBoot
 */
public class StatisticsResponse {

    /**
     * 数据统计报表响应
     */
    @Schema(description = "数据统计报表响应")
    public static class ReportResponse {

        @Schema(description = "用户统计")
        private UserStats userStats;

        @Schema(description = "日志统计")
        private LogStats logStats;

        @Schema(description = "系统概览")
        private OverviewStats overviewStats;

        @SuppressWarnings("all")
        public ReportResponse() {
        }

        @SuppressWarnings("all")
        public ReportResponse(final UserStats userStats, final LogStats logStats, final OverviewStats overviewStats) {
            this.userStats = userStats;
            this.logStats = logStats;
            this.overviewStats = overviewStats;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.ReportResponse.ReportResponseBuilder builder() {
            return new StatisticsResponse.ReportResponse.ReportResponseBuilder();
        }

        @SuppressWarnings("all")
        public UserStats getUserStats() {
            return this.userStats;
        }

        @SuppressWarnings("all")
        public void setUserStats(final UserStats userStats) {
            this.userStats = userStats;
        }

        @SuppressWarnings("all")
        public LogStats getLogStats() {
            return this.logStats;
        }

        @SuppressWarnings("all")
        public void setLogStats(final LogStats logStats) {
            this.logStats = logStats;
        }

        @SuppressWarnings("all")
        public OverviewStats getOverviewStats() {
            return this.overviewStats;
        }

        @SuppressWarnings("all")
        public void setOverviewStats(final OverviewStats overviewStats) {
            this.overviewStats = overviewStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.ReportResponse))
                return false;
            final StatisticsResponse.ReportResponse other = (StatisticsResponse.ReportResponse) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$userStats = this.getUserStats();
            final java.lang.Object other$userStats = other.getUserStats();
            if (this$userStats == null ? other$userStats != null : !this$userStats.equals(other$userStats))
                return false;
            final java.lang.Object this$logStats = this.getLogStats();
            final java.lang.Object other$logStats = other.getLogStats();
            if (this$logStats == null ? other$logStats != null : !this$logStats.equals(other$logStats))
                return false;
            final java.lang.Object this$overviewStats = this.getOverviewStats();
            final java.lang.Object other$overviewStats = other.getOverviewStats();
            if (this$overviewStats == null ? other$overviewStats != null
                    : !this$overviewStats.equals(other$overviewStats))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.ReportResponse;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $userStats = this.getUserStats();
            result = result * PRIME + ($userStats == null ? 43 : $userStats.hashCode());
            final java.lang.Object $logStats = this.getLogStats();
            result = result * PRIME + ($logStats == null ? 43 : $logStats.hashCode());
            final java.lang.Object $overviewStats = this.getOverviewStats();
            result = result * PRIME + ($overviewStats == null ? 43 : $overviewStats.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.ReportResponse(userStats=" + this.getUserStats() + ", logStats="
                    + this.getLogStats() + ", overviewStats=" + this.getOverviewStats() + ")";
        }

        @SuppressWarnings("all")
        public static class ReportResponseBuilder {

            @SuppressWarnings("all")
            private UserStats userStats;

            @SuppressWarnings("all")
            private LogStats logStats;

            @SuppressWarnings("all")
            private OverviewStats overviewStats;

            @SuppressWarnings("all")
            ReportResponseBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ReportResponse.ReportResponseBuilder userStats(final UserStats userStats) {
                this.userStats = userStats;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ReportResponse.ReportResponseBuilder logStats(final LogStats logStats) {
                this.logStats = logStats;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ReportResponse.ReportResponseBuilder overviewStats(
                    final OverviewStats overviewStats) {
                this.overviewStats = overviewStats;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.ReportResponse build() {
                return new StatisticsResponse.ReportResponse(this.userStats, this.logStats, this.overviewStats);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.ReportResponse.ReportResponseBuilder(userStats=" + this.userStats
                        + ", logStats=" + this.logStats + ", overviewStats=" + this.overviewStats + ")";
            }

        }

    }

    /**
     * 用户统计
     */
    @Schema(description = "用户统计")
    public static class UserStats {

        @Schema(description = "用户总数")
        private long totalUsers;

        @Schema(description = "今日新增用户")
        private long newUsersToday;

        @Schema(description = "今日活跃用户")
        private long activeUsersToday;

        @Schema(description = "部门分布")
        private List<ChartData> deptDistribution;

        @SuppressWarnings("all")
        public UserStats() {
        }

        @SuppressWarnings("all")
        public UserStats(final long totalUsers, final long newUsersToday, final long activeUsersToday,
                         final List<ChartData> deptDistribution) {
            this.totalUsers = totalUsers;
            this.newUsersToday = newUsersToday;
            this.activeUsersToday = activeUsersToday;
            this.deptDistribution = deptDistribution;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.UserStats.UserStatsBuilder builder() {
            return new StatisticsResponse.UserStats.UserStatsBuilder();
        }

        @SuppressWarnings("all")
        public long getTotalUsers() {
            return this.totalUsers;
        }

        @SuppressWarnings("all")
        public void setTotalUsers(final long totalUsers) {
            this.totalUsers = totalUsers;
        }

        @SuppressWarnings("all")
        public long getNewUsersToday() {
            return this.newUsersToday;
        }

        @SuppressWarnings("all")
        public void setNewUsersToday(final long newUsersToday) {
            this.newUsersToday = newUsersToday;
        }

        @SuppressWarnings("all")
        public long getActiveUsersToday() {
            return this.activeUsersToday;
        }

        @SuppressWarnings("all")
        public void setActiveUsersToday(final long activeUsersToday) {
            this.activeUsersToday = activeUsersToday;
        }

        @SuppressWarnings("all")
        public List<ChartData> getDeptDistribution() {
            return this.deptDistribution;
        }

        @SuppressWarnings("all")
        public void setDeptDistribution(final List<ChartData> deptDistribution) {
            this.deptDistribution = deptDistribution;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.UserStats))
                return false;
            final StatisticsResponse.UserStats other = (StatisticsResponse.UserStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTotalUsers() != other.getTotalUsers())
                return false;
            if (this.getNewUsersToday() != other.getNewUsersToday())
                return false;
            if (this.getActiveUsersToday() != other.getActiveUsersToday())
                return false;
            final java.lang.Object this$deptDistribution = this.getDeptDistribution();
            final java.lang.Object other$deptDistribution = other.getDeptDistribution();
            if (this$deptDistribution == null ? other$deptDistribution != null
                    : !this$deptDistribution.equals(other$deptDistribution))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.UserStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $totalUsers = this.getTotalUsers();
            result = result * PRIME + (int) ($totalUsers >>> 32 ^ $totalUsers);
            final long $newUsersToday = this.getNewUsersToday();
            result = result * PRIME + (int) ($newUsersToday >>> 32 ^ $newUsersToday);
            final long $activeUsersToday = this.getActiveUsersToday();
            result = result * PRIME + (int) ($activeUsersToday >>> 32 ^ $activeUsersToday);
            final java.lang.Object $deptDistribution = this.getDeptDistribution();
            result = result * PRIME + ($deptDistribution == null ? 43 : $deptDistribution.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.UserStats(totalUsers=" + this.getTotalUsers() + ", newUsersToday="
                    + this.getNewUsersToday() + ", activeUsersToday=" + this.getActiveUsersToday()
                    + ", deptDistribution=" + this.getDeptDistribution() + ")";
        }

        @SuppressWarnings("all")
        public static class UserStatsBuilder {

            @SuppressWarnings("all")
            private long totalUsers;

            @SuppressWarnings("all")
            private long newUsersToday;

            @SuppressWarnings("all")
            private long activeUsersToday;

            @SuppressWarnings("all")
            private List<ChartData> deptDistribution;

            @SuppressWarnings("all")
            UserStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UserStats.UserStatsBuilder totalUsers(final long totalUsers) {
                this.totalUsers = totalUsers;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UserStats.UserStatsBuilder newUsersToday(final long newUsersToday) {
                this.newUsersToday = newUsersToday;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UserStats.UserStatsBuilder activeUsersToday(final long activeUsersToday) {
                this.activeUsersToday = activeUsersToday;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UserStats.UserStatsBuilder deptDistribution(
                    final List<ChartData> deptDistribution) {
                this.deptDistribution = deptDistribution;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.UserStats build() {
                return new StatisticsResponse.UserStats(this.totalUsers, this.newUsersToday, this.activeUsersToday,
                        this.deptDistribution);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.UserStats.UserStatsBuilder(totalUsers=" + this.totalUsers
                        + ", newUsersToday=" + this.newUsersToday + ", activeUsersToday=" + this.activeUsersToday
                        + ", deptDistribution=" + this.deptDistribution + ")";
            }

        }

    }

    /**
     * 日志统计
     */
    @Schema(description = "日志统计")
    public static class LogStats {

        @Schema(description = "登录日志总数")
        private long loginLogCount;

        @Schema(description = "操作日志总数")
        private long operateLogCount;

        @Schema(description = "登录成功数")
        private long loginSuccessCount;

        @Schema(description = "登录失败数")
        private long loginFailCount;

        @Schema(description = "操作成功数")
        private long operateSuccessCount;

        @Schema(description = "操作失败数")
        private long operateFailCount;

        @Schema(description = "登录趋势")
        private List<TrendData> loginTrend;

        @Schema(description = "操作趋势")
        private List<TrendData> operateTrend;

        @Schema(description = "模块操作统计")
        private List<ChartData> moduleStats;

        @SuppressWarnings("all")
        public LogStats() {
        }

        @SuppressWarnings("all")
        public LogStats(final long loginLogCount, final long operateLogCount, final long loginSuccessCount,
                        final long loginFailCount, final long operateSuccessCount, final long operateFailCount,
                        final List<TrendData> loginTrend, final List<TrendData> operateTrend,
                        final List<ChartData> moduleStats) {
            this.loginLogCount = loginLogCount;
            this.operateLogCount = operateLogCount;
            this.loginSuccessCount = loginSuccessCount;
            this.loginFailCount = loginFailCount;
            this.operateSuccessCount = operateSuccessCount;
            this.operateFailCount = operateFailCount;
            this.loginTrend = loginTrend;
            this.operateTrend = operateTrend;
            this.moduleStats = moduleStats;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.LogStats.LogStatsBuilder builder() {
            return new StatisticsResponse.LogStats.LogStatsBuilder();
        }

        @SuppressWarnings("all")
        public long getLoginLogCount() {
            return this.loginLogCount;
        }

        @SuppressWarnings("all")
        public void setLoginLogCount(final long loginLogCount) {
            this.loginLogCount = loginLogCount;
        }

        @SuppressWarnings("all")
        public long getOperateLogCount() {
            return this.operateLogCount;
        }

        @SuppressWarnings("all")
        public void setOperateLogCount(final long operateLogCount) {
            this.operateLogCount = operateLogCount;
        }

        @SuppressWarnings("all")
        public long getLoginSuccessCount() {
            return this.loginSuccessCount;
        }

        @SuppressWarnings("all")
        public void setLoginSuccessCount(final long loginSuccessCount) {
            this.loginSuccessCount = loginSuccessCount;
        }

        @SuppressWarnings("all")
        public long getLoginFailCount() {
            return this.loginFailCount;
        }

        @SuppressWarnings("all")
        public void setLoginFailCount(final long loginFailCount) {
            this.loginFailCount = loginFailCount;
        }

        @SuppressWarnings("all")
        public long getOperateSuccessCount() {
            return this.operateSuccessCount;
        }

        @SuppressWarnings("all")
        public void setOperateSuccessCount(final long operateSuccessCount) {
            this.operateSuccessCount = operateSuccessCount;
        }

        @SuppressWarnings("all")
        public long getOperateFailCount() {
            return this.operateFailCount;
        }

        @SuppressWarnings("all")
        public void setOperateFailCount(final long operateFailCount) {
            this.operateFailCount = operateFailCount;
        }

        @SuppressWarnings("all")
        public List<TrendData> getLoginTrend() {
            return this.loginTrend;
        }

        @SuppressWarnings("all")
        public void setLoginTrend(final List<TrendData> loginTrend) {
            this.loginTrend = loginTrend;
        }

        @SuppressWarnings("all")
        public List<TrendData> getOperateTrend() {
            return this.operateTrend;
        }

        @SuppressWarnings("all")
        public void setOperateTrend(final List<TrendData> operateTrend) {
            this.operateTrend = operateTrend;
        }

        @SuppressWarnings("all")
        public List<ChartData> getModuleStats() {
            return this.moduleStats;
        }

        @SuppressWarnings("all")
        public void setModuleStats(final List<ChartData> moduleStats) {
            this.moduleStats = moduleStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.LogStats))
                return false;
            final StatisticsResponse.LogStats other = (StatisticsResponse.LogStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getLoginLogCount() != other.getLoginLogCount())
                return false;
            if (this.getOperateLogCount() != other.getOperateLogCount())
                return false;
            if (this.getLoginSuccessCount() != other.getLoginSuccessCount())
                return false;
            if (this.getLoginFailCount() != other.getLoginFailCount())
                return false;
            if (this.getOperateSuccessCount() != other.getOperateSuccessCount())
                return false;
            if (this.getOperateFailCount() != other.getOperateFailCount())
                return false;
            final java.lang.Object this$loginTrend = this.getLoginTrend();
            final java.lang.Object other$loginTrend = other.getLoginTrend();
            if (this$loginTrend == null ? other$loginTrend != null : !this$loginTrend.equals(other$loginTrend))
                return false;
            final java.lang.Object this$operateTrend = this.getOperateTrend();
            final java.lang.Object other$operateTrend = other.getOperateTrend();
            if (this$operateTrend == null ? other$operateTrend != null : !this$operateTrend.equals(other$operateTrend))
                return false;
            final java.lang.Object this$moduleStats = this.getModuleStats();
            final java.lang.Object other$moduleStats = other.getModuleStats();
            if (this$moduleStats == null ? other$moduleStats != null : !this$moduleStats.equals(other$moduleStats))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.LogStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $loginLogCount = this.getLoginLogCount();
            result = result * PRIME + (int) ($loginLogCount >>> 32 ^ $loginLogCount);
            final long $operateLogCount = this.getOperateLogCount();
            result = result * PRIME + (int) ($operateLogCount >>> 32 ^ $operateLogCount);
            final long $loginSuccessCount = this.getLoginSuccessCount();
            result = result * PRIME + (int) ($loginSuccessCount >>> 32 ^ $loginSuccessCount);
            final long $loginFailCount = this.getLoginFailCount();
            result = result * PRIME + (int) ($loginFailCount >>> 32 ^ $loginFailCount);
            final long $operateSuccessCount = this.getOperateSuccessCount();
            result = result * PRIME + (int) ($operateSuccessCount >>> 32 ^ $operateSuccessCount);
            final long $operateFailCount = this.getOperateFailCount();
            result = result * PRIME + (int) ($operateFailCount >>> 32 ^ $operateFailCount);
            final java.lang.Object $loginTrend = this.getLoginTrend();
            result = result * PRIME + ($loginTrend == null ? 43 : $loginTrend.hashCode());
            final java.lang.Object $operateTrend = this.getOperateTrend();
            result = result * PRIME + ($operateTrend == null ? 43 : $operateTrend.hashCode());
            final java.lang.Object $moduleStats = this.getModuleStats();
            result = result * PRIME + ($moduleStats == null ? 43 : $moduleStats.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.LogStats(loginLogCount=" + this.getLoginLogCount() + ", operateLogCount="
                    + this.getOperateLogCount() + ", loginSuccessCount=" + this.getLoginSuccessCount()
                    + ", loginFailCount=" + this.getLoginFailCount() + ", operateSuccessCount="
                    + this.getOperateSuccessCount() + ", operateFailCount=" + this.getOperateFailCount()
                    + ", loginTrend=" + this.getLoginTrend() + ", operateTrend=" + this.getOperateTrend()
                    + ", moduleStats=" + this.getModuleStats() + ")";
        }

        @SuppressWarnings("all")
        public static class LogStatsBuilder {

            @SuppressWarnings("all")
            private long loginLogCount;

            @SuppressWarnings("all")
            private long operateLogCount;

            @SuppressWarnings("all")
            private long loginSuccessCount;

            @SuppressWarnings("all")
            private long loginFailCount;

            @SuppressWarnings("all")
            private long operateSuccessCount;

            @SuppressWarnings("all")
            private long operateFailCount;

            @SuppressWarnings("all")
            private List<TrendData> loginTrend;

            @SuppressWarnings("all")
            private List<TrendData> operateTrend;

            @SuppressWarnings("all")
            private List<ChartData> moduleStats;

            @SuppressWarnings("all")
            LogStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder loginLogCount(final long loginLogCount) {
                this.loginLogCount = loginLogCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder operateLogCount(final long operateLogCount) {
                this.operateLogCount = operateLogCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder loginSuccessCount(final long loginSuccessCount) {
                this.loginSuccessCount = loginSuccessCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder loginFailCount(final long loginFailCount) {
                this.loginFailCount = loginFailCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder operateSuccessCount(final long operateSuccessCount) {
                this.operateSuccessCount = operateSuccessCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder operateFailCount(final long operateFailCount) {
                this.operateFailCount = operateFailCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder loginTrend(final List<TrendData> loginTrend) {
                this.loginTrend = loginTrend;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder operateTrend(final List<TrendData> operateTrend) {
                this.operateTrend = operateTrend;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.LogStats.LogStatsBuilder moduleStats(final List<ChartData> moduleStats) {
                this.moduleStats = moduleStats;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.LogStats build() {
                return new StatisticsResponse.LogStats(this.loginLogCount, this.operateLogCount, this.loginSuccessCount,
                        this.loginFailCount, this.operateSuccessCount, this.operateFailCount, this.loginTrend,
                        this.operateTrend, this.moduleStats);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.LogStats.LogStatsBuilder(loginLogCount=" + this.loginLogCount
                        + ", operateLogCount=" + this.operateLogCount + ", loginSuccessCount=" + this.loginSuccessCount
                        + ", loginFailCount=" + this.loginFailCount + ", operateSuccessCount="
                        + this.operateSuccessCount + ", operateFailCount=" + this.operateFailCount + ", loginTrend="
                        + this.loginTrend + ", operateTrend=" + this.operateTrend + ", moduleStats=" + this.moduleStats
                        + ")";
            }

        }

    }

    /**
     * 系统概览统计
     */
    @Schema(description = "系统概览统计")
    public static class OverviewStats {

        @Schema(description = "用户总数")
        private long userCount;

        @Schema(description = "角色总数")
        private long roleCount;

        @Schema(description = "部门总数")
        private long deptCount;

        @Schema(description = "岗位总数")
        private long postCount;

        @Schema(description = "菜单总数")
        private long menuCount;

        @Schema(description = "配置总数")
        private long configCount;

        @SuppressWarnings("all")
        public OverviewStats() {
        }

        @SuppressWarnings("all")
        public OverviewStats(final long userCount, final long roleCount, final long deptCount, final long postCount,
                             final long menuCount, final long configCount) {
            this.userCount = userCount;
            this.roleCount = roleCount;
            this.deptCount = deptCount;
            this.postCount = postCount;
            this.menuCount = menuCount;
            this.configCount = configCount;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.OverviewStats.OverviewStatsBuilder builder() {
            return new StatisticsResponse.OverviewStats.OverviewStatsBuilder();
        }

        @SuppressWarnings("all")
        public long getUserCount() {
            return this.userCount;
        }

        @SuppressWarnings("all")
        public void setUserCount(final long userCount) {
            this.userCount = userCount;
        }

        @SuppressWarnings("all")
        public long getRoleCount() {
            return this.roleCount;
        }

        @SuppressWarnings("all")
        public void setRoleCount(final long roleCount) {
            this.roleCount = roleCount;
        }

        @SuppressWarnings("all")
        public long getDeptCount() {
            return this.deptCount;
        }

        @SuppressWarnings("all")
        public void setDeptCount(final long deptCount) {
            this.deptCount = deptCount;
        }

        @SuppressWarnings("all")
        public long getPostCount() {
            return this.postCount;
        }

        @SuppressWarnings("all")
        public void setPostCount(final long postCount) {
            this.postCount = postCount;
        }

        @SuppressWarnings("all")
        public long getMenuCount() {
            return this.menuCount;
        }

        @SuppressWarnings("all")
        public void setMenuCount(final long menuCount) {
            this.menuCount = menuCount;
        }

        @SuppressWarnings("all")
        public long getConfigCount() {
            return this.configCount;
        }

        @SuppressWarnings("all")
        public void setConfigCount(final long configCount) {
            this.configCount = configCount;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.OverviewStats))
                return false;
            final StatisticsResponse.OverviewStats other = (StatisticsResponse.OverviewStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getUserCount() != other.getUserCount())
                return false;
            if (this.getRoleCount() != other.getRoleCount())
                return false;
            if (this.getDeptCount() != other.getDeptCount())
                return false;
            if (this.getPostCount() != other.getPostCount())
                return false;
            if (this.getMenuCount() != other.getMenuCount())
                return false;
            if (this.getConfigCount() != other.getConfigCount())
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.OverviewStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $userCount = this.getUserCount();
            result = result * PRIME + (int) ($userCount >>> 32 ^ $userCount);
            final long $roleCount = this.getRoleCount();
            result = result * PRIME + (int) ($roleCount >>> 32 ^ $roleCount);
            final long $deptCount = this.getDeptCount();
            result = result * PRIME + (int) ($deptCount >>> 32 ^ $deptCount);
            final long $postCount = this.getPostCount();
            result = result * PRIME + (int) ($postCount >>> 32 ^ $postCount);
            final long $menuCount = this.getMenuCount();
            result = result * PRIME + (int) ($menuCount >>> 32 ^ $menuCount);
            final long $configCount = this.getConfigCount();
            result = result * PRIME + (int) ($configCount >>> 32 ^ $configCount);
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.OverviewStats(userCount=" + this.getUserCount() + ", roleCount="
                    + this.getRoleCount() + ", deptCount=" + this.getDeptCount() + ", postCount=" + this.getPostCount()
                    + ", menuCount=" + this.getMenuCount() + ", configCount=" + this.getConfigCount() + ")";
        }

        @SuppressWarnings("all")
        public static class OverviewStatsBuilder {

            @SuppressWarnings("all")
            private long userCount;

            @SuppressWarnings("all")
            private long roleCount;

            @SuppressWarnings("all")
            private long deptCount;

            @SuppressWarnings("all")
            private long postCount;

            @SuppressWarnings("all")
            private long menuCount;

            @SuppressWarnings("all")
            private long configCount;

            @SuppressWarnings("all")
            OverviewStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder userCount(final long userCount) {
                this.userCount = userCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder roleCount(final long roleCount) {
                this.roleCount = roleCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder deptCount(final long deptCount) {
                this.deptCount = deptCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder postCount(final long postCount) {
                this.postCount = postCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder menuCount(final long menuCount) {
                this.menuCount = menuCount;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats.OverviewStatsBuilder configCount(final long configCount) {
                this.configCount = configCount;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.OverviewStats build() {
                return new StatisticsResponse.OverviewStats(this.userCount, this.roleCount, this.deptCount,
                        this.postCount, this.menuCount, this.configCount);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.OverviewStats.OverviewStatsBuilder(userCount=" + this.userCount
                        + ", roleCount=" + this.roleCount + ", deptCount=" + this.deptCount + ", postCount="
                        + this.postCount + ", menuCount=" + this.menuCount + ", configCount=" + this.configCount + ")";
            }

        }

    }

    /**
     * 系统使用分析响应
     */
    @Schema(description = "系统使用分析响应")
    public static class UsageResponse {

        @Schema(description = "活跃度分析")
        private ActivityStats activityStats;

        @Schema(description = "功能使用分析")
        private FeatureUsageStats featureUsageStats;

        @Schema(description = "时段分析")
        private TimeDistributionStats timeDistributionStats;

        @SuppressWarnings("all")
        public UsageResponse() {
        }

        @SuppressWarnings("all")
        public UsageResponse(final ActivityStats activityStats, final FeatureUsageStats featureUsageStats,
                             final TimeDistributionStats timeDistributionStats) {
            this.activityStats = activityStats;
            this.featureUsageStats = featureUsageStats;
            this.timeDistributionStats = timeDistributionStats;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.UsageResponse.UsageResponseBuilder builder() {
            return new StatisticsResponse.UsageResponse.UsageResponseBuilder();
        }

        @SuppressWarnings("all")
        public ActivityStats getActivityStats() {
            return this.activityStats;
        }

        @SuppressWarnings("all")
        public void setActivityStats(final ActivityStats activityStats) {
            this.activityStats = activityStats;
        }

        @SuppressWarnings("all")
        public FeatureUsageStats getFeatureUsageStats() {
            return this.featureUsageStats;
        }

        @SuppressWarnings("all")
        public void setFeatureUsageStats(final FeatureUsageStats featureUsageStats) {
            this.featureUsageStats = featureUsageStats;
        }

        @SuppressWarnings("all")
        public TimeDistributionStats getTimeDistributionStats() {
            return this.timeDistributionStats;
        }

        @SuppressWarnings("all")
        public void setTimeDistributionStats(final TimeDistributionStats timeDistributionStats) {
            this.timeDistributionStats = timeDistributionStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.UsageResponse))
                return false;
            final StatisticsResponse.UsageResponse other = (StatisticsResponse.UsageResponse) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$activityStats = this.getActivityStats();
            final java.lang.Object other$activityStats = other.getActivityStats();
            if (this$activityStats == null ? other$activityStats != null
                    : !this$activityStats.equals(other$activityStats))
                return false;
            final java.lang.Object this$featureUsageStats = this.getFeatureUsageStats();
            final java.lang.Object other$featureUsageStats = other.getFeatureUsageStats();
            if (this$featureUsageStats == null ? other$featureUsageStats != null
                    : !this$featureUsageStats.equals(other$featureUsageStats))
                return false;
            final java.lang.Object this$timeDistributionStats = this.getTimeDistributionStats();
            final java.lang.Object other$timeDistributionStats = other.getTimeDistributionStats();
            if (this$timeDistributionStats == null ? other$timeDistributionStats != null
                    : !this$timeDistributionStats.equals(other$timeDistributionStats))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.UsageResponse;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $activityStats = this.getActivityStats();
            result = result * PRIME + ($activityStats == null ? 43 : $activityStats.hashCode());
            final java.lang.Object $featureUsageStats = this.getFeatureUsageStats();
            result = result * PRIME + ($featureUsageStats == null ? 43 : $featureUsageStats.hashCode());
            final java.lang.Object $timeDistributionStats = this.getTimeDistributionStats();
            result = result * PRIME + ($timeDistributionStats == null ? 43 : $timeDistributionStats.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.UsageResponse(activityStats=" + this.getActivityStats() + ", featureUsageStats="
                    + this.getFeatureUsageStats() + ", timeDistributionStats=" + this.getTimeDistributionStats() + ")";
        }

        @SuppressWarnings("all")
        public static class UsageResponseBuilder {

            @SuppressWarnings("all")
            private ActivityStats activityStats;

            @SuppressWarnings("all")
            private FeatureUsageStats featureUsageStats;

            @SuppressWarnings("all")
            private TimeDistributionStats timeDistributionStats;

            @SuppressWarnings("all")
            UsageResponseBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UsageResponse.UsageResponseBuilder activityStats(
                    final ActivityStats activityStats) {
                this.activityStats = activityStats;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UsageResponse.UsageResponseBuilder featureUsageStats(
                    final FeatureUsageStats featureUsageStats) {
                this.featureUsageStats = featureUsageStats;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.UsageResponse.UsageResponseBuilder timeDistributionStats(
                    final TimeDistributionStats timeDistributionStats) {
                this.timeDistributionStats = timeDistributionStats;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.UsageResponse build() {
                return new StatisticsResponse.UsageResponse(this.activityStats, this.featureUsageStats,
                        this.timeDistributionStats);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.UsageResponse.UsageResponseBuilder(activityStats=" + this.activityStats
                        + ", featureUsageStats=" + this.featureUsageStats + ", timeDistributionStats="
                        + this.timeDistributionStats + ")";
            }

        }

    }

    /**
     * 活跃度统计
     */
    @Schema(description = "活跃度统计")
    public static class ActivityStats {

        @Schema(description = "今日活跃用户")
        private long todayActiveUsers;

        @Schema(description = "本周活跃用户")
        private long weekActiveUsers;

        @Schema(description = "本月活跃用户")
        private long monthActiveUsers;

        @Schema(description = "活跃用户趋势")
        private List<TrendData> activeUserTrend;

        @Schema(description = "活跃用户排行")
        private List<RankData> topActiveUsers;

        @SuppressWarnings("all")
        public ActivityStats() {
        }

        @SuppressWarnings("all")
        public ActivityStats(final long todayActiveUsers, final long weekActiveUsers, final long monthActiveUsers,
                             final List<TrendData> activeUserTrend, final List<RankData> topActiveUsers) {
            this.todayActiveUsers = todayActiveUsers;
            this.weekActiveUsers = weekActiveUsers;
            this.monthActiveUsers = monthActiveUsers;
            this.activeUserTrend = activeUserTrend;
            this.topActiveUsers = topActiveUsers;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.ActivityStats.ActivityStatsBuilder builder() {
            return new StatisticsResponse.ActivityStats.ActivityStatsBuilder();
        }

        @SuppressWarnings("all")
        public long getTodayActiveUsers() {
            return this.todayActiveUsers;
        }

        @SuppressWarnings("all")
        public void setTodayActiveUsers(final long todayActiveUsers) {
            this.todayActiveUsers = todayActiveUsers;
        }

        @SuppressWarnings("all")
        public long getWeekActiveUsers() {
            return this.weekActiveUsers;
        }

        @SuppressWarnings("all")
        public void setWeekActiveUsers(final long weekActiveUsers) {
            this.weekActiveUsers = weekActiveUsers;
        }

        @SuppressWarnings("all")
        public long getMonthActiveUsers() {
            return this.monthActiveUsers;
        }

        @SuppressWarnings("all")
        public void setMonthActiveUsers(final long monthActiveUsers) {
            this.monthActiveUsers = monthActiveUsers;
        }

        @SuppressWarnings("all")
        public List<TrendData> getActiveUserTrend() {
            return this.activeUserTrend;
        }

        @SuppressWarnings("all")
        public void setActiveUserTrend(final List<TrendData> activeUserTrend) {
            this.activeUserTrend = activeUserTrend;
        }

        @SuppressWarnings("all")
        public List<RankData> getTopActiveUsers() {
            return this.topActiveUsers;
        }

        @SuppressWarnings("all")
        public void setTopActiveUsers(final List<RankData> topActiveUsers) {
            this.topActiveUsers = topActiveUsers;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.ActivityStats))
                return false;
            final StatisticsResponse.ActivityStats other = (StatisticsResponse.ActivityStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getTodayActiveUsers() != other.getTodayActiveUsers())
                return false;
            if (this.getWeekActiveUsers() != other.getWeekActiveUsers())
                return false;
            if (this.getMonthActiveUsers() != other.getMonthActiveUsers())
                return false;
            final java.lang.Object this$activeUserTrend = this.getActiveUserTrend();
            final java.lang.Object other$activeUserTrend = other.getActiveUserTrend();
            if (this$activeUserTrend == null ? other$activeUserTrend != null
                    : !this$activeUserTrend.equals(other$activeUserTrend))
                return false;
            final java.lang.Object this$topActiveUsers = this.getTopActiveUsers();
            final java.lang.Object other$topActiveUsers = other.getTopActiveUsers();
            if (this$topActiveUsers == null ? other$topActiveUsers != null
                    : !this$topActiveUsers.equals(other$topActiveUsers))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.ActivityStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $todayActiveUsers = this.getTodayActiveUsers();
            result = result * PRIME + (int) ($todayActiveUsers >>> 32 ^ $todayActiveUsers);
            final long $weekActiveUsers = this.getWeekActiveUsers();
            result = result * PRIME + (int) ($weekActiveUsers >>> 32 ^ $weekActiveUsers);
            final long $monthActiveUsers = this.getMonthActiveUsers();
            result = result * PRIME + (int) ($monthActiveUsers >>> 32 ^ $monthActiveUsers);
            final java.lang.Object $activeUserTrend = this.getActiveUserTrend();
            result = result * PRIME + ($activeUserTrend == null ? 43 : $activeUserTrend.hashCode());
            final java.lang.Object $topActiveUsers = this.getTopActiveUsers();
            result = result * PRIME + ($topActiveUsers == null ? 43 : $topActiveUsers.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.ActivityStats(todayActiveUsers=" + this.getTodayActiveUsers()
                    + ", weekActiveUsers=" + this.getWeekActiveUsers() + ", monthActiveUsers="
                    + this.getMonthActiveUsers() + ", activeUserTrend=" + this.getActiveUserTrend()
                    + ", topActiveUsers=" + this.getTopActiveUsers() + ")";
        }

        @SuppressWarnings("all")
        public static class ActivityStatsBuilder {

            @SuppressWarnings("all")
            private long todayActiveUsers;

            @SuppressWarnings("all")
            private long weekActiveUsers;

            @SuppressWarnings("all")
            private long monthActiveUsers;

            @SuppressWarnings("all")
            private List<TrendData> activeUserTrend;

            @SuppressWarnings("all")
            private List<RankData> topActiveUsers;

            @SuppressWarnings("all")
            ActivityStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats.ActivityStatsBuilder todayActiveUsers(final long todayActiveUsers) {
                this.todayActiveUsers = todayActiveUsers;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats.ActivityStatsBuilder weekActiveUsers(final long weekActiveUsers) {
                this.weekActiveUsers = weekActiveUsers;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats.ActivityStatsBuilder monthActiveUsers(final long monthActiveUsers) {
                this.monthActiveUsers = monthActiveUsers;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats.ActivityStatsBuilder activeUserTrend(
                    final List<TrendData> activeUserTrend) {
                this.activeUserTrend = activeUserTrend;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats.ActivityStatsBuilder topActiveUsers(
                    final List<RankData> topActiveUsers) {
                this.topActiveUsers = topActiveUsers;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.ActivityStats build() {
                return new StatisticsResponse.ActivityStats(this.todayActiveUsers, this.weekActiveUsers,
                        this.monthActiveUsers, this.activeUserTrend, this.topActiveUsers);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.ActivityStats.ActivityStatsBuilder(todayActiveUsers=" + this.todayActiveUsers
                        + ", weekActiveUsers=" + this.weekActiveUsers + ", monthActiveUsers=" + this.monthActiveUsers
                        + ", activeUserTrend=" + this.activeUserTrend + ", topActiveUsers=" + this.topActiveUsers + ")";
            }

        }

    }

    /**
     * 功能使用统计
     */
    @Schema(description = "功能使用统计")
    public static class FeatureUsageStats {

        @Schema(description = "模块访问排行")
        private List<ChartData> moduleRanking;

        @Schema(description = "API调用统计")
        private List<ChartData> apiCallStats;

        @SuppressWarnings("all")
        public FeatureUsageStats() {
        }

        @SuppressWarnings("all")
        public FeatureUsageStats(final List<ChartData> moduleRanking, final List<ChartData> apiCallStats) {
            this.moduleRanking = moduleRanking;
            this.apiCallStats = apiCallStats;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.FeatureUsageStats.FeatureUsageStatsBuilder builder() {
            return new StatisticsResponse.FeatureUsageStats.FeatureUsageStatsBuilder();
        }

        @SuppressWarnings("all")
        public List<ChartData> getModuleRanking() {
            return this.moduleRanking;
        }

        @SuppressWarnings("all")
        public void setModuleRanking(final List<ChartData> moduleRanking) {
            this.moduleRanking = moduleRanking;
        }

        @SuppressWarnings("all")
        public List<ChartData> getApiCallStats() {
            return this.apiCallStats;
        }

        @SuppressWarnings("all")
        public void setApiCallStats(final List<ChartData> apiCallStats) {
            this.apiCallStats = apiCallStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.FeatureUsageStats))
                return false;
            final StatisticsResponse.FeatureUsageStats other = (StatisticsResponse.FeatureUsageStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$moduleRanking = this.getModuleRanking();
            final java.lang.Object other$moduleRanking = other.getModuleRanking();
            if (this$moduleRanking == null ? other$moduleRanking != null
                    : !this$moduleRanking.equals(other$moduleRanking))
                return false;
            final java.lang.Object this$apiCallStats = this.getApiCallStats();
            final java.lang.Object other$apiCallStats = other.getApiCallStats();
            if (this$apiCallStats == null ? other$apiCallStats != null : !this$apiCallStats.equals(other$apiCallStats))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.FeatureUsageStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $moduleRanking = this.getModuleRanking();
            result = result * PRIME + ($moduleRanking == null ? 43 : $moduleRanking.hashCode());
            final java.lang.Object $apiCallStats = this.getApiCallStats();
            result = result * PRIME + ($apiCallStats == null ? 43 : $apiCallStats.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.FeatureUsageStats(moduleRanking=" + this.getModuleRanking() + ", apiCallStats="
                    + this.getApiCallStats() + ")";
        }

        @SuppressWarnings("all")
        public static class FeatureUsageStatsBuilder {

            @SuppressWarnings("all")
            private List<ChartData> moduleRanking;

            @SuppressWarnings("all")
            private List<ChartData> apiCallStats;

            @SuppressWarnings("all")
            FeatureUsageStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.FeatureUsageStats.FeatureUsageStatsBuilder moduleRanking(
                    final List<ChartData> moduleRanking) {
                this.moduleRanking = moduleRanking;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.FeatureUsageStats.FeatureUsageStatsBuilder apiCallStats(
                    final List<ChartData> apiCallStats) {
                this.apiCallStats = apiCallStats;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.FeatureUsageStats build() {
                return new StatisticsResponse.FeatureUsageStats(this.moduleRanking, this.apiCallStats);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.FeatureUsageStats.FeatureUsageStatsBuilder(moduleRanking="
                        + this.moduleRanking + ", apiCallStats=" + this.apiCallStats + ")";
            }

        }

    }

    /**
     * 时段分布统计
     */
    @Schema(description = "时段分布统计")
    public static class TimeDistributionStats {

        @Schema(description = "24小时访问分布")
        private List<HourData> hourlyDistribution;

        @Schema(description = "高峰时段")
        private String peakHours;

        @SuppressWarnings("all")
        public TimeDistributionStats() {
        }

        @SuppressWarnings("all")
        public TimeDistributionStats(final List<HourData> hourlyDistribution, final String peakHours) {
            this.hourlyDistribution = hourlyDistribution;
            this.peakHours = peakHours;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.TimeDistributionStats.TimeDistributionStatsBuilder builder() {
            return new StatisticsResponse.TimeDistributionStats.TimeDistributionStatsBuilder();
        }

        @SuppressWarnings("all")
        public List<HourData> getHourlyDistribution() {
            return this.hourlyDistribution;
        }

        @SuppressWarnings("all")
        public void setHourlyDistribution(final List<HourData> hourlyDistribution) {
            this.hourlyDistribution = hourlyDistribution;
        }

        @SuppressWarnings("all")
        public String getPeakHours() {
            return this.peakHours;
        }

        @SuppressWarnings("all")
        public void setPeakHours(final String peakHours) {
            this.peakHours = peakHours;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.TimeDistributionStats))
                return false;
            final StatisticsResponse.TimeDistributionStats other = (StatisticsResponse.TimeDistributionStats) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$hourlyDistribution = this.getHourlyDistribution();
            final java.lang.Object other$hourlyDistribution = other.getHourlyDistribution();
            if (this$hourlyDistribution == null ? other$hourlyDistribution != null
                    : !this$hourlyDistribution.equals(other$hourlyDistribution))
                return false;
            final java.lang.Object this$peakHours = this.getPeakHours();
            final java.lang.Object other$peakHours = other.getPeakHours();
            if (this$peakHours == null ? other$peakHours != null : !this$peakHours.equals(other$peakHours))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.TimeDistributionStats;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $hourlyDistribution = this.getHourlyDistribution();
            result = result * PRIME + ($hourlyDistribution == null ? 43 : $hourlyDistribution.hashCode());
            final java.lang.Object $peakHours = this.getPeakHours();
            result = result * PRIME + ($peakHours == null ? 43 : $peakHours.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.TimeDistributionStats(hourlyDistribution=" + this.getHourlyDistribution()
                    + ", peakHours=" + this.getPeakHours() + ")";
        }

        @SuppressWarnings("all")
        public static class TimeDistributionStatsBuilder {

            @SuppressWarnings("all")
            private List<HourData> hourlyDistribution;

            @SuppressWarnings("all")
            private String peakHours;

            @SuppressWarnings("all")
            TimeDistributionStatsBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.TimeDistributionStats.TimeDistributionStatsBuilder hourlyDistribution(
                    final List<HourData> hourlyDistribution) {
                this.hourlyDistribution = hourlyDistribution;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.TimeDistributionStats.TimeDistributionStatsBuilder peakHours(
                    final String peakHours) {
                this.peakHours = peakHours;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.TimeDistributionStats build() {
                return new StatisticsResponse.TimeDistributionStats(this.hourlyDistribution, this.peakHours);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.TimeDistributionStats.TimeDistributionStatsBuilder(hourlyDistribution="
                        + this.hourlyDistribution + ", peakHours=" + this.peakHours + ")";
            }

        }

    }

    /**
     * 图表数据
     */
    @Schema(description = "图表数据")
    public static class ChartData {

        @Schema(description = "名称/标签")
        private String name;

        @Schema(description = "数值")
        private long value;

        @SuppressWarnings("all")
        public ChartData() {
        }

        @SuppressWarnings("all")
        public ChartData(final String name, final long value) {
            this.name = name;
            this.value = value;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.ChartData.ChartDataBuilder builder() {
            return new StatisticsResponse.ChartData.ChartDataBuilder();
        }

        @SuppressWarnings("all")
        public String getName() {
            return this.name;
        }

        @SuppressWarnings("all")
        public void setName(final String name) {
            this.name = name;
        }

        @SuppressWarnings("all")
        public long getValue() {
            return this.value;
        }

        @SuppressWarnings("all")
        public void setValue(final long value) {
            this.value = value;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.ChartData))
                return false;
            final StatisticsResponse.ChartData other = (StatisticsResponse.ChartData) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getValue() != other.getValue())
                return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.ChartData;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $value = this.getValue();
            result = result * PRIME + (int) ($value >>> 32 ^ $value);
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.ChartData(name=" + this.getName() + ", value=" + this.getValue() + ")";
        }

        @SuppressWarnings("all")
        public static class ChartDataBuilder {

            @SuppressWarnings("all")
            private String name;

            @SuppressWarnings("all")
            private long value;

            @SuppressWarnings("all")
            ChartDataBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ChartData.ChartDataBuilder name(final String name) {
                this.name = name;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.ChartData.ChartDataBuilder value(final long value) {
                this.value = value;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.ChartData build() {
                return new StatisticsResponse.ChartData(this.name, this.value);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.ChartData.ChartDataBuilder(name=" + this.name + ", value=" + this.value
                        + ")";
            }

        }

    }

    /**
     * 趋势数据
     */
    @Schema(description = "趋势数据")
    public static class TrendData {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "数值")
        private long value;

        @SuppressWarnings("all")
        public TrendData() {
        }

        @SuppressWarnings("all")
        public TrendData(final String date, final long value) {
            this.date = date;
            this.value = value;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.TrendData.TrendDataBuilder builder() {
            return new StatisticsResponse.TrendData.TrendDataBuilder();
        }

        @SuppressWarnings("all")
        public String getDate() {
            return this.date;
        }

        @SuppressWarnings("all")
        public void setDate(final String date) {
            this.date = date;
        }

        @SuppressWarnings("all")
        public long getValue() {
            return this.value;
        }

        @SuppressWarnings("all")
        public void setValue(final long value) {
            this.value = value;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.TrendData))
                return false;
            final StatisticsResponse.TrendData other = (StatisticsResponse.TrendData) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getValue() != other.getValue())
                return false;
            final java.lang.Object this$date = this.getDate();
            final java.lang.Object other$date = other.getDate();
            if (this$date == null ? other$date != null : !this$date.equals(other$date))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.TrendData;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $value = this.getValue();
            result = result * PRIME + (int) ($value >>> 32 ^ $value);
            final java.lang.Object $date = this.getDate();
            result = result * PRIME + ($date == null ? 43 : $date.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.TrendData(date=" + this.getDate() + ", value=" + this.getValue() + ")";
        }

        @SuppressWarnings("all")
        public static class TrendDataBuilder {

            @SuppressWarnings("all")
            private String date;

            @SuppressWarnings("all")
            private long value;

            @SuppressWarnings("all")
            TrendDataBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.TrendData.TrendDataBuilder date(final String date) {
                this.date = date;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.TrendData.TrendDataBuilder value(final long value) {
                this.value = value;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.TrendData build() {
                return new StatisticsResponse.TrendData(this.date, this.value);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.TrendData.TrendDataBuilder(date=" + this.date + ", value=" + this.value
                        + ")";
            }

        }

    }

    /**
     * 排行数据
     */
    @Schema(description = "排行数据")
    public static class RankData {

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "操作数")
        private long count;

        @SuppressWarnings("all")
        public RankData() {
        }

        @SuppressWarnings("all")
        public RankData(final String username, final long count) {
            this.username = username;
            this.count = count;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.RankData.RankDataBuilder builder() {
            return new StatisticsResponse.RankData.RankDataBuilder();
        }

        @SuppressWarnings("all")
        public String getUsername() {
            return this.username;
        }

        @SuppressWarnings("all")
        public void setUsername(final String username) {
            this.username = username;
        }

        @SuppressWarnings("all")
        public long getCount() {
            return this.count;
        }

        @SuppressWarnings("all")
        public void setCount(final long count) {
            this.count = count;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.RankData))
                return false;
            final StatisticsResponse.RankData other = (StatisticsResponse.RankData) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getCount() != other.getCount())
                return false;
            final java.lang.Object this$username = this.getUsername();
            final java.lang.Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.RankData;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final long $count = this.getCount();
            result = result * PRIME + (int) ($count >>> 32 ^ $count);
            final java.lang.Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.RankData(username=" + this.getUsername() + ", count=" + this.getCount() + ")";
        }

        @SuppressWarnings("all")
        public static class RankDataBuilder {

            @SuppressWarnings("all")
            private String username;

            @SuppressWarnings("all")
            private long count;

            @SuppressWarnings("all")
            RankDataBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.RankData.RankDataBuilder username(final String username) {
                this.username = username;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.RankData.RankDataBuilder count(final long count) {
                this.count = count;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.RankData build() {
                return new StatisticsResponse.RankData(this.username, this.count);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.RankData.RankDataBuilder(username=" + this.username + ", count=" + this.count
                        + ")";
            }

        }

    }

    /**
     * 小时分布数据
     */
    @Schema(description = "小时分布数据")
    public static class HourData {

        @Schema(description = "小时 (0-23)")
        private int hour;

        @Schema(description = "数值")
        private long value;

        @SuppressWarnings("all")
        public HourData() {
        }

        @SuppressWarnings("all")
        public HourData(final int hour, final long value) {
            this.hour = hour;
            this.value = value;
        }

        @SuppressWarnings("all")
        public static StatisticsResponse.HourData.HourDataBuilder builder() {
            return new StatisticsResponse.HourData.HourDataBuilder();
        }

        @SuppressWarnings("all")
        public int getHour() {
            return this.hour;
        }

        @SuppressWarnings("all")
        public void setHour(final int hour) {
            this.hour = hour;
        }

        @SuppressWarnings("all")
        public long getValue() {
            return this.value;
        }

        @SuppressWarnings("all")
        public void setValue(final long value) {
            this.value = value;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof StatisticsResponse.HourData))
                return false;
            final StatisticsResponse.HourData other = (StatisticsResponse.HourData) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            if (this.getHour() != other.getHour())
                return false;
            if (this.getValue() != other.getValue())
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StatisticsResponse.HourData;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getHour();
            final long $value = this.getValue();
            result = result * PRIME + (int) ($value >>> 32 ^ $value);
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StatisticsResponse.HourData(hour=" + this.getHour() + ", value=" + this.getValue() + ")";
        }

        @SuppressWarnings("all")
        public static class HourDataBuilder {

            @SuppressWarnings("all")
            private int hour;

            @SuppressWarnings("all")
            private long value;

            @SuppressWarnings("all")
            HourDataBuilder() {
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.HourData.HourDataBuilder hour(final int hour) {
                this.hour = hour;
                return this;
            }

            /**
             * @return {@code this}.
             */
            @SuppressWarnings("all")
            public StatisticsResponse.HourData.HourDataBuilder value(final long value) {
                this.value = value;
                return this;
            }

            @SuppressWarnings("all")
            public StatisticsResponse.HourData build() {
                return new StatisticsResponse.HourData(this.hour, this.value);
            }

            @java.lang.Override
            @SuppressWarnings("all")
            public java.lang.String toString() {
                return "StatisticsResponse.HourData.HourDataBuilder(hour=" + this.hour + ", value=" + this.value + ")";
            }

        }

    }

}
