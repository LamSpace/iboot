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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.infrastructure.persistence.po.LoginLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录日志 Mapper 接口
 * <p>
 * 负责用户登录日志的数据库操作，支持日志查询和统计分析
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface LoginLogMapper {

    /**
     * 插入登录日志记录
     *
     * @param loginLogPO 登录日志持久化对象
     * @return 影响的记录数
     */
    int insert(LoginLogPO loginLogPO);

    /**
     * 根据 ID 查询登录日志
     *
     * @param id 登录日志 ID
     * @return 登录日志持久化对象
     */
    LoginLogPO selectById(@Param("id") Long id);

    /**
     * 分页查询登录日志（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 登录日志列表
     */
    @DataScope(deptAlias = "ll", userAlias = "ll")
    List<LoginLogPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按条件分页查询登录日志（受数据权限控制）
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 登录日志列表
     */
    @DataScope(deptAlias = "ll", userAlias = "ll")
    List<LoginLogPO> selectPageByCondition(@Param("username") String username,
                                            @Param("ipAddress") String ipAddress,
                                            @Param("status") Integer status,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    /**
     * 按条件查询所有登录日志（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志列表
     */
    @DataScope(deptAlias = "ll", userAlias = "ll")
    List<LoginLogPO> selectAllByCondition(@Param("username") String username,
                                           @Param("ipAddress") String ipAddress,
                                           @Param("status") Integer status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计登录日志总数
     *
     * @return 总数
     */
    long count();

    /**
     * 按条件统计登录日志总数（受数据权限控制）
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 总数
     */
    @DataScope(deptAlias = "ll", userAlias = "ll")
    long countByCondition(@Param("username") String username,
                          @Param("ipAddress") String ipAddress,
                          @Param("status") Integer status,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     * @return 影响的记录数
     */
    int deleteBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 按日期统计登录次数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日期和登录次数的映射列表
     */
    List<Map<String, Object>> countByDate(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的登录成功/失败次数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成功/失败次数的映射
     */
    Map<String, Object> countByStatus(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的独立登录用户数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立用户数
     */
    long countDistinctUsers(@Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时统计登录分布
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时和登录次数的映射列表
     */
    List<Map<String, Object>> countByHour(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);
}
