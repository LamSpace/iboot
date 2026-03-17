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
import com.iboot.admin.domain.system.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志应用服务
 * <p>
 * 提供登录日志的记录、查询、统计、清理等功能 日志记录支持异步处理，不影响主业务流程
 * </p>
 *
 * @author iBoot
 */
@Service
public class LoginLogApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogApplicationService.class);

    private final LoginLogRepository loginLogRepository;

    @SuppressWarnings("all")
    public LoginLogApplicationService(final LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * 记录登录日志（异步执行）
     * <p>
     * 创建登录日志记录并保存到数据库，包括用户名、IP 地址、登录地点、 浏览器信息、操作系统、登录状态等信息
     * </p>
     *
     * @param username      用户名
     * @param ipAddress     登录 IP 地址
     * @param loginLocation 登录地点
     * @param browser       浏览器信息
     * @param os            操作系统信息
     * @param status        登录状态（1 成功，0 失败）
     * @param message       登录消息
     */
    @Async
    public void recordLoginLog(String username, String ipAddress, String loginLocation, String browser, String os,
                               Integer status, String message) {
        LoginLog loginLog = LoginLog.builder()
                .username(username)
                .ipAddress(ipAddress)
                .loginLocation(loginLocation)
                .browser(browser)
                .os(os)
                .status(status)
                .message(message)
                .loginTime(LocalDateTime.now())
                .build();
        loginLogRepository.save(loginLog);
        log.debug("记录登录日志：{} - {}", username, status == 1 ? "成功" : "失败");
    }

    /**
     * 分页查询登录日志
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 登录日志列表
     */
    public List<LoginLog> getLoginLogPage(int pageNum, int pageSize) {
        return loginLogRepository.findPage(pageNum, pageSize);
    }

    /**
     * 根据条件分页查询登录日志
     *
     * @param username  用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status    登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页数量
     *
     * @return 登录日志列表
     */
    public List<LoginLog> getLoginLogPageByCondition(String username, String ipAddress, Integer status,
                                                     LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize) {
        return loginLogRepository.findPageByCondition(username, ipAddress, status, startTime, endTime, pageNum,
                pageSize);
    }

    /**
     * 统计登录日志总数
     *
     * @return 登录日志总数
     */
    public long countLoginLogs() {
        return loginLogRepository.count();
    }

    /**
     * 根据条件统计登录日志总数
     *
     * @param username  用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status    登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     *
     * @return 登录日志总数
     */
    public long countLoginLogsByCondition(String username, String ipAddress, Integer status, LocalDateTime startTime,
                                          LocalDateTime endTime) {
        return loginLogRepository.countByCondition(username, ipAddress, status, startTime, endTime);
    }

    /**
     * 根据条件获取所有登录日志（不分页，用于导出）
     *
     * @param username  用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status    登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     *
     * @return 登录日志列表
     */
    public List<LoginLog> getAllLoginLogsByCondition(String username, String ipAddress, Integer status,
                                                     LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogRepository.findAllByCondition(username, ipAddress, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的登录日志
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 清理的日志数量
     */
    public int cleanLoginLogs(LocalDateTime beforeDate) {
        return loginLogRepository.cleanLogBeforeDate(beforeDate);
    }

}
