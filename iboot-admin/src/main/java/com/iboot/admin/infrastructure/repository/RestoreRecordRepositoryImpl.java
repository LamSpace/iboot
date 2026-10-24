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
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.RestoreStatus;
import com.iboot.admin.domain.backup.model.TableCountMismatch;
import com.iboot.admin.domain.backup.repository.RestoreRecordRepository;
import com.iboot.admin.infrastructure.persistence.mapper.RestoreRecordMapper;
import com.iboot.admin.infrastructure.persistence.po.RestoreRecordPO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 恢复演练记录仓储实现。
 *
 * <p>基于 MyBatis Mapper 完成持久化，并负责枚举、逐表行数与差异明细 JSON 的转换。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class RestoreRecordRepositoryImpl implements RestoreRecordRepository {

    /**
     * 逐表行数 JSON 的类型引用。
     */
    private static final TypeReference<LinkedHashMap<String, Long>> STATS_TYPE = new TypeReference<>() {
    };

    /**
     * 差异明细 JSON 的类型引用。
     */
    private static final TypeReference<ArrayList<TableCountMismatch>> MISMATCH_TYPE = new TypeReference<>() {
    };

    /**
     * MyBatis Mapper。
     */
    private final RestoreRecordMapper mapper;

    /**
     * JSON 序列化器。
     */
    private final ObjectMapper objectMapper;

    @SuppressWarnings("all")
    public RestoreRecordRepositoryImpl(final RestoreRecordMapper mapper, final ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public RestoreRecord insert(final RestoreRecord record) {
        RestoreRecordPO po = toPO(record);
        mapper.insert(po);
        record.setId(po.getId());
        return record;
    }

    @Override
    public boolean update(final RestoreRecord record) {
        return mapper.update(toPO(record)) > 0;
    }

    @Override
    public Optional<RestoreRecord> findById(final Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<RestoreRecord> findRunning() {
        return Optional.ofNullable(mapper.selectRunning()).map(this::toDomain);
    }

    @Override
    public List<RestoreRecord> findPage(final Long backupId, final int offset, final int limit) {
        return mapper.selectPage(backupId, offset, limit).stream().map(this::toDomain).toList();
    }

    @Override
    public long count(final Long backupId) {
        return mapper.countPage(backupId);
    }

    private RestoreRecord toDomain(final RestoreRecordPO po) {
        RestoreRecord record = new RestoreRecord();
        record.setId(po.getId());
        record.setBackupId(po.getBackupId());
        record.setTargetSchema(po.getTargetSchema());
        record.setStatus(po.getStatus() == null ? null : RestoreStatus.valueOf(po.getStatus()));
        record.setTableCount(po.getTableCount());
        record.setTableStats(parseStats(po.getTableStatsJson()));
        record.setMismatches(parseMismatches(po.getMismatchJson()));
        record.setStartedAt(po.getStartedAt());
        record.setFinishedAt(po.getFinishedAt());
        record.setDurationMs(po.getDurationMs());
        record.setErrorMessage(po.getErrorMessage());
        record.setCreatedBy(po.getCreatedBy());
        record.setCreateTime(po.getCreateTime());
        record.setUpdateTime(po.getUpdateTime());
        return record;
    }

    private RestoreRecordPO toPO(final RestoreRecord record) {
        RestoreRecordPO po = new RestoreRecordPO();
        po.setId(record.getId());
        po.setBackupId(record.getBackupId());
        po.setTargetSchema(record.getTargetSchema());
        po.setStatus(record.getStatus() == null ? null : record.getStatus().name());
        po.setTableCount(record.getTableCount());
        po.setTableStatsJson(writeJson(record.getTableStats()));
        po.setMismatchJson(writeJson(record.getMismatches()));
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
            throw new IllegalStateException("解析演练行数快照失败", e);
        }
    }

    private List<TableCountMismatch> parseMismatches(final String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, MISMATCH_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析演练差异明细失败", e);
        }
    }

    private String writeJson(final Object value) {
        if (value == null || (value instanceof Map<?, ?> map && map.isEmpty())
                || (value instanceof List<?> list && list.isEmpty())) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化演练结果失败", e);
        }
    }
}
