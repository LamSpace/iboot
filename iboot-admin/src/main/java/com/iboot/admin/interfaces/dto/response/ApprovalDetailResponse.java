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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 审批详情响应。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "审批详情响应")
public class ApprovalDetailResponse {

    /**
     * 请假申请。
     */
    @Schema(description = "请假申请")
    private LeaveApplicationResponse application;

    /**
     * 审批历史，按时间升序。
     */
    @Schema(description = "审批历史")
    private List<ApprovalHistoryResponse> history;

    public LeaveApplicationResponse getApplication() {
        return application;
    }

    public void setApplication(final LeaveApplicationResponse application) {
        this.application = application;
    }

    public List<ApprovalHistoryResponse> getHistory() {
        return history;
    }

    public void setHistory(final List<ApprovalHistoryResponse> history) {
        this.history = history;
    }
}
