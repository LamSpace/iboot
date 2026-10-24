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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupStatus;
import com.iboot.admin.domain.backup.model.BackupTriggerType;
import com.iboot.admin.domain.backup.repository.BackupRecordRepository;
import com.iboot.admin.infrastructure.persistence.mapper.BackupRecordMapper;
import com.iboot.admin.infrastructure.persistence.po.BackupRecordPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 备份记录仓储实现。
 *
 * <p>基于 MyBatis Mapper 完成持久化，并负责枚举与逐表行数 JSON 的转换；
 * 不包含业务规则，事务边界由应用服务控制。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class BackupRecordRepositoryImpl implements BackupRecordRepository {

    /**
     * 逐表行数 JSON 的类型引用。
     */
    private static final TypeReference<LinkedHashMap<String, Long>> STATS_TYPE = new TypeReference<>() {
    };

    /**
     * MyBatis Mapper。
     */
    private final BackupRecordMapper mapper;

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    @SuppressWarnings("all")
    public BackupRecordRepositoryImpl(final BackupRecordMapper mapper, final ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public BackupRecord insert(final BackupRecord record) {
        BackupRecordPO po = toPO(record);
        mapper.insert(po);
        record.setId(po.getId());
        return record;
    }

    @Override
    public boolean update(final BackupRecord record) {
        return mapper.update(toPO(record)) > 0;
    }

    @Override
    public Optional<BackupRecord> findById(final Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<BackupRecord> findRunning() {
        return Optional.ofNullable(mapper.selectRunning()).map(this::toDomain);
    }

    @Override
    public List<BackupRecord> findPage(final String status, final int offset, final int limit) {
        return mapper.selectPage(status, offset, limit).stream().map(this::toDomain).toList();
    }

    @Override
    public long count(final String status) {
        return mapper.countPage(status);
    }

    @Override
    public int resetStaleRunning(final LocalDateTime threshold, final LocalDateTime now) {
        return mapper.resetStaleRunning(threshold, now);
    }

    @Override
    public long countAll() {
        return mapper.countAll();
    }

    @Override
    public long sumCiphertextSize() {
        return mapper.sumCiphertextSize();
    }

    @Override
    public LocalDateTime lastSuccessAt() {
        return mapper.lastSuccessAt();
    }

    private BackupRecord toDomain(final BackupRecordPO po) {
        BackupRecord record = new BackupRecord();
        record.setId(po.getId());
        record.setBackupNo(po.getBackupNo());
        record.setTriggerType(po.getTriggerType() == null ? null : BackupTriggerType.valueOf(po.getTriggerType()));
        record.setStatus(po.getStatus() == null ? null : BackupStatus.valueOf(po.getStatus()));
        record.setDatabaseName(po.getDatabaseName());
        record.setStorageType(po.getStorageType());
        record.setStoragePath(po.getStoragePath());
        record.setCiphertextSize(po.getCiphertextSize());
        record.setPlaintextSize(po.getPlaintextSize());
        record.setChecksumSha256(po.getChecksumSha256());
        record.setEncryption(po.getEncryption());
        record.setDumpTool(po.getDumpTool());
        record.setTableCount(po.getTableCount());
        record.setTableStats(parseStats(po.getTableStatsJson()));
        record.setStartedAt(po.getStartedAt());
        record.setFinishedAt(po.getFinishedAt());
        record.setDurationMs(po.getDurationMs());
        record.setErrorMessage(po.getErrorMessage());
        record.setCreatedBy(po.getCreatedBy());
        record.setCreateTime(po.getCreateTime());
        record.setUpdateTime(po.getUpdateTime());
        return record;
    }

    private BackupRecordPO toPO(final BackupRecord record) {
        BackupRecordPO po = new BackupRecordPO();
        po.setId(record.getId());
        po.setBackupNo(record.getBackupNo());
        po.setTriggerType(record.getTriggerType() == null ? null : record.getTriggerType().name());
        po.setStatus(record.getStatus() == null ? null : record.getStatus().name());
        po.setDatabaseName(record.getDatabaseName());
        po.setStorageType(record.getStorageType());
        po.setStoragePath(record.getStoragePath());
        po.setCiphertextSize(record.getCiphertextSize());
        po.setPlaintextSize(record.getPlaintextSize());
        po.setChecksumSha256(record.getChecksumSha256());
        po.setEncryption(record.getEncryption());
        po.setDumpTool(record.getDumpTool());
        po.setTableCount(record.getTableCount());
        po.setTableStatsJson(writeStats(record.getTableStats()));
        po.setStartedAt(record.getStartedAt());
        po.setFinishedAt(record.getFinishedAt());
        po.setDurationMs(record.getDurationMs());
        po.setErrorMessage(record.getErrorMessage());
        po.setCreatedBy(record.getCreatedBy());
        po.setCreateTime(record.getCreateTime());
        po.setUpdateTime(record.getUpdateTime());
        return po;
    }

    private Map<String, Long> parseStats(final String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, STATS_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析备份行数快照失败", e);
        }
    }

    private String writeStats(final Map<String, Long> stats) {
        if (stats == null || stats.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化备份行数快照失败", e);
        }
    }
}
