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

import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;

/**
 * 待办或已办列表项。
 *
 * <p>把业务申请与流程节点组合起来供接口层转换：待办项包含当前任务，
 * 已办项包含已完成的历史节点。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ApprovalItem {

    /**
     * 请假申请。
     */
    private LeaveApplication application;

    /**
     * 当前待办任务；已办项为 {@code null}。
     */
    private WorkflowTask task;

    /**
     * 已办历史节点；待办项为 {@code null}。
     */
    private WorkflowHistoryItem history;

    /**
     * 申请人显示名。
     */
    private String applicantName;

    /**
     * 审批人显示名。
     */
    private String approverName;

    public LeaveApplication getApplication() {
        return application;
    }

    public void setApplication(final LeaveApplication application) {
        this.application = application;
    }

    public WorkflowTask getTask() {
        return task;
    }

    public void setTask(final WorkflowTask task) {
        this.task = task;
    }

    public WorkflowHistoryItem getHistory() {
        return history;
    }

    public void setHistory(final WorkflowHistoryItem history) {
        this.history = history;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(final String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(final String approverName) {
        this.approverName = approverName;
    }
}
