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

import java.util.List;

/**
 * 审批详情聚合。
 *
 * <p>包含请假申请本身与按时间排序的审批历史，由应用服务组装后交给接口层。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ApprovalDetail {

    /**
     * 请假申请。
     */
    private LeaveApplication application;

    /**
     * 审批历史，按时间升序。
     */
    private List<WorkflowHistoryItem> history;

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

    public List<WorkflowHistoryItem> getHistory() {
        return history;
    }

    public void setHistory(final List<WorkflowHistoryItem> history) {
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
