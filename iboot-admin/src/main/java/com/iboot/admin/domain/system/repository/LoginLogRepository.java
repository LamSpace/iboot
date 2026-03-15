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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.LoginLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 登录日志仓储接口
 * <p>
 * 负责用户登录日志的持久化操作，支持登录记录的查询、统计和清理
 * </p>
 *
 * @author iBoot
 */
public interface LoginLogRepository {

    /**
     * 保存登录日志
     *
     * @param loginLog 登录日志实体
     * @return 保存后的登录日志
     */
    LoginLog save(LoginLog loginLog);

    /**
     * 根据 ID 查询登录日志
     *
     * @param id 登录日志 ID
     * @return 登录日志实体，不存在则返回空
     */
    Optional<LoginLog> findById(Long id);

    /**
     * 分页查询登录日志
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 登录日志列表
     */
    List<LoginLog> findPage(int pageNum, int pageSize);

    /**
     * 根据条件分页查询登录日志
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 登录日志列表
     */
    List<LoginLog> findPageByCondition(String username, String ipAddress, Integer status,
                                        LocalDateTime startTime, LocalDateTime endTime,
                                        int pageNum, int pageSize);

    /**
     * 根据条件查询所有登录日志（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志列表
     */
    List<LoginLog> findAllByCondition(String username, String ipAddress, Integer status,
                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计登录日志总数
     *
     * @return 登录日志总数
     */
    long count();

    /**
     * 根据条件统计登录日志总数
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志总数
     */
    long countByCondition(String username, String ipAddress, Integer status,
                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     * <p>
     * 用于定期清理历史日志数据，释放存储空间
     * </p>
     *
     * @param beforeDate 清理该日期之前的日志
     * @return 清理的日志数量
     */
    int cleanLogBeforeDate(LocalDateTime beforeDate);
}
