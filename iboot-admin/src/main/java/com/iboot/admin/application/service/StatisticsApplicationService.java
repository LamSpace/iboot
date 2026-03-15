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

import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.infrastructure.persistence.mapper.LoginLogMapper;
import com.iboot.admin.infrastructure.persistence.mapper.OperateLogMapper;
import com.iboot.admin.interfaces.dto.response.StatisticsResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析应用服务
 * <p>
 * 提供系统数据统计分析功能，包括用户统计、日志统计、系统概览、
 * 活跃度分析、功能使用统计和时段分布统计等
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsApplicationService {

    private final UserApplicationService userApplicationService;
    private final RoleApplicationService roleApplicationService;
    private final DeptApplicationService deptApplicationService;
    private final PostApplicationService postApplicationService;
    private final MenuApplicationService menuApplicationService;
    private final ConfigApplicationService configApplicationService;
    private final LoginLogMapper loginLogMapper;
    private final OperateLogMapper operateLogMapper;

    /**
     * 获取数据统计报表
     * <p>
     * 综合统计用户数据、日志数据和系统概览信息
     * </p>
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 数据统计报表响应对象
     */
    public ReportResponse getReportStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        return ReportResponse.builder()
                .userStats(getUserStats(startTime, endTime))
                .logStats(getLogStats(startTime, endTime))
                .overviewStats(getOverviewStats())
                .build();
    }

    /**
     * 获取系统使用分析
     * <p>
     * 分析系统活跃度、功能使用情况和时段分布
     * </p>
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 系统使用分析响应对象
     */
    public UsageResponse getUsageAnalysis(LocalDateTime startTime, LocalDateTime endTime) {
        return UsageResponse.builder()
                .activityStats(getActivityStats(startTime, endTime))
                .featureUsageStats(getFeatureUsageStats(startTime, endTime))
                .timeDistributionStats(getTimeDistributionStats(startTime, endTime))
                .build();
    }

    /**
     * 获取用户统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户统计对象
     */
    private UserStats getUserStats(LocalDateTime startTime, LocalDateTime endTime) {
        long totalUsers = userApplicationService.countUsers();

        // 今日开始时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 今日活跃用户（今日成功登录的独立用户数）
        long activeUsersToday = loginLogMapper.countDistinctUsers(todayStart, todayEnd);

        // 部门分布统计
        List<ChartData> deptDistribution = getDeptDistribution();

        return UserStats.builder()
                .totalUsers(totalUsers)
                .newUsersToday(userApplicationService.countUsersByCreateTime(todayStart, todayEnd))
                .activeUsersToday(activeUsersToday)
                .deptDistribution(deptDistribution)
                .build();
    }

    /**
     * 获取日志统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志统计对象
     */
    private LogStats getLogStats(LocalDateTime startTime, LocalDateTime endTime) {
        long loginLogCount = loginLogMapper.count();
        long operateLogCount = operateLogMapper.count();

        // 登录状态统计
        Map<String, Object> loginStatusMap = loginLogMapper.countByStatus(startTime, endTime);
        long loginSuccessCount = getMapLongValue(loginStatusMap, "success");
        long loginFailCount = getMapLongValue(loginStatusMap, "fail");

        // 操作状态统计
        Map<String, Object> operateStatusMap = operateLogMapper.countByStatus(startTime, endTime);
        long operateSuccessCount = getMapLongValue(operateStatusMap, "success");
        long operateFailCount = getMapLongValue(operateStatusMap, "fail");

        // 登录趋势
        List<TrendData> loginTrend = loginLogMapper.countByDate(startTime, endTime).stream()
                .map(m -> TrendData.builder()
                        .date(String.valueOf(m.get("date")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        // 操作趋势
        List<TrendData> operateTrend = operateLogMapper.countByDate(startTime, endTime).stream()
                .map(m -> TrendData.builder()
                        .date(String.valueOf(m.get("date")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        // 模块操作统计
        List<ChartData> moduleStats = operateLogMapper.countByModule(startTime, endTime).stream()
                .map(m -> ChartData.builder()
                        .name(String.valueOf(m.get("module")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        return LogStats.builder()
                .loginLogCount(loginLogCount)
                .operateLogCount(operateLogCount)
                .loginSuccessCount(loginSuccessCount)
                .loginFailCount(loginFailCount)
                .operateSuccessCount(operateSuccessCount)
                .operateFailCount(operateFailCount)
                .loginTrend(loginTrend)
                .operateTrend(operateTrend)
                .moduleStats(moduleStats)
                .build();
    }

    /**
     * 获取系统概览
     *
     * @return 系统概览对象
     */
    private OverviewStats getOverviewStats() {
        return OverviewStats.builder()
                .userCount(userApplicationService.countUsers())
                .roleCount(roleApplicationService.countRoles())
                .deptCount(deptApplicationService.countDepts())
                .postCount(postApplicationService.countPosts())
                .menuCount(menuApplicationService.countMenus())
                .configCount(configApplicationService.countConfigs())
                .build();
    }

    /**
     * 获取活跃度统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃度统计对象
     */
    private ActivityStats getActivityStats(LocalDateTime startTime, LocalDateTime endTime) {
        // 今日
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        // 本周
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1);
        LocalDateTime weekStartTime = weekStart.atStartOfDay();

        // 本月
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartTime = monthStart.atStartOfDay();

        long todayActiveUsers = loginLogMapper.countDistinctUsers(todayStart, todayEnd);
        long weekActiveUsers = loginLogMapper.countDistinctUsers(weekStartTime, todayEnd);
        long monthActiveUsers = loginLogMapper.countDistinctUsers(monthStartTime, todayEnd);

        // 活跃用户趋势（按日统计登录的独立用户数）
        List<TrendData> activeUserTrend = loginLogMapper.countByDate(startTime, endTime).stream()
                .map(m -> TrendData.builder()
                        .date(String.valueOf(m.get("date")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        // 活跃用户排行
        List<RankData> topActiveUsers = operateLogMapper.topActiveUsers(startTime, endTime, 10).stream()
                .map(m -> RankData.builder()
                        .username(String.valueOf(m.get("username")))
                        .count(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        return ActivityStats.builder()
                .todayActiveUsers(todayActiveUsers)
                .weekActiveUsers(weekActiveUsers)
                .monthActiveUsers(monthActiveUsers)
                .activeUserTrend(activeUserTrend)
                .topActiveUsers(topActiveUsers)
                .build();
    }

    /**
     * 获取功能使用统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 功能使用统计对象
     */
    private FeatureUsageStats getFeatureUsageStats(LocalDateTime startTime, LocalDateTime endTime) {
        // 模块访问排行
        List<ChartData> moduleRanking = operateLogMapper.countByModule(startTime, endTime).stream()
                .limit(10)
                .map(m -> ChartData.builder()
                        .name(String.valueOf(m.get("module")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        // API 调用统计（按请求方法分组）
        List<ChartData> apiCallStats = operateLogMapper.countByRequestMethod(startTime, endTime).stream()
                .map(m -> ChartData.builder()
                        .name(String.valueOf(m.get("name")))
                        .value(getMapLongValue(m, "count"))
                        .build())
                .collect(Collectors.toList());

        return FeatureUsageStats.builder()
                .moduleRanking(moduleRanking)
                .apiCallStats(apiCallStats)
                .build();
    }

    /**
     * 获取时段分布统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时段分布统计对象
     */
    private TimeDistributionStats getTimeDistributionStats(LocalDateTime startTime, LocalDateTime endTime) {
        // 登录小时分布
        List<Map<String, Object>> loginHourData = loginLogMapper.countByHour(startTime, endTime);
        // 操作小时分布
        List<Map<String, Object>> operateHourData = operateLogMapper.countByHour(startTime, endTime);

        // 合并小时分布
        Map<Integer, Long> hourMap = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourMap.put(i, 0L);
        }
        for (Map<String, Object> m : loginHourData) {
            int hour = ((Number) m.get("hour")).intValue();
            long count = getMapLongValue(m, "count");
            hourMap.merge(hour, count, Long::sum);
        }
        for (Map<String, Object> m : operateHourData) {
            int hour = ((Number) m.get("hour")).intValue();
            long count = getMapLongValue(m, "count");
            hourMap.merge(hour, count, Long::sum);
        }

        List<HourData> hourlyDistribution = hourMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> HourData.builder()
                        .hour(e.getKey())
                        .value(e.getValue())
                        .build())
                .collect(Collectors.toList());

        // 计算高峰时段
        String peakHours = calculatePeakHours(hourMap);

        return TimeDistributionStats.builder()
                .hourlyDistribution(hourlyDistribution)
                .peakHours(peakHours)
                .build();
    }

    /**
     * 获取部门用户分布
     *
     * @return 部门用户分布图表数据
     */
    private List<ChartData> getDeptDistribution() {
        List<Dept> allDepts = deptApplicationService.getAllDepts();
        Map<Long, Integer> userCounts = deptApplicationService.getDeptUserCounts();

        return allDepts.stream()
                .filter(dept -> userCounts.containsKey(dept.getId()))
                .map(dept -> ChartData.builder()
                        .name(dept.getDeptName())
                        .value(userCounts.getOrDefault(dept.getId(), 0).longValue())
                        .build())
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 计算高峰时段
     * <p>
     * 找出访问量最高的 3 个小时
     * </p>
     *
     * @param hourMap 小时访问量 Map
     * @return 高峰时段字符串
     */
    private String calculatePeakHours(Map<Integer, Long> hourMap) {
        if (hourMap.isEmpty()) {
            return "无数据";
        }

        // 找出访问量最高的 3 个小时
        return hourMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> String.format("%02d:00", e.getKey()))
                .collect(Collectors.joining(", "));
    }

    /**
     * 从 Map 中安全获取 Long 值
     *
     * @param map 数据 Map
     * @param key 键名
     * @return Long 值，获取失败则返回 0
     */
    private long getMapLongValue(Map<String, Object> map, String key) {
        if (map == null) {
            return 0L;
        }
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
