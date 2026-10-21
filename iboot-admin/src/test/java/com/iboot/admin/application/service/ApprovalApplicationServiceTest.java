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
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.model.WorkflowTask;
import com.iboot.admin.domain.workflow.repository.LeaveApplicationRepository;
import com.iboot.admin.domain.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ApprovalApplicationService} 的单元测试。
 *
 * <p>使用 Mock 隔离仓储与工作流，覆盖发起校验、审批人归属、自审、重复处理与可见性。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ApprovalApplicationServiceTest {

    private LeaveApplicationRepository repository;

    private WorkflowService workflowService;

    private UserRepository userRepository;

    private ApprovalApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveApplicationRepository.class);
        workflowService = mock(WorkflowService.class);
        userRepository = mock(UserRepository.class);
        service = new ApprovalApplicationService(repository, workflowService, userRepository);
    }

    /**
     * 验证发起成功时启动流程并保存待审批记录。
     */
    @Test
    void submitStartsProcessAndSavesPendingApplication() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 1)));
        when(workflowService.startProcess(1L, 2L)).thenReturn("pid-1");
        when(repository.save(any(LeaveApplication.class))).thenAnswer(invocation -> {
            LeaveApplication application = invocation.getArgument(0);
            application.setId(10L);
            return application;
        });

        LeaveApplication application = service.submit(1L, "alice", 2L, "ANNUAL",
                LocalDateTime.of(2026, 11, 2, 9, 0), LocalDateTime.of(2026, 11, 3, 18, 0), "回家");

        assertThat(application.getId()).isEqualTo(10L);
        assertThat(application.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(application.getProcessInstanceId()).isEqualTo("pid-1");
        verify(workflowService).startProcess(1L, 2L);
    }

    /**
     * 验证审批人不能是本人。
     */
    @Test
    void submitRejectsSelfApproval() {
        assertThatThrownBy(() -> service.submit(1L, "alice", 1L, "ANNUAL",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "回家"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本人");
        verify(workflowService, never()).startProcess(anyLong(), anyLong());
    }

    /**
     * 验证审批人不存在时拒绝。
     */
    @Test
    void submitRejectsMissingApprover() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(1L, "alice", 2L, "ANNUAL",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "回家"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批人不存在");
    }

    /**
     * 验证审批人停用时拒绝。
     */
    @Test
    void submitRejectsDisabledApprover() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, 0)));

        assertThatThrownBy(() -> service.submit(1L, "alice", 2L, "ANNUAL",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "回家"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用");
    }

    /**
     * 验证审批通过会推进流程并更新业务状态。
     */
    @Test
    void approveCompletesTaskAndUpdatesStatus() {
        LeaveApplication application = application(1L, 2L, LeaveStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(application));
        when(workflowService.findActiveTask("pid-10")).thenReturn(task("task-1", 2L));

        service.approve(10L, 2L, "bob", "同意");

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(application.getApproveComment()).isEqualTo("同意");
        verify(workflowService).completeTask("task-1", true, "同意");
        verify(repository).update(application);
    }

    /**
     * 验证非审批人不能通过。
     */
    @Test
    void approveRejectsNonAssignee() {
        LeaveApplication application = application(1L, 2L, LeaveStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(application));
        when(workflowService.findActiveTask("pid-10")).thenReturn(task("task-1", 3L));

        assertThatThrownBy(() -> service.approve(10L, 99L, "eve", "同意"))
                .isInstanceOf(AccessDeniedException.class);
        verify(workflowService, never()).completeTask(anyString(), anyBoolean(), any());
    }

    /**
     * 验证申请人不能审批自己的申请。
     */
    @Test
    void approveRejectsSelfApproval() {
        LeaveApplication application = application(1L, 1L, LeaveStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approve(10L, 1L, "alice", "同意"))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * 验证已处理的申请不能重复审批。
     */
    @Test
    void approveRejectsTerminalApplication() {
        LeaveApplication application = application(1L, 2L, LeaveStatus.APPROVED);
        when(repository.findById(10L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.approve(10L, 2L, "bob", "同意"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已处理");
    }

    /**
     * 验证驳回原因必填。
     */
    @Test
    void rejectRequiresComment() {
        assertThatThrownBy(() -> service.reject(10L, 2L, "bob", " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("驳回原因");
    }

    /**
     * 验证驳回会更新业务状态与意见。
     */
    @Test
    void rejectCompletesTaskAndUpdatesStatus() {
        LeaveApplication application = application(1L, 2L, LeaveStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(application));
        when(workflowService.findActiveTask("pid-10")).thenReturn(task("task-1", 2L));

        service.reject(10L, 2L, "bob", "材料不全");

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(application.getApproveComment()).isEqualTo("材料不全");
        verify(workflowService).completeTask("task-1", false, "材料不全");
    }

    /**
     * 验证参与者可以查看详情，无关用户被拒绝。
     */
    @Test
    void detailEnforcesParticipantVisibility() {
        LeaveApplication application = application(1L, 2L, LeaveStatus.PENDING);
        when(repository.findById(10L)).thenReturn(Optional.of(application));
        when(workflowService.findHistory("pid-10")).thenReturn(List.of());

        assertThat(service.detail(10L, 1L, false).getApplication().getId()).isEqualTo(10L);
        assertThat(service.detail(10L, 2L, false).getApplication().getId()).isEqualTo(10L);
        assertThat(service.detail(10L, 99L, true).getApplication().getId()).isEqualTo(10L);
        assertThatThrownBy(() -> service.detail(10L, 99L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    private User user(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private LeaveApplication application(Long applicantId, Long approverId, LeaveStatus status) {
        LeaveApplication application = new LeaveApplication();
        application.setId(10L);
        application.setApplicantId(applicantId);
        application.setApproverId(approverId);
        application.setStatus(status);
        application.setProcessInstanceId("pid-10");
        application.setCreateTime(LocalDateTime.now());
        return application;
    }

    private WorkflowTask task(String taskId, Long assigneeId) {
        WorkflowTask task = new WorkflowTask();
        task.setTaskId(taskId);
        task.setAssigneeId(assigneeId);
        task.setProcessInstanceId("pid-10");
        return task;
    }
}
