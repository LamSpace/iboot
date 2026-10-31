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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志应用服务
 * <p>
 * 提供登录日志的查询、统计与清理功能。日志写入已随认证服务迁至 {@code iboot-auth}，
 * 两条路径共用同一张 {@code sys_login_log} 表，因此本服务继续为管理端的日志页提供读侧能力。
 * </p>
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class LoginLogApplicationService {

    private final LoginLogRepository loginLogRepository;

    @SuppressWarnings("all")
    public LoginLogApplicationService(final LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
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
