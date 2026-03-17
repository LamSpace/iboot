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

import com.iboot.admin.domain.system.model.OperateLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 操作日志仓储接口
 * <p>
 * 负责系统操作日志的持久化操作，记录用户在系统中的所有操作行为
 * </p>
 *
 * @author iBoot
 */
public interface OperateLogRepository {

    /**
     * 保存操作日志
     *
     * @param operateLog 操作日志实体
     *
     * @return 保存后的操作日志
     */
    OperateLog save(OperateLog operateLog);

    /**
     * 根据 ID 查询操作日志
     *
     * @param id 操作日志 ID
     *
     * @return 操作日志实体，不存在则返回空
     */
    Optional<OperateLog> findById(Long id);

    /**
     * 分页查询操作日志
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 操作日志列表
     */
    List<OperateLog> findPage(int pageNum, int pageSize);

    /**
     * 根据条件分页查询操作日志
     *
     * @param title        操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status       操作状态（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     * @param pageNum      页码
     * @param pageSize     每页数量
     *
     * @return 操作日志列表
     */
    List<OperateLog> findPageByCondition(String title, String operatorName, Integer businessType, Integer status,
                                         LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize);

    /**
     * 根据条件查询所有操作日志（不分页，用于导出）
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
    List<OperateLog> findAllByCondition(String title, String operatorName, Integer businessType, Integer status,
                                        LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计操作日志总数
     *
     * @return 操作日志总数
     */
    long count();

    /**
     * 根据条件统计操作日志总数
     *
     * @param title        操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status       操作状态（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     *
     * @return 操作日志总数
     */
    long countByCondition(String title, String operatorName, Integer businessType, Integer status,
                          LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 清理指定日期之前的日志
     * <p>
     * 用于定期清理历史日志数据，释放存储空间
     * </p>
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 清理的日志数量
     */
    int cleanLogBeforeDate(LocalDateTime beforeDate);

}
