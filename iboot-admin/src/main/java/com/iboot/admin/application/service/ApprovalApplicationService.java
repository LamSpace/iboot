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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;
import com.iboot.admin.domain.workflow.repository.LeaveApplicationRepository;
import com.iboot.admin.domain.workflow.service.WorkflowService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 审批应用服务。
 *
 * <p>编排请假业务记录与工作流推进：发起时校验审批人并启动流程，审批时校验任务归属、
 * 禁止自审与重复处理，并在同一事务内更新业务状态。历史展示由业务状态与流程历史组合而成。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see WorkflowService
 * @see LeaveApplicationRepository
 */
@Service
public class ApprovalApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;

    private final WorkflowService workflowService;

    private final UserRepository userRepository;

    @SuppressWarnings("all")
    public ApprovalApplicationService(final LeaveApplicationRepository leaveApplicationRepository,
                                      final WorkflowService workflowService,
                                      final UserRepository userRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.workflowService = workflowService;
        this.userRepository = userRepository;
    }

    /**
     * 发起请假申请。
     *
     * <p>校验审批人存在、启用且不是申请人后，先启动流程再保存业务记录；
     * 任一步骤失败时整个事务回滚。
     *
     * @param applicantId   申请人用户 ID
     * @param applicantName 申请人用户名
     * @param approverId    审批人用户 ID
     * @param leaveType     请假类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param reason        请假事由
     * @return 创建后的申请
     * @throws BusinessException 审批人不合法或时间不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveApplication submit(final Long applicantId, final String applicantName, final Long approverId,
                                   final String leaveType, final LocalDateTime startTime,
                                   final LocalDateTime endTime, final String reason) {
        validateApprover(applicantId, approverId);
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new BusinessException(400, "请假时间不合法");
        }

        String processInstanceId = workflowService.startProcess(applicantId, approverId);

        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setApproverId(approverId);
        application.setLeaveType(leaveType);
        application.setStartTime(startTime);
        application.setEndTime(endTime);
        application.setReason(reason);
        application.setStatus(LeaveStatus.PENDING);
        application.setProcessInstanceId(processInstanceId);
        application.setCreateBy(applicantName);
        application.setCreateTime(LocalDateTime.now());
        application.setDeleted(0);
        return leaveApplicationRepository.save(application);
    }

    /**
     * 分页查询当前用户的待办。
     *
     * @param userId   当前用户 ID
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 待办分页结果
     */
    public PageResult<ApprovalItem> findTodo(final Long userId, final int pageNum, final int pageSize) {
        int offset = offset(pageNum, pageSize);
        List<WorkflowTask> tasks = workflowService.findActiveTasks(userId, offset, pageSize);
        long total = workflowService.countActiveTasks(userId);
        Map<String, LeaveApplication> applications = loadApplications(
                tasks.stream().map(WorkflowTask::getProcessInstanceId).toList());
        List<ApprovalItem> items = tasks.stream().map(task -> {
            ApprovalItem item = new ApprovalItem();
            item.setApplication(applications.get(task.getProcessInstanceId()));
            item.setTask(task);
            fillNames(item);
            return item;
        }).toList();
        return new PageResult<>(items, total, pageNum, pageSize);
    }

    /**
     * 分页查询当前用户的已办。
     *
     * @param userId   当前用户 ID
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 已办分页结果
     */
    public PageResult<ApprovalItem> findDone(final Long userId, final int pageNum, final int pageSize) {
        int offset = offset(pageNum, pageSize);
        List<WorkflowHistoryItem> history = workflowService.findCompletedTasks(userId, offset, pageSize);
        long total = workflowService.countCompletedTasks(userId);
        Map<String, LeaveApplication> applications = loadApplications(
                history.stream().map(WorkflowHistoryItem::getProcessInstanceId).toList());
        List<ApprovalItem> items = history.stream().map(item -> {
            LeaveApplication application = applications.get(item.getProcessInstanceId());
            decorateHistoryItem(item, application);
            ApprovalItem approvalItem = new ApprovalItem();
            approvalItem.setApplication(application);
            approvalItem.setHistory(item);
            fillNames(approvalItem);
            return approvalItem;
        }).toList();
        return new PageResult<>(items, total, pageNum, pageSize);
    }

    /**
     * 分页查询当前用户提交的申请。
     *
     * @param userId   当前用户 ID
     * @param status   状态筛选，可为 {@code null}
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     * @return 申请分页结果
     */
    public PageResult<ApprovalItem> findMine(final Long userId, final LeaveStatus status,
                                             final int pageNum, final int pageSize) {
        int offset = offset(pageNum, pageSize);
        List<LeaveApplication> records = leaveApplicationRepository.findByApplicant(userId, status, offset, pageSize);
        long total = leaveApplicationRepository.countByApplicant(userId, status);
        List<ApprovalItem> items = records.stream().map(application -> {
            ApprovalItem item = new ApprovalItem();
            item.setApplication(application);
            fillNames(item);
            return item;
        }).toList();
        return new PageResult<>(items, total, pageNum, pageSize);
    }

    /**
     * 查询申请详情与审批历史。
     *
     * @param id         申请 ID
     * @param viewerId   查看人用户 ID
     * @param canViewAll 是否拥有查看全部申请的权限
     * @return 详情聚合
     * @throws BusinessException 申请不存在或查看人无权查看时抛出
     */
    public ApprovalDetail detail(final Long id, final Long viewerId, final boolean canViewAll) {
        LeaveApplication application = loadApplication(id);
        boolean participant = viewerId.equals(application.getApplicantId())
                || viewerId.equals(application.getApproverId());
        if (!canViewAll && !participant) {
            throw new BusinessException(404, "申请不存在");
        }
        ApprovalDetail detail = new ApprovalDetail();
        detail.setApplication(application);
        detail.setHistory(buildHistory(application));
        detail.setApplicantName(displayName(application.getApplicantId()));
        detail.setApproverName(displayName(application.getApproverId()));
        return detail;
    }

    /**
     * 审批通过。
     *
     * @param id           申请 ID
     * @param operatorId   操作人用户 ID
     * @param operatorName 操作人用户名
     * @param comment      审批意见，可为 {@code null}
     * @throws BusinessException      申请不存在、已处理或任务不存在时抛出
     * @throws AccessDeniedException 非当前审批人或自审时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(final Long id, final Long operatorId, final String operatorName, final String comment) {
        LeaveApplication application = loadApplication(id);
        WorkflowTask task = requireActiveTask(application, operatorId);
        workflowService.completeTask(task.getTaskId(), true, comment);
        application.markApproved(comment);
        application.setUpdateBy(operatorName);
        leaveApplicationRepository.update(application);
    }

    /**
     * 审批驳回。
     *
     * @param id           申请 ID
     * @param operatorId   操作人用户 ID
     * @param operatorName 操作人用户名
     * @param comment      驳回原因，不能为空
     * @throws BusinessException      驳回原因为空、申请不存在、已处理或任务不存在时抛出
     * @throws AccessDeniedException 非当前审批人或自审时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(final Long id, final Long operatorId, final String operatorName, final String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(400, "驳回原因不能为空");
        }
        LeaveApplication application = loadApplication(id);
        WorkflowTask task = requireActiveTask(application, operatorId);
        workflowService.completeTask(task.getTaskId(), false, comment);
        application.markRejected(comment);
        application.setUpdateBy(operatorName);
        leaveApplicationRepository.update(application);
    }

    private void validateApprover(final Long applicantId, final Long approverId) {
        if (approverId == null) {
            throw new BusinessException(400, "审批人不能为空");
        }
        if (approverId.equals(applicantId)) {
            throw new BusinessException(400, "审批人不能是本人");
        }
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(400, "审批人不存在"));
        if (!Integer.valueOf(1).equals(approver.getStatus())) {
            throw new BusinessException(400, "审批人已停用");
        }
    }

    private LeaveApplication loadApplication(final Long id) {
        return leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "申请不存在"));
    }

    private WorkflowTask requireActiveTask(final LeaveApplication application, final Long operatorId) {
        if (application.getStatus().isTerminal()) {
            throw new BusinessException(409, "申请已处理");
        }
        if (application.getApplicantId().equals(operatorId)) {
            throw new AccessDeniedException("不能审批自己的申请");
        }
        WorkflowTask task = workflowService.findActiveTask(application.getProcessInstanceId());
        if (task == null) {
            throw new BusinessException(409, "任务已处理");
        }
        if (!operatorId.equals(task.getAssigneeId())) {
            throw new AccessDeniedException("不是当前任务的审批人");
        }
        return task;
    }

    private Map<String, LeaveApplication> loadApplications(final List<String> processInstanceIds) {
        return leaveApplicationRepository.findByProcessInstanceIds(processInstanceIds).stream()
                .collect(Collectors.toMap(LeaveApplication::getProcessInstanceId, Function.identity()));
    }

    private List<WorkflowHistoryItem> buildHistory(final LeaveApplication application) {
        List<WorkflowHistoryItem> result = new ArrayList<>();
        WorkflowHistoryItem submit = new WorkflowHistoryItem();
        submit.setProcessInstanceId(application.getProcessInstanceId());
        submit.setActivityName("提交申请");
        submit.setActorId(application.getApplicantId());
        submit.setTime(application.getCreateTime());
        result.add(submit);

        for (WorkflowHistoryItem item : workflowService.findHistory(application.getProcessInstanceId())) {
            decorateHistoryItem(item, application);
            result.add(item);
        }
        return result;
    }

    private void decorateHistoryItem(final WorkflowHistoryItem item, final LeaveApplication application) {
        if (application == null) {
            return;
        }
        item.setComment(application.getApproveComment());
        switch (application.getStatus()) {
            case APPROVED -> {
                item.setActivityName("审批通过");
                item.setApproved(true);
            }
            case REJECTED -> {
                item.setActivityName("审批驳回");
                item.setApproved(false);
            }
            default -> {
                item.setActivityName("审批中");
                item.setApproved(null);
            }
        }
    }

    private void fillNames(final ApprovalItem item) {
        if (item.getApplication() == null) {
            return;
        }
        item.setApplicantName(displayName(item.getApplication().getApplicantId()));
        item.setApproverName(displayName(item.getApplication().getApproverId()));
    }

    private String displayName(final Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(user -> user.getNickname() == null || user.getNickname().isBlank()
                        ? user.getUsername() : user.getNickname())
                .orElse(null);
    }

    private int offset(final int pageNum, final int pageSize) {
        return Math.max(pageNum - 1, 0) * pageSize;
    }
}
