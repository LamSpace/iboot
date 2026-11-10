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

package com.iboot.workflow.application.service;

import com.iboot.kernel.web.BusinessException;
import com.iboot.workflow.application.service.quota.LeaveQuotaTransferPort;
import com.iboot.workflow.infrastructure.directory.WorkflowUser;
import com.iboot.workflow.infrastructure.directory.WorkflowUserReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 额度划转应用服务测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class LeaveQuotaTransferApplicationServiceTest {

    private LeaveQuotaTransferPort port;

    private WorkflowUserReader userReader;

    private LeaveQuotaTransferApplicationService service;

    @BeforeEach
    void setUp() {
        port = mock(LeaveQuotaTransferPort.class);
        userReader = mock(WorkflowUserReader.class);
        service = new LeaveQuotaTransferApplicationService(port, userReader);
        when(userReader.findById(2L)).thenReturn(Optional.of(new WorkflowUser(2L, 1, "bob", "Bob")));
    }

    /**
     * 验证合法请求生成划转标识并调用 TCC 端口。
     */
    @Test
    void transferDelegatesWithGeneratedId() {
        String transferId = service.transfer(1L, 2L, 2026, 1);

        assertThat(transferId).isNotBlank();
        verify(port).transfer(transferId, 1L, 2L, 2026, 1);
    }

    /**
     * 验证划出方与划入方相同时拒绝请求。
     */
    @Test
    void rejectsSameUser() {
        assertThatThrownBy(() -> service.transfer(2L, 2L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
        verify(port, never()).transfer(anyString(), anyLong(), anyLong(), anyInt(), anyInt());
    }

    /**
     * 验证划入方不存在时拒绝请求。
     */
    @Test
    void rejectsMissingTarget() {
        when(userReader.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(1L, 9L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }

    /**
     * 验证划入方停用时拒绝请求。
     */
    @Test
    void rejectsDisabledTarget() {
        when(userReader.findById(3L)).thenReturn(Optional.of(new WorkflowUser(3L, 0, "carol", "Carol")));

        assertThatThrownBy(() -> service.transfer(1L, 3L, 2026, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }

    /**
     * 验证划转次数必须为正数。
     */
    @Test
    void rejectsNonPositiveCount() {
        assertThatThrownBy(() -> service.transfer(1L, 2L, 2026, 0))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }

    /**
     * 验证年度越界时拒绝请求。
     */
    @Test
    void rejectsInvalidYear() {
        assertThatThrownBy(() -> service.transfer(1L, 2L, 1999, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }
}
