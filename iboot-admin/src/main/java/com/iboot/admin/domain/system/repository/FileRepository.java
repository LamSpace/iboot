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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.FileInfo;

import java.util.List;
import java.util.Optional;

/**
 * 文件信息仓储接口
 * <p>
 * 负责文件元数据的持久化操作，记录文件存储位置和基本信息
 * </p>
 *
 * @author iBoot
 */
public interface FileRepository {

    /**
     * 保存文件信息
     *
     * @param fileInfo 文件信息实体
     *
     * @return 保存后的文件信息
     */
    FileInfo save(FileInfo fileInfo);

    /**
     * 更新文件信息
     *
     * @param fileInfo 文件信息实体
     *
     * @return 是否更新成功
     */
    boolean update(FileInfo fileInfo);

    /**
     * 根据 ID 删除文件信息（逻辑删除）
     *
     * @param id 文件 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询文件信息
     *
     * @param id 文件 ID
     *
     * @return 文件信息实体，不存在则返回空
     */
    Optional<FileInfo> findById(Long id);

    /**
     * 查询所有文件信息
     *
     * @return 文件信息列表
     */
    List<FileInfo> findAll();

    /**
     * 分页查询文件信息
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 文件信息列表
     */
    List<FileInfo> findPage(int pageNum, int pageSize);

    /**
     * 统计文件总数
     *
     * @return 文件总数
     */
    long count();

    /**
     * 按条件分页查询文件
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt      文件扩展名（可选）
     * @param pageNum      页码
     * @param pageSize     每页数量
     *
     * @return 文件信息列表
     */
    List<FileInfo> findPageByCondition(String fileName, String fileCategory, String fileExt, int pageNum, int pageSize);

    /**
     * 按条件统计文件总数
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt      文件扩展名（可选）
     *
     * @return 文件总数
     */
    long countByCondition(String fileName, String fileCategory, String fileExt);

    /**
     * 按条件查询所有文件（导出用）
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     *
     * @return 文件信息列表
     */
    List<FileInfo> findAllByCondition(String fileName, String fileCategory);

}
