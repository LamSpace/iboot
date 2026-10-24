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

package com.iboot.admin.domain.backup.repository;

import com.iboot.admin.domain.backup.model.RestoreRecord;

import java.util.List;
import java.util.Optional;

/**
 * 恢复演练记录仓储。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface RestoreRecordRepository {

    /**
     * 插入演练记录并回填 ID。
     *
     * @param record 演练记录
     * @return 已回填 ID 的记录
     */
    RestoreRecord insert(RestoreRecord record);

    /**
     * 更新演练记录。
     *
     * @param record 演练记录
     * @return 更新成功返回 {@code true}
     */
    boolean update(RestoreRecord record);

    /**
     * 按 ID 查询演练记录。
     *
     * @param id 记录 ID
     * @return 演练记录，不存在返回空
     */
    Optional<RestoreRecord> findById(Long id);

    /**
     * 查询当前运行中的演练记录。
     *
     * @return 运行中的记录，不存在返回空
     */
    Optional<RestoreRecord> findRunning();

    /**
     * 分页查询演练记录。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @param offset   起始偏移
     * @param limit    每页数量
     * @return 演练记录列表
     */
    List<RestoreRecord> findPage(Long backupId, int offset, int limit);

    /**
     * 统计演练记录数。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @return 记录数
     */
    long count(Long backupId);
}
