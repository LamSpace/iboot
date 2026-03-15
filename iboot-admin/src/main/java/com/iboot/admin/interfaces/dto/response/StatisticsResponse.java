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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "数据统计报表响应")
    public static class ReportResponse {

        @Schema(description = "用户统计")
        private UserStats userStats;

        @Schema(description = "日志统计")
        private LogStats logStats;

        @Schema(description = "系统概览")
        private OverviewStats overviewStats;
    }

    /**
     * 用户统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    }

    /**
     * 日志统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    }

    /**
     * 系统概览统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    }

    /**
     * 系统使用分析响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "系统使用分析响应")
    public static class UsageResponse {

        @Schema(description = "活跃度分析")
        private ActivityStats activityStats;

        @Schema(description = "功能使用分析")
        private FeatureUsageStats featureUsageStats;

        @Schema(description = "时段分析")
        private TimeDistributionStats timeDistributionStats;
    }

    /**
     * 活跃度统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
    }

    /**
     * 功能使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "功能使用统计")
    public static class FeatureUsageStats {

        @Schema(description = "模块访问排行")
        private List<ChartData> moduleRanking;

        @Schema(description = "API调用统计")
        private List<ChartData> apiCallStats;
    }

    /**
     * 时段分布统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时段分布统计")
    public static class TimeDistributionStats {

        @Schema(description = "24小时访问分布")
        private List<HourData> hourlyDistribution;

        @Schema(description = "高峰时段")
        private String peakHours;
    }

    /**
     * 图表数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图表数据")
    public static class ChartData {

        @Schema(description = "名称/标签")
        private String name;

        @Schema(description = "数值")
        private long value;
    }

    /**
     * 趋势数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势数据")
    public static class TrendData {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "数值")
        private long value;
    }

    /**
     * 排行数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "排行数据")
    public static class RankData {

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "操作数")
        private long count;
    }

    /**
     * 小时分布数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "小时分布数据")
    public static class HourData {

        @Schema(description = "小时 (0-23)")
        private int hour;

        @Schema(description = "数值")
        private long value;
    }
}
