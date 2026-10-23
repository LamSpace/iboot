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

package com.iboot.admin.infrastructure.notification;

import com.iboot.admin.application.service.notification.NotificationApplicationService;
import com.iboot.admin.domain.notification.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link NotificationRetryTask} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationRetryTaskTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 23, 12, 0);

    private NotificationDeliveryRepository repository;

    private NotificationApplicationService service;

    private NotificationRetryTask task;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationDeliveryRepository.class);
        service = mock(NotificationApplicationService.class);
        NotificationProperties properties = new NotificationProperties();
        properties.getRetry().setBatchSize(10);
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
        task = new NotificationRetryTask(repository, service, properties, clock);
    }

    /**
     * 验证先重置超时记录，再逐条投递到期记录。
     */
    @Test
    void retriesDueRecordsAndResetsStaleSending() {
        when(repository.findDueIds(NOW, 10)).thenReturn(List.of(1L, 2L));

        task.retry();

        verify(repository).resetStaleSending(NOW.minusMinutes(5), NOW);
        verify(repository).findDueIds(NOW, 10);
        verify(service).dispatch(1L);
        verify(service).dispatch(2L);
    }

    /**
     * 验证单条投递异常不影响后续记录。
     */
    @Test
    void continuesWhenOneRecordFails() {
        when(repository.findDueIds(NOW, 10)).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("boom")).when(service).dispatch(1L);

        task.retry();

        verify(service).dispatch(2L);
    }

    /**
     * 验证重置失败时仍继续处理到期记录。
     */
    @Test
    void continuesWhenStaleResetFails() {
        when(repository.resetStaleSending(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("db down"));
        when(repository.findDueIds(NOW, 10)).thenReturn(List.of(1L));

        task.retry();

        verify(service).dispatch(1L);
    }
}
