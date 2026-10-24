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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.infrastructure.persistence.po.BackupRecordPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份记录 MyBatis Mapper。
 *
 * <p>只负责单表读写，不包含业务规则。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface BackupRecordMapper {

    /**
     * 插入备份记录并回填主键。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int insert(BackupRecordPO po);

    /**
     * 更新备份记录。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int update(BackupRecordPO po);

    /**
     * 按 ID 查询。
     *
     * @param id 记录 ID
     * @return 持久化对象，不存在时为 {@code null}
     */
    BackupRecordPO selectById(@Param("id") Long id);

    /**
     * 查询运行中的记录。
     *
     * @return 持久化对象，不存在时为 {@code null}
     */
    BackupRecordPO selectRunning();

    /**
     * 分页查询。
     *
     * @param status 状态筛选，可为 {@code null}
     * @param offset 起始偏移
     * @param limit  每页数量
     * @return 持久化对象列表
     */
    List<BackupRecordPO> selectPage(@Param("status") String status, @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计记录数。
     *
     * @param status 状态筛选，可为 {@code null}
     * @return 记录数
     */
    long countPage(@Param("status") String status);

    /**
     * 重置超时运行中记录。
     *
     * @param threshold 开始时间阈值
     * @param now       当前时间
     * @return 受影响行数
     */
    int resetStaleRunning(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    /**
     * 统计全部记录数。
     *
     * @return 记录数
     */
    long countAll();

    /**
     * 统计密文总大小。
     *
     * @return 总字节数
     */
    long sumCiphertextSize();

    /**
     * 查询最近成功备份的结束时间。
     *
     * @return 结束时间，可能为 {@code null}
     */
    LocalDateTime lastSuccessAt();
}
