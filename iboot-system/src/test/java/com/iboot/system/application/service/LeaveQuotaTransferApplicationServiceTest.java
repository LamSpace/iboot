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

package com.iboot.system.application.service;

import com.iboot.kernel.web.BusinessException;
import com.iboot.system.domain.system.model.LeaveQuotaTransfer;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;
import com.iboot.system.domain.system.repository.LeaveQuotaTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 额度划转 TCC 应用服务测试。
 *
 * <p>直接驱动事务模板执行状态机，验证 Try/Confirm/Cancel 的额度调整、幂等、
 * 空回滚与悬挂保护。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class LeaveQuotaTransferApplicationServiceTest {

    private LeaveQuotaTransferRepository repository;

    private TransactionTemplate transactionTemplate;

    private LeaveQuotaApplicationService leaveQuotaApplicationService;

    private LeaveQuotaTransferApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveQuotaTransferRepository.class);
        transactionTemplate = mock(TransactionTemplate.class);
        leaveQuotaApplicationService = mock(LeaveQuotaApplicationService.class);
        when(leaveQuotaApplicationService.defaultCount()).thenReturn(10);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        service = new LeaveQuotaTransferApplicationService(repository, transactionTemplate,
                leaveQuotaApplicationService);
    }

    /**
     * 验证 Try 冻结划出方并登记 TRYING。
     */
    @Test
    void tryFreezesSourceAndRegistersTrying() {
        when(repository.insertTrying(any())).thenReturn(true);
        when(repository.freeze(1L, 2026, 2)).thenReturn(1);

        assertThat(service.tryTransfer("xid-1", "t-1", 1L, 2L, 2026, 2)).isTrue();

        verify(repository).ensureQuota(1L, 2026, 10);
        verify(repository).ensureQuota(2L, 2026, 10);
        verify(repository).freeze(1L, 2026, 2);
    }

    /**
     * 验证同一 XID 重复 Try 不重复冻结。
     */
    @Test
    void tryIsIdempotentByXid() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.TRYING)));

        assertThat(service.tryTransfer("xid-1", "t-1", 1L, 2L, 2026, 2)).isTrue();

        verify(repository, never()).insertTrying(any());
        verify(repository, never()).freeze(1L, 2026, 2);
    }

    /**
     * 验证命中取消墓碑时拒绝重新冻结（悬挂保护）。
     */
    @Test
    void tryRejectsCancelledTombstone() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.CANCELLED)));

        assertThatThrownBy(() -> service.tryTransfer("xid-1", "t-1", 1L, 2L, 2026, 2))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(409));
        verify(repository, never()).freeze(1L, 2026, 2);
    }

    /**
     * 验证额度不足时 Try 失败。
     */
    @Test
    void tryThrowsWhenQuotaInsufficient() {
        when(repository.insertTrying(any())).thenReturn(true);
        when(repository.freeze(1L, 2026, 2)).thenReturn(0);

        assertThatThrownBy(() -> service.tryTransfer("xid-1", "t-1", 1L, 2L, 2026, 2))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
    }

    /**
     * 验证 Confirm 结算冻结并增加划入方总额度。
     */
    @Test
    void confirmSettlesTransfer() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.TRYING)));
        when(repository.settleFrozen(1L, 2026, 2)).thenReturn(1);
        when(repository.increaseTotal(2L, 2026, 2)).thenReturn(1);

        assertThat(service.confirm("xid-1")).isTrue();

        verify(repository).markTerminal("xid-1", LeaveQuotaTransferStatus.CONFIRMED);
    }

    /**
     * 验证重复 Confirm 不重复调整额度。
     */
    @Test
    void confirmIsIdempotent() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.CONFIRMED)));

        assertThat(service.confirm("xid-1")).isTrue();

        verify(repository, never()).settleFrozen(1L, 2026, 2);
        verify(repository, never()).increaseTotal(2L, 2026, 2);
    }

    /**
     * 验证取消后的迟到 Confirm 不改变额度。
     */
    @Test
    void confirmAfterCancelIsIgnored() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.CANCELLED)));

        assertThat(service.confirm("xid-1")).isTrue();

        verify(repository, never()).settleFrozen(1L, 2026, 2);
    }

    /**
     * 验证 Cancel 释放冻结并迁移到 CANCELLED。
     */
    @Test
    void cancelReleasesFrozen() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.TRYING)));
        when(repository.releaseFrozen(1L, 2026, 2)).thenReturn(1);

        assertThat(service.cancel("xid-1", "t-1", 1L, 2L, 2026, 2)).isTrue();

        verify(repository).markTerminal("xid-1", LeaveQuotaTransferStatus.CANCELLED);
    }

    /**
     * 验证空回滚写入取消墓碑且不改变额度。
     */
    @Test
    void cancelWithoutTryWritesTombstone() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.empty());
        when(repository.insertTombstone(any())).thenReturn(true);

        assertThat(service.cancel("xid-1", "t-1", 1L, 2L, 2026, 2)).isTrue();

        verify(repository).insertTombstone(any());
        verify(repository, never()).releaseFrozen(any(), anyInt(), anyInt());
    }

    /**
     * 验证重复 Cancel 幂等。
     */
    @Test
    void cancelIsIdempotent() {
        when(repository.findByXid("xid-1")).thenReturn(Optional.of(
                new LeaveQuotaTransfer("t-1", "xid-1", 1L, 2L, 2026, 2, LeaveQuotaTransferStatus.CANCELLED)));

        assertThat(service.cancel("xid-1", "t-1", 1L, 2L, 2026, 2)).isTrue();

        verify(repository, never()).releaseFrozen(1L, 2026, 2);
    }
}
