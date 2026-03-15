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

import com.iboot.admin.domain.system.model.OperateLog;
import com.iboot.admin.domain.system.repository.OperateLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 操作日志应用服务
 * <p>
 * 提供操作日志的记录、查询、统计、清理等功能
 * 日志记录支持异步处理，不影响主业务流程
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperateLogApplicationService {

    private final OperateLogRepository operateLogRepository;

    /**
     * 记录操作日志（异步执行）
     * <p>
     * 将操作日志保存到数据库，包括操作标题、业务类型、操作人员、
     * 请求方法、操作 URL、IP 地址、请求参数、返回结果、状态等信息
     * </p>
     *
     * @param operateLog 操作日志实体
     */
    @Async
    public void recordOperateLog(OperateLog operateLog) {
        operateLogRepository.save(operateLog);
        log.debug("记录操作日志：{} - {}", operateLog.getTitle(), operateLog.getOperatorName());
    }

    /**
     * 根据 ID 获取操作日志
     *
     * @param id 操作日志 ID
     * @return 操作日志实体，不存在则返回空 Optional
     */
    public Optional<OperateLog> getOperateLogById(Long id) {
        return operateLogRepository.findById(id);
    }

    /**
     * 分页查询操作日志
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 操作日志列表
     */
    public List<OperateLog> getOperateLogPage(int pageNum, int pageSize) {
        return operateLogRepository.findPage(pageNum, pageSize);
    }

    /**
     * 根据条件分页查询操作日志
     *
     * @param title 操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status 操作状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 操作日志列表
     */
    public List<OperateLog> getOperateLogPageByCondition(String title, String operatorName, Integer businessType,
                                                          Integer status, LocalDateTime startTime, LocalDateTime endTime,
                                                          int pageNum, int pageSize) {
        return operateLogRepository.findPageByCondition(title, operatorName, businessType, status, startTime, endTime, pageNum, pageSize);
    }

    /**
     * 统计操作日志总数
     *
     * @return 操作日志总数
     */
    public long countOperateLogs() {
        return operateLogRepository.count();
    }

    /**
     * 根据条件统计操作日志总数
     *
     * @param title 操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status 操作状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 操作日志总数
     */
    public long countOperateLogsByCondition(String title, String operatorName, Integer businessType,
                                             Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        return operateLogRepository.countByCondition(title, operatorName, businessType, status, startTime, endTime);
    }

    /**
     * 根据条件获取所有操作日志（不分页，用于导出）
     *
     * @param title 操作标题（可选）
     * @param operatorName 操作人员姓名（可选）
     * @param businessType 业务类型（可选）
     * @param status 操作状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 操作日志列表
     */
    public List<OperateLog> getAllOperateLogsByCondition(String title, String operatorName, Integer businessType,
                                                          Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        return operateLogRepository.findAllByCondition(title, operatorName, businessType, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的操作日志
     *
     * @param beforeDate 清理该日期之前的日志
     * @return 清理的日志数量
     */
    public int cleanOperateLogs(LocalDateTime beforeDate) {
        return operateLogRepository.cleanLogBeforeDate(beforeDate);
    }
}
