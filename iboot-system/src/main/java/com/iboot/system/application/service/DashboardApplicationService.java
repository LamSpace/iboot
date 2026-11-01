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

package com.iboot.system.application.service;

import com.iboot.system.infrastructure.persistence.mapper.LoginLogMapper;
import com.iboot.system.infrastructure.persistence.mapper.OperateLogMapper;
import com.iboot.system.infrastructure.persistence.po.LoginLogPO;
import com.iboot.system.infrastructure.persistence.po.OperateLogPO;
import com.iboot.system.interfaces.dto.response.DashboardSummaryResponse;
import com.iboot.system.interfaces.dto.response.LoginLogResponse;
import com.iboot.system.interfaces.dto.response.OperateLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 仪表盘应用服务
 * <p>
 * 提供 Dashboard 首页数据汇总功能，包括各模块统计数量和最新日志记录。
 * </p>
 *
 * <p>日志数据由监控服务拥有，这里跨 schema 只读 {@code iboot_monitor} 的两张表：
 * 仪表盘只取总数与最近 5 条，为它引入一次跨服务调用不划算。
 * 退出条件：{@code iboot-system} 拆分时改为调用监控服务的查询接口。</p>
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class DashboardApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DashboardApplicationService.class);

    /**
     * 仪表盘展示的最近日志条数。
     */
    private static final int RECENT_LOG_LIMIT = 5;

    private final UserApplicationService userApplicationService;

    private final RoleApplicationService roleApplicationService;

    private final DeptApplicationService deptApplicationService;

    private final PostApplicationService postApplicationService;

    private final LoginLogMapper loginLogMapper;

    private final OperateLogMapper operateLogMapper;

    @SuppressWarnings("all")
    public DashboardApplicationService(final UserApplicationService userApplicationService,
                                       final RoleApplicationService roleApplicationService, final DeptApplicationService deptApplicationService,
                                       final PostApplicationService postApplicationService,
                                       final LoginLogMapper loginLogMapper,
                                       final OperateLogMapper operateLogMapper) {
        this.userApplicationService = userApplicationService;
        this.roleApplicationService = roleApplicationService;
        this.deptApplicationService = deptApplicationService;
        this.postApplicationService = postApplicationService;
        this.loginLogMapper = loginLogMapper;
        this.operateLogMapper = operateLogMapper;
    }

    /**
     * 获取仪表盘汇总数据
     * <p>
     * 统计用户、角色、部门、岗位、登录日志、操作日志的数量， 并返回最新的 5 条登录日志和操作日志记录
     * </p>
     *
     * @return 仪表盘汇总数据响应对象
     */
    public DashboardSummaryResponse getDashboardSummary() {
        long userCount = userApplicationService.countUsers();
        long roleCount = roleApplicationService.countRoles();
        long deptCount = deptApplicationService.countDepts();
        long postCount = postApplicationService.countPosts();
        long loginLogCount = loginLogMapper.count();
        long operateLogCount = operateLogMapper.count();
        List<LoginLogPO> recentLoginLogs = loginLogMapper.selectPage(0, RECENT_LOG_LIMIT);
        List<OperateLogPO> recentOperateLogs = operateLogMapper.selectPage(0, RECENT_LOG_LIMIT);
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
     *
     * @return 登录日志响应对象
     */
    private LoginLogResponse convertLoginLog(LoginLogPO log) {
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
     *
     * @return 操作日志响应对象
     */
    private OperateLogResponse convertOperateLog(OperateLogPO log) {
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
