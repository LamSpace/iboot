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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.system.model.OperateLog;
import com.iboot.admin.domain.system.repository.OperateLogRepository;
import com.iboot.admin.infrastructure.persistence.mapper.OperateLogMapper;
import com.iboot.admin.infrastructure.persistence.po.OperateLogPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 操作日志仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现系统操作日志的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class OperateLogRepositoryImpl implements OperateLogRepository {

    private final OperateLogMapper operateLogMapper;

    @SuppressWarnings("all")
    public OperateLogRepositoryImpl(final OperateLogMapper operateLogMapper) {
        this.operateLogMapper = operateLogMapper;
    }

    /**
     * 保存操作日志
     *
     * @param operateLog 操作日志实体
     *
     * @return 保存后的操作日志
     */
    @Override
    public OperateLog save(OperateLog operateLog) {
        OperateLogPO po = convertToPO(operateLog);
        operateLogMapper.insert(po);
        return convertToDomain(po);
    }

    /**
     * 根据 ID 查询操作日志
     *
     * @param id 操作日志 ID
     *
     * @return 操作日志实体，不存在则返回空
     */
    @Override
    public Optional<OperateLog> findById(Long id) {
        OperateLogPO po = operateLogMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 分页查询操作日志
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 操作日志列表
     */
    @Override
    public List<OperateLog> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return operateLogMapper.selectPage(offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

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
    @Override
    public List<OperateLog> findPageByCondition(String title, String operatorName, Integer businessType, Integer status,
                                                LocalDateTime startTime, LocalDateTime endTime, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return operateLogMapper
                .selectPageByCondition(title, operatorName, businessType, status, startTime, endTime, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

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
    @Override
    public List<OperateLog> findAllByCondition(String title, String operatorName, Integer businessType, Integer status,
                                               LocalDateTime startTime, LocalDateTime endTime) {
        return operateLogMapper.selectAllByCondition(title, operatorName, businessType, status, startTime, endTime)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计操作日志总数
     *
     * @return 操作日志总数
     */
    @Override
    public long count() {
        return operateLogMapper.count();
    }

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
    @Override
    public long countByCondition(String title, String operatorName, Integer businessType, Integer status,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        return operateLogMapper.countByCondition(title, operatorName, businessType, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     *
     * @return 清理的日志数量
     */
    @Override
    public int cleanLogBeforeDate(LocalDateTime beforeDate) {
        return operateLogMapper.deleteBeforeDate(beforeDate);
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param operateLog 操作日志领域对象
     *
     * @return 操作日志持久化对象
     */
    private OperateLogPO convertToPO(OperateLog operateLog) {
        OperateLogPO po = new OperateLogPO();
        po.setId(operateLog.getId());
        po.setTitle(operateLog.getTitle());
        po.setBusinessType(operateLog.getBusinessType());
        po.setMethod(operateLog.getMethod());
        po.setRequestMethod(operateLog.getRequestMethod());
        po.setOperatorType(operateLog.getOperatorType());
        po.setOperatorName(operateLog.getOperatorName());
        po.setDeptName(operateLog.getDeptName());
        po.setUrl(operateLog.getUrl());
        po.setIp(operateLog.getIp());
        po.setLocation(operateLog.getLocation());
        po.setParam(operateLog.getParam());
        po.setResult(operateLog.getResult());
        po.setStatus(operateLog.getStatus());
        po.setErrorMsg(operateLog.getErrorMsg());
        po.setOperateTime(operateLog.getOperateTime());
        po.setCostTime(operateLog.getCostTime());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 操作日志持久化对象
     *
     * @return 操作日志领域对象
     */
    private OperateLog convertToDomain(OperateLogPO po) {
        return OperateLog.builder()
                .id(po.getId())
                .title(po.getTitle())
                .businessType(po.getBusinessType())
                .method(po.getMethod())
                .requestMethod(po.getRequestMethod())
                .operatorType(po.getOperatorType())
                .operatorName(po.getOperatorName())
                .deptName(po.getDeptName())
                .url(po.getUrl())
                .ip(po.getIp())
                .location(po.getLocation())
                .param(po.getParam())
                .result(po.getResult())
                .status(po.getStatus())
                .errorMsg(po.getErrorMsg())
                .operateTime(po.getOperateTime())
                .costTime(po.getCostTime())
                .build();
    }

}
