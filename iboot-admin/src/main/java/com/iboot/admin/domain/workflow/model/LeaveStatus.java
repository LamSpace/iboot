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

package com.iboot.admin.domain.workflow.model;

/**
 * 请假申请状态。
 *
 * <p>状态只能从 {@link #PENDING} 进入 {@link #APPROVED} 或 {@link #REJECTED}，
 * 终态之间不可互转，也不可回到待审批。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum LeaveStatus {

    /**
     * 待审批。
     */
    PENDING,

    /**
     * 审批通过，流程已归档。
     */
    APPROVED,

    /**
     * 审批驳回。
     */
    REJECTED;

    /**
     * 判断当前状态是否为终态。
     *
     * @return 非待审批状态返回 {@code true}
     */
    public boolean isTerminal() {
        return this != PENDING;
    }
}
