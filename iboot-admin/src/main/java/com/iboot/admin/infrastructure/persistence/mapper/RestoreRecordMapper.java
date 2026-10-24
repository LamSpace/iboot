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

import com.iboot.admin.infrastructure.persistence.po.RestoreRecordPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 恢复演练记录 MyBatis Mapper。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface RestoreRecordMapper {

    /**
     * 插入演练记录并回填主键。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int insert(RestoreRecordPO po);

    /**
     * 更新演练记录。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int update(RestoreRecordPO po);

    /**
     * 按 ID 查询。
     *
     * @param id 记录 ID
     * @return 持久化对象，不存在时为 {@code null}
     */
    RestoreRecordPO selectById(@Param("id") Long id);

    /**
     * 查询运行中的记录。
     *
     * @return 持久化对象，不存在时为 {@code null}
     */
    RestoreRecordPO selectRunning();

    /**
     * 分页查询。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @param offset   起始偏移
     * @param limit    每页数量
     * @return 持久化对象列表
     */
    List<RestoreRecordPO> selectPage(@Param("backupId") Long backupId, @Param("offset") int offset,
                                     @Param("limit") int limit);

    /**
     * 统计记录数。
     *
     * @param backupId 备份记录 ID 筛选，可为 {@code null}
     * @return 记录数
     */
    long countPage(@Param("backupId") Long backupId);
}
