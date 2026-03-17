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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.domain.system.repository.FileRepository;
import com.iboot.admin.infrastructure.persistence.mapper.FileMapper;
import com.iboot.admin.infrastructure.persistence.po.FilePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件信息仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现文件信息数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class FileRepositoryImpl implements FileRepository {

    private final FileMapper fileMapper;

    @SuppressWarnings("all")
    public FileRepositoryImpl(final FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    /**
     * 保存文件信息
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param fileInfo 文件信息实体
     *
     * @return 保存后的文件信息
     */
    @Override
    public FileInfo save(FileInfo fileInfo) {
        FilePO po = convertToPO(fileInfo);
        if (fileInfo.getId() == null) {
            fileMapper.insert(po);
        } else {
            fileMapper.update(po);
        }
        return convertToDomain(po);
    }

    /**
     * 更新文件信息
     *
     * @param fileInfo 文件信息实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(FileInfo fileInfo) {
        FilePO po = convertToPO(fileInfo);
        return fileMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除文件信息（逻辑删除）
     *
     * @param id 文件信息 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return fileMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询文件信息
     *
     * @param id 文件信息 ID
     *
     * @return 文件信息实体，不存在则返回空
     */
    @Override
    public Optional<FileInfo> findById(Long id) {
        FilePO po = fileMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 查询所有文件信息
     *
     * @return 文件信息列表
     */
    @Override
    public List<FileInfo> findAll() {
        return fileMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 分页查询文件信息
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 文件信息列表
     */
    @Override
    public List<FileInfo> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return fileMapper.selectPage(offset, pageSize).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 统计文件信息总数
     *
     * @return 文件信息总数
     */
    @Override
    public long count() {
        return fileMapper.count();
    }

    /**
     * 按条件分页查询文件信息
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt      文件扩展名（可选）
     * @param pageNum      页码
     * @param pageSize     每页数量
     *
     * @return 文件信息列表
     */
    @Override
    public List<FileInfo> findPageByCondition(String fileName, String fileCategory, String fileExt, int pageNum,
                                              int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return fileMapper.selectPageByCondition(fileName, fileCategory, fileExt, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计文件信息总数
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt      文件扩展名（可选）
     *
     * @return 文件信息总数
     */
    @Override
    public long countByCondition(String fileName, String fileCategory, String fileExt) {
        return fileMapper.countByCondition(fileName, fileCategory, fileExt);
    }

    /**
     * 按条件查询所有文件信息（不分页，用于导出）
     *
     * @param fileName     文件名称（可选）
     * @param fileCategory 文件分类（可选）
     *
     * @return 文件信息列表
     */
    @Override
    public List<FileInfo> findAllByCondition(String fileName, String fileCategory) {
        return fileMapper.selectAllByCondition(fileName, fileCategory)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param fileInfo 文件信息领域对象
     *
     * @return 文件信息持久化对象
     */
    private FilePO convertToPO(FileInfo fileInfo) {
        FilePO po = new FilePO();
        po.setId(fileInfo.getId());
        po.setFileName(fileInfo.getFileName());
        po.setFilePath(fileInfo.getFilePath());
        po.setFileSize(fileInfo.getFileSize());
        po.setFileType(fileInfo.getFileType());
        po.setFileExt(fileInfo.getFileExt());
        po.setFileCategory(fileInfo.getFileCategory());
        po.setUploadBy(fileInfo.getUploadBy());
        po.setUploadTime(fileInfo.getUploadTime());
        po.setCreateBy(fileInfo.getCreateBy());
        po.setCreateTime(fileInfo.getCreateTime());
        po.setUpdateBy(fileInfo.getUpdateBy());
        po.setUpdateTime(fileInfo.getUpdateTime());
        po.setDeleted(fileInfo.getDeleted() != null ? fileInfo.getDeleted() : 0);
        po.setRemark(fileInfo.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 文件信息持久化对象
     *
     * @return 文件信息领域对象
     */
    private FileInfo convertToDomain(FilePO po) {
        return FileInfo.builder()
                .id(po.getId())
                .fileName(po.getFileName())
                .filePath(po.getFilePath())
                .fileSize(po.getFileSize())
                .fileType(po.getFileType())
                .fileExt(po.getFileExt())
                .fileCategory(po.getFileCategory())
                .uploadBy(po.getUploadBy())
                .uploadTime(po.getUploadTime())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .deleted(po.getDeleted())
                .remark(po.getRemark())
                .build();
    }

}
