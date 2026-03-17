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
import com.iboot.admin.infrastructure.persistence.po.OperateLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Mapper 接口
 * <p>
 * 负责系统操作日志的数据库操作，支持日志查询和统计分析
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface OperateLogMapper {

    /**
     * 插入操作日志记录
     *
     * @param operateLogPO 操作日志持久化对象
     *
     * @return 影响的记录数
     */
    int insert(OperateLogPO operateLogPO);

    /**
     * 根据 ID 查询操作日志
     *
     * @param id 操作日志 ID
     *
     * @return 操作日志持久化对象
     */
    OperateLogPO selectById(@Param("id") Long id);

    /**
     * 分页查询操作日志（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit  限制数量
     *
     * @return 操作日志列表
     */
    @DataScope(deptAlias = "ol", userAlias = "ol")
    List<OperateLogPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按条件分页查询操作日志（受数据权限控制）
     *
     * @param title        操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status       操作状态（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     * @param offset       偏移量
     * @param limit        限制数量
     *
     * @return 操作日志列表
     */
    @DataScope(deptAlias = "ol", userAlias = "ol")
    List<OperateLogPO> selectPageByCondition(@Param("title") String title, @Param("operatorName") String operatorName,
                                             @Param("businessType") Integer businessType,
                                             @Param("status") Integer status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按条件查询所有操作日志（不分页，用于导出）
     *
     * @param title        操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status       操作状态（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     *
     * @return 操作日志列表
     */
    @DataScope(deptAlias = "ol", userAlias = "ol")
    List<OperateLogPO> selectAllByCondition(@Param("title") String title, @Param("operatorName") String operatorName,
                                            @Param("businessType") Integer businessType,
                                            @Param("status") Integer status,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计操作日志总数
     *
     * @return 总数
     */
    long count();

    /**
     * 按条件统计操作日志总数（受数据权限控制）
     *
     * @param title        操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status       操作状态（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     *
     * @return 总数
     */
    @DataScope(deptAlias = "ol", userAlias = "ol")
    long countByCondition(@Param("title") String title, @Param("operatorName") String operatorName,
                          @Param("businessType") Integer businessType, @Param("status") Integer status,
                          @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 影响的记录数
     */
    int deleteBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 按日期统计操作次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 日期和操作次数的映射列表
     */
    List<Map<String, Object>> countByDate(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按模块统计操作次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 模块和操作次数的映射列表
     */
    List<Map<String, Object>> countByModule(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的操作成功/失败次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 成功/失败次数的映射
     */
    Map<String, Object> countByStatus(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 按小时统计操作分布
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 小时和操作次数的映射列表
     */
    List<Map<String, Object>> countByHour(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计活跃用户排行
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     返回数量限制
     *
     * @return 用户和操作次数的映射列表
     */
    List<Map<String, Object>> topActiveUsers(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime, @Param("limit") int limit);

    /**
     * 按请求方法统计操作次数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 请求方法和操作次数的映射列表
     */
    List<Map<String, Object>> countByRequestMethod(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

}
