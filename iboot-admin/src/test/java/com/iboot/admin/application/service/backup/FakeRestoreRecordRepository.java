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

package com.iboot.admin.application.service.backup;

import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.RestoreStatus;
import com.iboot.admin.domain.backup.repository.RestoreRecordRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内存演练记录仓储，供应用服务单元测试使用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class FakeRestoreRecordRepository implements RestoreRecordRepository {

    private final Map<Long, RestoreRecord> records = new LinkedHashMap<>();

    private long sequence;

    @Override
    public RestoreRecord insert(final RestoreRecord record) {
        record.setId(++sequence);
        records.put(record.getId(), copy(record));
        return record;
    }

    @Override
    public boolean update(final RestoreRecord record) {
        records.put(record.getId(), copy(record));
        return true;
    }

    @Override
    public Optional<RestoreRecord> findById(final Long id) {
        return Optional.ofNullable(records.get(id)).map(this::copy);
    }

    @Override
    public Optional<RestoreRecord> findRunning() {
        return records.values().stream()
                .filter(record -> record.getStatus() == RestoreStatus.RUNNING)
                .findFirst()
                .map(this::copy);
    }

    @Override
    public List<RestoreRecord> findPage(final Long backupId, final int offset, final int limit) {
        return records.values().stream()
                .filter(record -> backupId == null || backupId.equals(record.getBackupId()))
                .sorted(Comparator.comparing(RestoreRecord::getId).reversed())
                .skip(offset)
                .limit(limit)
                .map(this::copy)
                .toList();
    }

    @Override
    public long count(final Long backupId) {
        return records.values().stream()
                .filter(record -> backupId == null || backupId.equals(record.getBackupId()))
                .count();
    }

    private RestoreRecord copy(final RestoreRecord source) {
        RestoreRecord target = new RestoreRecord();
        target.setId(source.getId());
        target.setBackupId(source.getBackupId());
        target.setTargetSchema(source.getTargetSchema());
        target.setStatus(source.getStatus());
        target.setTableCount(source.getTableCount());
        target.setTableStats(source.getTableStats());
        target.setMismatches(source.getMismatches());
        target.setStartedAt(source.getStartedAt());
        target.setFinishedAt(source.getFinishedAt());
        target.setDurationMs(source.getDurationMs());
        target.setErrorMessage(source.getErrorMessage());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreateTime(source.getCreateTime());
        target.setUpdateTime(source.getUpdateTime());
        return target;
    }
}
