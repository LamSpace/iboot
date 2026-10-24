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

package com.iboot.admin.domain.backup.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BackupRecord} 状态机单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupRecordTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 24, 2, 0);

    /**
     * 验证新建记录处于运行中。
     */
    @Test
    void startsRunning() {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin", START);

        assertThat(record.getStatus()).isEqualTo(BackupStatus.RUNNING);
        assertThat(record.isRunning()).isTrue();
        assertThat(record.isRestorable()).isFalse();
    }

    /**
     * 验证成功迁移保存产物元数据与行数快照。
     */
    @Test
    void marksSuccess() {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin", START);

        record.markSuccess("minio", "backup/iboot_admin/2026/10/24/B1.sql.enc", 128L, 96L,
                "abc", Map.of("sys_user", 3L), START.plusSeconds(5));

        assertThat(record.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(record.isRestorable()).isTrue();
        assertThat(record.getStoragePath()).contains("B1.sql.enc");
        assertThat(record.getTableCount()).isEqualTo(1);
        assertThat(record.getDurationMs()).isEqualTo(5000L);
        assertThat(record.getErrorMessage()).isNull();
    }

    /**
     * 验证失败迁移保存错误并标记终态。
     */
    @Test
    void marksFailed() {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.SCHEDULED, "iboot_admin", "system", START);

        record.markFailed("导出失败", START.plusSeconds(1));

        assertThat(record.getStatus()).isEqualTo(BackupStatus.FAILED);
        assertThat(record.isRestorable()).isFalse();
        assertThat(record.getErrorMessage()).isEqualTo("导出失败");
    }

    /**
     * 验证终态记录不允许再次迁移。
     */
    @Test
    void rejectsTransitionFromTerminalState() {
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin", START);
        record.markFailed("失败", START.plusSeconds(1));

        assertThatThrownBy(() -> record.markSuccess("local", "path", 1L, 1L, "abc", Map.of(),
                START.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> record.markFailed("再次失败", START.plusSeconds(3)))
                .isInstanceOf(IllegalStateException.class);
    }
}
