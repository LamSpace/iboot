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

import com.iboot.admin.domain.system.model.LoginLog;
import com.iboot.admin.domain.system.model.OperateLog;
import com.iboot.admin.interfaces.dto.response.DashboardSummaryResponse;
import com.iboot.admin.interfaces.dto.response.LoginLogResponse;
import com.iboot.admin.interfaces.dto.response.OperateLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘应用服务
 * <p>
 * 提供 Dashboard 首页数据汇总功能，包括各模块统计数量和最新日志记录
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final UserApplicationService userApplicationService;
    private final RoleApplicationService roleApplicationService;
    private final DeptApplicationService deptApplicationService;
    private final PostApplicationService postApplicationService;
    private final LoginLogApplicationService loginLogApplicationService;
    private final OperateLogApplicationService operateLogApplicationService;

    /**
     * 获取仪表盘汇总数据
     * <p>
     * 统计用户、角色、部门、岗位、登录日志、操作日志的数量，
     * 并返回最新的 5 条登录日志和操作日志记录
     * </p>
     *
     * @return 仪表盘汇总数据响应对象
     */
    public DashboardSummaryResponse getDashboardSummary() {
        long userCount = userApplicationService.countUsers();
        long roleCount = roleApplicationService.countRoles();
        long deptCount = deptApplicationService.countDepts();
        long postCount = postApplicationService.countPosts();
        long loginLogCount = loginLogApplicationService.countLoginLogs();
        long operateLogCount = operateLogApplicationService.countOperateLogs();

        List<LoginLog> recentLoginLogs = loginLogApplicationService.getLoginLogPage(1, 5);
        List<OperateLog> recentOperateLogs = operateLogApplicationService.getOperateLogPage(1, 5);

        List<LoginLogResponse> loginLogResponses = recentLoginLogs.stream()
                .map(this::convertLoginLog)
                .collect(Collectors.toList());

        List<OperateLogResponse> operateLogResponses = recentOperateLogs.stream()
                .map(this::convertOperateLog)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .userCount(userCount)
                .roleCount(roleCount)
                .deptCount(deptCount)
                .postCount(postCount)
                .loginLogCount(loginLogCount)
                .operateLogCount(operateLogCount)
                .recentLoginLogs(loginLogResponses)
                .recentOperateLogs(operateLogResponses)
                .build();
    }

    /**
     * 将登录日志领域对象转换为响应对象
     *
     * @param log 登录日志领域对象
     * @return 登录日志响应对象
     */
    private LoginLogResponse convertLoginLog(LoginLog log) {
        return LoginLogResponse.builder()
                .id(log.getId())
                .username(log.getUsername())
                .ipAddress(log.getIpAddress())
                .loginLocation(log.getLoginLocation())
                .browser(log.getBrowser())
                .os(log.getOs())
                .status(log.getStatus())
                .msg(log.getMessage())
                .loginTime(log.getLoginTime())
                .build();
    }

    /**
     * 将操作日志领域对象转换为响应对象
     *
     * @param log 操作日志领域对象
     * @return 操作日志响应对象
     */
    private OperateLogResponse convertOperateLog(OperateLog log) {
        return OperateLogResponse.builder()
                .id(log.getId())
                .title(log.getTitle())
                .businessType(log.getBusinessType())
                .method(log.getMethod())
                .requestMethod(log.getRequestMethod())
                .operatorName(log.getOperatorName())
                .deptName(log.getDeptName())
                .operUrl(log.getUrl())
                .operIp(log.getIp())
                .operLocation(log.getLocation())
                .operParam(log.getParam())
                .jsonResult(log.getResult())
                .status(log.getStatus())
                .errorMsg(log.getErrorMsg())
                .costTime(log.getCostTime())
                .operTime(log.getOperateTime())
                .build();
    }
}
