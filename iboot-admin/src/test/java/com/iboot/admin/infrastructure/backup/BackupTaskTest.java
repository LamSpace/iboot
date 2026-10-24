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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.backup;

import com.iboot.admin.application.service.backup.BackupApplicationService;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupStatus;
import com.iboot.admin.domain.backup.model.BackupTriggerType;
import com.iboot.admin.domain.backup.repository.BackupRecordRepository;
import com.iboot.admin.domain.backup.service.BackupExecutor;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link BackupTask} 与定时触发行为的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupTaskTest {

    /**
     * 验证任务委托给应用服务。
     */
    @Test
    void runDelegatesToService() {
        BackupApplicationService service = mock(BackupApplicationService.class);

        new BackupTask(service).run();

        verify(service).triggerScheduled();
    }

    /**
     * 验证功能关闭时定时触发不做任何持久化操作。
     */
    @Test
    void scheduledTriggerIsNoOpWhenDisabled() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(false);
        BackupRecordRepository repository = mock(BackupRecordRepository.class);
        BackupApplicationService service = new BackupApplicationService(properties, repository,
                mock(BackupExecutor.class), BackupCipher.disabled(), mock(FileStorageStrategy.class),
                mock(AsyncTaskExecutor.class), fixedClock());

        service.triggerScheduled();

        verifyNoInteractions(repository);
    }

    /**
     * 验证已有备份运行时定时触发只跳过，不插入新记录。
     */
    @Test
    void scheduledTriggerSkipsWhenRunning() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(true);
        BackupRecordRepository repository = mock(BackupRecordRepository.class);
        when(repository.findRunning()).thenReturn(Optional.of(runningRecord()));
        BackupApplicationService service = new BackupApplicationService(properties, repository,
                mock(BackupExecutor.class), BackupCipher.disabled(), mock(FileStorageStrategy.class),
                mock(AsyncTaskExecutor.class), fixedClock());

        service.triggerScheduled();

        verify(repository, never()).insert(any());
    }

    private BackupRecord runningRecord() {
        return BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin",
                LocalDateTime.now(fixedClock()));
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-10-23T18:00:00Z"), ZoneId.of("Asia/Shanghai"));
    }
}
