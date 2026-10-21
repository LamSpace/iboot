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

package com.iboot.admin.infrastructure.workflow;

import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;
import com.iboot.admin.domain.workflow.service.WorkflowService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Flowable 的工作流领域服务实现。
 *
 * <p>把 Flowable 的运行时/历史 API 适配为 {@link WorkflowService} 的领域语义，
 * 所有引擎类型都限制在本包内。流程 key 固定为 {@code leaveApproval}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class FlowableWorkflowAdapter implements WorkflowService {

    private static final String PROCESS_KEY = "leaveApproval";

    private final RuntimeService runtimeService;

    private final TaskService taskService;

    private final HistoryService historyService;

    @SuppressWarnings("all")
    public FlowableWorkflowAdapter(final RuntimeService runtimeService,
                                   final TaskService taskService,
                                   final HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    @Override
    public String startProcess(final Long applicantId, final Long approverId) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, Map.of(
                "applicantId", applicantId,
                "approverId", approverId));
        return instance.getId();
    }

    @Override
    public List<WorkflowTask> findActiveTasks(final Long assigneeId, final int offset, final int limit) {
        return taskService.createTaskQuery()
                .taskAssignee(String.valueOf(assigneeId))
                .orderByTaskCreateTime()
                .desc()
                .listPage(offset, limit)
                .stream()
                .map(this::toWorkflowTask)
                .collect(Collectors.toList());
    }

    @Override
    public long countActiveTasks(final Long assigneeId) {
        return taskService.createTaskQuery()
                .taskAssignee(String.valueOf(assigneeId))
                .count();
    }

    @Override
    public List<WorkflowHistoryItem> findCompletedTasks(final Long assigneeId, final int offset, final int limit) {
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(assigneeId))
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .listPage(offset, limit)
                .stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
    }

    @Override
    public long countCompletedTasks(final Long assigneeId) {
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(assigneeId))
                .finished()
                .count();
    }

    @Override
    public WorkflowTask findActiveTask(final String processInstanceId) {
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        return task == null ? null : toWorkflowTask(task);
    }

    @Override
    public void completeTask(final String taskId, final boolean approved, final String comment) {
        taskService.complete(taskId, Map.of("approved", approved, "comment", comment == null ? "" : comment));
    }

    @Override
    public List<WorkflowHistoryItem> findHistory(final String processInstanceId) {
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (instance == null) {
            return List.of();
        }
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();
        if (tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream().map(this::toHistoryItem).collect(Collectors.toList());
    }

    private WorkflowTask toWorkflowTask(final Task task) {
        WorkflowTask item = new WorkflowTask();
        item.setTaskId(task.getId());
        item.setProcessInstanceId(task.getProcessInstanceId());
        item.setAssigneeId(parseUserId(task.getAssignee()));
        item.setTaskName(task.getName());
        item.setCreateTime(toLocalDateTime(task.getCreateTime()));
        return item;
    }

    private WorkflowHistoryItem toHistoryItem(final HistoricTaskInstance task) {
        WorkflowHistoryItem item = new WorkflowHistoryItem();
        item.setProcessInstanceId(task.getProcessInstanceId());
        item.setTaskId(task.getId());
        item.setActivityName(task.getName());
        item.setActorId(parseUserId(task.getAssignee()));
        item.setTime(toLocalDateTime(task.getEndTime() == null ? task.getCreateTime() : task.getEndTime()));
        return item;
    }

    private Long parseUserId(final String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(final Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
