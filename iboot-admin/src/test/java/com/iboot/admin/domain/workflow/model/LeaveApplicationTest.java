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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LeaveApplication} 状态流转的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class LeaveApplicationTest {

    /**
     * 验证待审批状态可以通过并记录意见。
     */
    @Test
    void pendingCanBeApproved() {
        LeaveApplication application = pendingApplication();

        application.markApproved("同意");

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(application.getApproveComment()).isEqualTo("同意");
    }

    /**
     * 验证待审批状态可以驳回并记录原因。
     */
    @Test
    void pendingCanBeRejected() {
        LeaveApplication application = pendingApplication();

        application.markRejected("材料不全");

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(application.getApproveComment()).isEqualTo("材料不全");
    }

    /**
     * 验证终态不可再次流转。
     */
    @Test
    void terminalStateCannotTransitionAgain() {
        LeaveApplication application = pendingApplication();
        application.markApproved("同意");

        assertThatThrownBy(() -> application.markRejected("重复处理"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(application.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    /**
     * 验证终态判断。
     */
    @Test
    void terminalStateDetection() {
        assertThat(LeaveStatus.PENDING.isTerminal()).isFalse();
        assertThat(LeaveStatus.APPROVED.isTerminal()).isTrue();
        assertThat(LeaveStatus.REJECTED.isTerminal()).isTrue();
    }

    private LeaveApplication pendingApplication() {
        LeaveApplication application = new LeaveApplication();
        application.setStatus(LeaveStatus.PENDING);
        return application;
    }
}
