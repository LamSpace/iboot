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

package com.iboot.admin.domain.workflow.service;

import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;

import java.util.List;

/**
 * 工作流领域服务。
 *
 * <p>对流程引擎的抽象：启动流程、查询待办、完成任务与查询历史。实现必须位于
 * {@code infrastructure} 层，本接口及其参数不得依赖任何流程引擎类型。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface WorkflowService {

    /**
     * 启动请假审批流程。
     *
     * @param applicantId 申请人用户 ID
     * @param approverId  审批人用户 ID
     * @return 流程实例 ID
     */
    String startProcess(Long applicantId, Long approverId);

    /**
     * 分页查询待办任务。
     *
     * @param assigneeId 处理人用户 ID
     * @param offset     起始偏移，从 0 开始
     * @param limit      每页数量
     * @return 待办任务列表，无数据时为空列表
     */
    List<WorkflowTask> findActiveTasks(Long assigneeId, int offset, int limit);

    /**
     * 统计待办任务数量。
     *
     * @param assigneeId 处理人用户 ID
     * @return 任务数
     */
    long countActiveTasks(Long assigneeId);

    /**
     * 分页查询已办任务。
     *
     * @param assigneeId 处理人用户 ID
     * @param offset     起始偏移，从 0 开始
     * @param limit      每页数量
     * @return 已办历史节点，无数据时为空列表
     */
    List<WorkflowHistoryItem> findCompletedTasks(Long assigneeId, int offset, int limit);

    /**
     * 统计已办任务数量。
     *
     * @param assigneeId 处理人用户 ID
     * @return 任务数
     */
    long countCompletedTasks(Long assigneeId);

    /**
     * 查询流程实例当前的待办任务。
     *
     * @param processInstanceId 流程实例 ID
     * @return 当前任务；流程已结束或不存在时返回 {@code null}
     */
    WorkflowTask findActiveTask(String processInstanceId);

    /**
     * 完成审批任务。
     *
     * @param taskId  任务 ID
     * @param approved {@code true} 通过，{@code false} 驳回
     * @param comment 审批意见或驳回原因
     */
    void completeTask(String taskId, boolean approved, String comment);

    /**
     * 查询流程实例的完整历史。
     *
     * @param processInstanceId 流程实例 ID
     * @return 按时间升序排列的历史节点，无数据时为空列表
     */
    List<WorkflowHistoryItem> findHistory(String processInstanceId);
}
