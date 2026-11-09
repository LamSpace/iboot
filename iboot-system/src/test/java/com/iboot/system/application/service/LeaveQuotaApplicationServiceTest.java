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
import com.iboot.system.domain.system.model.LeaveQuota;
import com.iboot.system.domain.system.repository.LeaveQuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 请假额度应用服务测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class LeaveQuotaApplicationServiceTest {

    private LeaveQuotaRepository repository;

    private ConfigApplicationService configApplicationService;

    private LeaveQuotaApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LeaveQuotaRepository.class);
        configApplicationService = mock(ConfigApplicationService.class);
        service = new LeaveQuotaApplicationService(repository, configApplicationService);
    }

    /**
     * 验证无额度行时按配置默认值返回。
     */
    @Test
    void viewUsesConfiguredDefault() {
        when(configApplicationService.getConfigValue(LeaveQuotaApplicationService.DEFAULT_COUNT_KEY))
                .thenReturn("5");
        when(repository.findByUserIdAndYear(1L, 2026)).thenReturn(Optional.empty());

        LeaveQuota quota = service.view(1L, 2026);

        assertThat(quota.getTotalCount()).isEqualTo(5);
        assertThat(quota.getUsedCount()).isZero();
        assertThat(quota.remainingCount()).isEqualTo(5);
    }

    /**
     * 验证默认值非法时回落到 10。
     */
    @Test
    void viewFallsBackWhenDefaultInvalid() {
        when(configApplicationService.getConfigValue(LeaveQuotaApplicationService.DEFAULT_COUNT_KEY))
                .thenReturn("abc");
        when(repository.findByUserIdAndYear(1L, 2026)).thenReturn(Optional.empty());

        assertThat(service.view(1L, 2026).getTotalCount()).isEqualTo(10);
    }

    /**
     * 验证同一申请重复扣减时直接返回当前额度。
     */
    @Test
    void deductIsIdempotent() {
        LeaveQuota existing = new LeaveQuota(1L, 2026, 10, 1, 0);
        when(repository.insertChange("leave-1", 1L, 2026, 1, "DEDUCT")).thenReturn(false);
        when(repository.findByUserIdAndYear(1L, 2026)).thenReturn(Optional.of(existing));

        LeaveQuota result = service.deduct("leave-1", 1L, 2026);

        assertThat(result.getUsedCount()).isEqualTo(1);
        verify(repository, never()).deductOne(1L, 2026);
    }

    /**
     * 验证额度不足时抛出业务异常且不返回额度。
     */
    @Test
    void deductThrowsWhenInsufficient() {
        when(repository.insertChange("leave-2", 1L, 2026, 1, "DEDUCT")).thenReturn(true);
        when(configApplicationService.getConfigValue(LeaveQuotaApplicationService.DEFAULT_COUNT_KEY))
                .thenReturn("1");
        when(repository.deductOne(1L, 2026)).thenReturn(0);

        assertThatThrownBy(() -> service.deduct("leave-2", 1L, 2026))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode()).isEqualTo(400));
        verify(repository).ensureQuota(1L, 2026, 1);
    }

    /**
     * 验证扣减成功后返回最新额度。
     */
    @Test
    void deductReturnsUpdatedQuota() {
        LeaveQuota updated = new LeaveQuota(1L, 2026, 10, 1, 0);
        when(repository.insertChange("leave-3", 1L, 2026, 1, "DEDUCT")).thenReturn(true);
        when(configApplicationService.getConfigValue(LeaveQuotaApplicationService.DEFAULT_COUNT_KEY))
                .thenReturn("10");
        when(repository.deductOne(1L, 2026)).thenReturn(1);
        when(repository.findByUserIdAndYear(1L, 2026)).thenReturn(Optional.of(updated));

        LeaveQuota result = service.deduct("leave-3", 1L, 2026);

        assertThat(result.getUsedCount()).isEqualTo(1);
        assertThat(result.remainingCount()).isEqualTo(9);
        verify(repository).ensureQuota(1L, 2026, 10);
    }
}
