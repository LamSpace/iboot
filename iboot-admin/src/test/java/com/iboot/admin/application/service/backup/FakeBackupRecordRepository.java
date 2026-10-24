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

import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupStatus;
import com.iboot.admin.domain.backup.repository.BackupRecordRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内存备份记录仓储，供应用服务单元测试使用。
 *
 * <p>插入与更新都保存副本，模拟真实持久化边界，避免服务持有的对象直接改变仓储状态。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class FakeBackupRecordRepository implements BackupRecordRepository {

    private final Map<Long, BackupRecord> records = new LinkedHashMap<>();

    private long sequence;

    private int resetCalls;

    private boolean failUpdateOnce;

    @Override
    public BackupRecord insert(final BackupRecord record) {
        record.setId(++sequence);
        records.put(record.getId(), copy(record));
        return record;
    }

    @Override
    public boolean update(final BackupRecord record) {
        if (failUpdateOnce) {
            failUpdateOnce = false;
            throw new IllegalStateException("模拟数据库更新失败");
        }
        records.put(record.getId(), copy(record));
        return true;
    }

    @Override
    public Optional<BackupRecord> findById(final Long id) {
        return Optional.ofNullable(records.get(id)).map(this::copy);
    }

    @Override
    public Optional<BackupRecord> findRunning() {
        return records.values().stream()
                .filter(record -> record.getStatus() == BackupStatus.RUNNING)
                .findFirst()
                .map(this::copy);
    }

    @Override
    public List<BackupRecord> findPage(final String status, final int offset, final int limit) {
        return records.values().stream()
                .filter(record -> status == null || status.isBlank()
                        || (record.getStatus() != null && record.getStatus().name().equals(status)))
                .sorted(Comparator.comparing(BackupRecord::getId).reversed())
                .skip(offset)
                .limit(limit)
                .map(this::copy)
                .toList();
    }

    @Override
    public long count(final String status) {
        return records.values().stream()
                .filter(record -> status == null || status.isBlank()
                        || (record.getStatus() != null && record.getStatus().name().equals(status)))
                .count();
    }

    @Override
    public int resetStaleRunning(final LocalDateTime threshold, final LocalDateTime now) {
        resetCalls++;
        List<BackupRecord> stale = new ArrayList<>(records.values().stream()
                .filter(record -> record.getStatus() == BackupStatus.RUNNING)
                .filter(record -> record.getStartedAt() != null && record.getStartedAt().isBefore(threshold))
                .toList());
        for (BackupRecord record : stale) {
            record.markFailed("执行超时", now);
        }
        return stale.size();
    }

    @Override
    public long countAll() {
        return records.size();
    }

    @Override
    public long sumCiphertextSize() {
        return records.values().stream()
                .filter(record -> record.getStatus() == BackupStatus.SUCCESS)
                .map(BackupRecord::getCiphertextSize)
                .filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    @Override
    public LocalDateTime lastSuccessAt() {
        return records.values().stream()
                .filter(record -> record.getStatus() == BackupStatus.SUCCESS)
                .map(BackupRecord::getFinishedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    int resetCalls() {
        return resetCalls;
    }

    void failNextUpdate() {
        this.failUpdateOnce = true;
    }

    /**
     * 直接替换一条记录，供集成测试构造不一致快照。
     *
     * @param record 新记录
     */
    void replace(final BackupRecord record) {
        records.put(record.getId(), copy(record));
    }

    private BackupRecord copy(final BackupRecord source) {
        BackupRecord target = new BackupRecord();
        target.setId(source.getId());
        target.setBackupNo(source.getBackupNo());
        target.setTriggerType(source.getTriggerType());
        target.setStatus(source.getStatus());
        target.setDatabaseName(source.getDatabaseName());
        target.setStorageType(source.getStorageType());
        target.setStoragePath(source.getStoragePath());
        target.setCiphertextSize(source.getCiphertextSize());
        target.setPlaintextSize(source.getPlaintextSize());
        target.setChecksumSha256(source.getChecksumSha256());
        target.setEncryption(source.getEncryption());
        target.setDumpTool(source.getDumpTool());
        target.setTableCount(source.getTableCount());
        target.setTableStats(source.getTableStats());
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
