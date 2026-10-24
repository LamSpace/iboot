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

import com.iboot.admin.domain.backup.model.BackupRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 备份记录仓储。
 *
 * <p>提供记录写入、分页查询、运行中记录检测与超时重置。列表无数据时返回空列表，
 * 按 ID 查询返回 {@link Optional}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface BackupRecordRepository {

    /**
     * 插入备份记录并回填 ID。
     *
     * @param record 备份记录
     * @return 已回填 ID 的记录
     */
    BackupRecord insert(BackupRecord record);

    /**
     * 更新备份记录。
     *
     * @param record 备份记录
     * @return 更新成功返回 {@code true}
     */
    boolean update(BackupRecord record);

    /**
     * 按 ID 查询备份记录。
     *
     * @param id 记录 ID
     * @return 备份记录，不存在返回空
     */
    Optional<BackupRecord> findById(Long id);

    /**
     * 查询当前运行中的备份记录。
     *
     * @return 运行中的记录，不存在返回空
     */
    Optional<BackupRecord> findRunning();

    /**
     * 分页查询备份记录。
     *
     * @param status 状态筛选，可为 {@code null}
     * @param offset 起始偏移
     * @param limit  每页数量
     * @return 备份记录列表
     */
    List<BackupRecord> findPage(String status, int offset, int limit);

    /**
     * 统计备份记录数。
     *
     * @param status 状态筛选，可为 {@code null}
     * @return 记录数
     */
    long count(String status);

    /**
     * 将超时仍处于运行中的记录重置为失败。
     *
     * @param threshold 开始时间阈值
     * @param now       当前时间
     * @return 受影响行数
     */
    int resetStaleRunning(LocalDateTime threshold, LocalDateTime now);

    /**
     * 统计全部备份记录数。
     *
     * @return 记录数
     */
    long countAll();

    /**
     * 统计全部备份产物的密文总大小。
     *
     * @return 总字节数
     */
    long sumCiphertextSize();

    /**
     * 查询最近一次成功备份的结束时间。
     *
     * @return 结束时间，从未成功时为 {@code null}
     */
    LocalDateTime lastSuccessAt();
}
