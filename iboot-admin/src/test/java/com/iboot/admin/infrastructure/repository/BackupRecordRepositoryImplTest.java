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

package com.iboot.admin.infrastructure.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupTriggerType;
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.TableCountMismatch;
import com.iboot.admin.infrastructure.persistence.mapper.BackupRecordMapper;
import com.iboot.admin.infrastructure.persistence.mapper.RestoreRecordMapper;
import com.iboot.admin.infrastructure.persistence.po.BackupRecordPO;
import com.iboot.admin.infrastructure.persistence.po.RestoreRecordPO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 备份与演练仓储实现的单元测试。
 *
 * <p>重点验证 MyBatis 生成键会回填到领域对象，以及行数快照与差异明细的 JSON 往返。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupRecordRepositoryImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证插入后回填生成的主键。
     */
    @Test
    void insertBackfillsGeneratedId() {
        BackupRecordMapper mapper = mock(BackupRecordMapper.class);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            BackupRecordPO po = invocation.getArgument(0);
            po.setId(42L);
            return 1;
        });
        BackupRecordRepositoryImpl repository = new BackupRecordRepositoryImpl(mapper, objectMapper);
        BackupRecord record = BackupRecord.start("B1", BackupTriggerType.MANUAL, "iboot_admin", "admin",
                LocalDateTime.now());

        repository.insert(record);

        assertThat(record.getId()).isEqualTo(42L);
    }

    /**
     * 验证逐表行数快照可从 JSON 还原。
     */
    @Test
    void readsTableStatsJson() {
        BackupRecordMapper mapper = mock(BackupRecordMapper.class);
        BackupRecordPO po = new BackupRecordPO();
        po.setId(1L);
        po.setStatus("SUCCESS");
        po.setTriggerType("MANUAL");
        po.setTableStatsJson("{\"sys_user\":3,\"sys_role\":2}");
        when(mapper.selectById(1L)).thenReturn(po);
        BackupRecordRepositoryImpl repository = new BackupRecordRepositoryImpl(mapper, objectMapper);

        BackupRecord record = repository.findById(1L).orElseThrow();

        assertThat(record.getTableStats()).containsEntry("sys_user", 3L).containsEntry("sys_role", 2L);
    }

    /**
     * 验证演练记录插入后回填主键，并可从 JSON 还原差异明细。
     */
    @Test
    void restoreRecordBackfillsIdAndReadsMismatches() {
        RestoreRecordMapper mapper = mock(RestoreRecordMapper.class);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            RestoreRecordPO po = invocation.getArgument(0);
            po.setId(7L);
            return 1;
        });
        RestoreRecordPO po = new RestoreRecordPO();
        po.setId(7L);
        po.setStatus("MISMATCH");
        po.setTableStatsJson("{\"sys_user\":3}");
        po.setMismatchJson("[{\"tableName\":\"sys_user\",\"expected\":3,\"actual\":2}]");
        when(mapper.selectById(7L)).thenReturn(po);
        RestoreRecordRepositoryImpl repository = new RestoreRecordRepositoryImpl(mapper, objectMapper);
        RestoreRecord record = RestoreRecord.start(1L, "iboot_restore_drill", "admin", LocalDateTime.now());

        repository.insert(record);
        RestoreRecord loaded = repository.findById(7L).orElseThrow();

        assertThat(record.getId()).isEqualTo(7L);
        assertThat(loaded.getMismatches()).containsExactly(new TableCountMismatch("sys_user", 3L, 2L));
        assertThat(loaded.getTableStats()).isEqualTo(Map.of("sys_user", 3L));
    }
}
