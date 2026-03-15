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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.infrastructure.persistence.po.FilePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件信息 Mapper 接口
 * <p>
 * 负责文件元数据的数据库操作，支持文件的管理和查询
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface FileMapper {

    /**
     * 插入文件记录
     *
     * @param filePO 文件持久化对象
     * @return 影响的记录数
     */
    int insert(FilePO filePO);

    /**
     * 更新文件记录
     *
     * @param filePO 文件持久化对象
     * @return 影响的记录数
     */
    int update(FilePO filePO);

    /**
     * 根据 ID 删除文件
     *
     * @param id 文件 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询文件
     *
     * @param id 文件 ID
     * @return 文件持久化对象
     */
    FilePO selectById(@Param("id") Long id);

    /**
     * 查询所有文件
     *
     * @return 文件列表
     */
    List<FilePO> selectAll();

    /**
     * 分页查询文件（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文件列表
     */
    @DataScope(deptAlias = "f", userAlias = "f")
    List<FilePO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计文件总数
     *
     * @return 文件总数
     */
    long count();

    /**
     * 按条件分页查询文件（受数据权限控制）
     *
     * @param fileName 文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt 文件扩展名（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文件列表
     */
    @DataScope(deptAlias = "f", userAlias = "f")
    List<FilePO> selectPageByCondition(@Param("fileName") String fileName,
                                       @Param("fileCategory") String fileCategory,
                                       @Param("fileExt") String fileExt,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 按条件统计文件总数
     *
     * @param fileName 文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt 文件扩展名（可选）
     * @return 文件总数
     */
    long countByCondition(@Param("fileName") String fileName,
                          @Param("fileCategory") String fileCategory,
                          @Param("fileExt") String fileExt);

    /**
     * 按条件查询所有文件（导出用）
     *
     * @param fileName 文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @return 文件列表
     */
    @DataScope(deptAlias = "f", userAlias = "f")
    List<FilePO> selectAllByCondition(@Param("fileName") String fileName,
                                      @Param("fileCategory") String fileCategory);
}
