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

import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.repository.LeaveApplicationRepository;
import com.iboot.admin.infrastructure.persistence.mapper.LeaveApplicationMapper;
import com.iboot.admin.infrastructure.persistence.po.LeaveApplicationPO;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 请假申请仓储实现。
 *
 * <p>基于 MyBatis Mapper 完成持久化，并在 PO 与领域模型之间转换。不包含业务规则，
 * 事务边界由应用服务控制。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class LeaveApplicationRepositoryImpl implements LeaveApplicationRepository {

    private final LeaveApplicationMapper leaveApplicationMapper;

    @SuppressWarnings("all")
    public LeaveApplicationRepositoryImpl(final LeaveApplicationMapper leaveApplicationMapper) {
        this.leaveApplicationMapper = leaveApplicationMapper;
    }

    @Override
    public LeaveApplication save(final LeaveApplication application) {
        LeaveApplicationPO po = toPO(application);
        leaveApplicationMapper.insert(po);
        application.setId(po.getId());
        return application;
    }

    @Override
    public boolean update(final LeaveApplication application) {
        return leaveApplicationMapper.update(toPO(application)) > 0;
    }

    @Override
    public Optional<LeaveApplication> findById(final Long id) {
        return Optional.ofNullable(leaveApplicationMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<LeaveApplication> findByProcessInstanceId(final String processInstanceId) {
        return Optional.ofNullable(leaveApplicationMapper.selectByProcessInstanceId(processInstanceId))
                .map(this::toDomain);
    }

    @Override
    public List<LeaveApplication> findByProcessInstanceIds(final Collection<String> processInstanceIds) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return List.of();
        }
        return leaveApplicationMapper.selectByProcessInstanceIds(processInstanceIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaveApplication> findByApplicant(final Long applicantId, final LeaveStatus status,
                                                  final int offset, final int limit) {
        String statusValue = status == null ? null : status.name();
        return leaveApplicationMapper.selectByApplicant(applicantId, statusValue, offset, limit).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByApplicant(final Long applicantId, final LeaveStatus status) {
        return leaveApplicationMapper.countByApplicant(applicantId, status == null ? null : status.name());
    }

    private LeaveApplicationPO toPO(final LeaveApplication application) {
        LeaveApplicationPO po = new LeaveApplicationPO();
        po.setId(application.getId());
        po.setApplicantId(application.getApplicantId());
        po.setApproverId(application.getApproverId());
        po.setLeaveType(application.getLeaveType());
        po.setStartTime(application.getStartTime());
        po.setEndTime(application.getEndTime());
        po.setReason(application.getReason());
        po.setStatus(application.getStatus() == null ? null : application.getStatus().name());
        po.setProcessInstanceId(application.getProcessInstanceId());
        po.setApproveComment(application.getApproveComment());
        po.setCreateBy(application.getCreateBy());
        po.setCreateTime(application.getCreateTime());
        po.setUpdateBy(application.getUpdateBy());
        po.setUpdateTime(application.getUpdateTime());
        po.setDeleted(application.getDeleted());
        return po;
    }

    private LeaveApplication toDomain(final LeaveApplicationPO po) {
        LeaveApplication application = new LeaveApplication();
        application.setId(po.getId());
        application.setApplicantId(po.getApplicantId());
        application.setApproverId(po.getApproverId());
        application.setLeaveType(po.getLeaveType());
        application.setStartTime(po.getStartTime());
        application.setEndTime(po.getEndTime());
        application.setReason(po.getReason());
        application.setStatus(po.getStatus() == null ? null : LeaveStatus.valueOf(po.getStatus()));
        application.setProcessInstanceId(po.getProcessInstanceId());
        application.setApproveComment(po.getApproveComment());
        application.setCreateBy(po.getCreateBy());
        application.setCreateTime(po.getCreateTime());
        application.setUpdateBy(po.getUpdateBy());
        application.setUpdateTime(po.getUpdateTime());
        application.setDeleted(po.getDeleted());
        return application;
    }
}
