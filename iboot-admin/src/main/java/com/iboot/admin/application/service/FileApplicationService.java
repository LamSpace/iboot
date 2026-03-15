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

package com.iboot.admin.application.service;

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.domain.system.repository.FileRepository;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.storage.LocalFileStorageStrategy;
import com.iboot.admin.infrastructure.storage.StorageObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件管理应用服务
 * <p>
 * 负责文件上传、下载、删除、更新和查询等业务逻辑处理。
 * 支持多种存储策略（本地存储/MinIO），通过 FileStorageStrategy 接口实现存储方式切换。
 * 文件大小和允许的类型通过 sys_config 参数配置动态读取，支持自动推断文件分类。
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileApplicationService {

    private final FileRepository fileRepository;
    private final ConfigApplicationService configApplicationService;
    private final FileStorageStrategy fileStorageStrategy;
    private final LocalFileStorageStrategy localFileStorageStrategy;

    private static final String CONFIG_KEY_MAX_SIZE = "sys.file.maxSize";
    private static final String CONFIG_KEY_ALLOWED_EXTENSIONS = "sys.file.allowedExtensions";

    private static final long DEFAULT_MAX_SIZE_MB = 50;

    /**
     * 上传文件
     * <p>
     * 校验文件大小和类型，生成唯一存储路径，使用存储策略保存文件，
     * 自动推断文件分类（如果未指定），最后保存文件信息到数据库。
     * 存储路径格式：yyyy/MM/dd/UUID.ext
     * </p>
     *
     * @param file 上传的文件
     * @param fileCategory 文件分类，为空则自动推断
     * @param uploadBy 上传人
     * @return 文件信息实体
     * @throws BusinessException 当文件为空、超过大小限制或类型不允许时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public FileInfo uploadFile(MultipartFile file, String fileCategory, String uploadBy) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成存储路径
        String originalFilename = file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = datePath + "/" + storedFileName;

        // 3. 使用存储策略保存文件
        try {
            fileStorageStrategy.save(file, relativePath);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }

        // 4. 自动推断文件分类（如果未指定）
        if (fileCategory == null || fileCategory.isEmpty()) {
            fileCategory = inferFileCategory(ext);
        }

        // 5. 保存文件信息到数据库
        FileInfo fileInfo = FileInfo.builder()
                .fileName(originalFilename)
                .filePath(relativePath)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileExt(ext != null ? ext.toLowerCase() : "")
                .fileCategory(fileCategory)
                .uploadBy(uploadBy)
                .uploadTime(LocalDateTime.now())
                .createBy(uploadBy)
                .createTime(LocalDateTime.now())
                .build();

        log.info("文件上传成功：{} -> {} (存储类型：{})",
                originalFilename, relativePath, fileStorageStrategy.getStorageType());

        return fileRepository.save(fileInfo);
    }

    /**
     * 根据 ID 获取文件信息
     *
     * @param id 文件 ID
     * @return 文件信息实体
     * @throws BusinessException 当文件不存在时抛出
     */
    public FileInfo getFileById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }

    /**
     * 获取文件下载 URL
     * <p>
     * 对于 MinIO 存储返回预签名 URL，对于本地存储返回相对路径。
     * </p>
     *
     * @param fileId 文件 ID
     * @return 文件下载 URL
     * @throws BusinessException 当文件不存在时抛出
     */
    public String getFileDownloadUrl(Long fileId) {
        FileInfo fileInfo = getFileById(fileId);
        return fileStorageStrategy.getFileUrl(fileInfo.getFilePath());
    }

    /**
     * 获取文件流（用于下载）
     * <p>
     * 通过存储策略获取文件输入流，支持各种存储方式。
     * </p>
     *
     * @param fileId 文件 ID
     * @return 存储对象，包含输入流和文件名
     * @throws IOException 当读取文件失败时抛出
     * @throws BusinessException 当文件不存在时抛出
     */
    public StorageObject getFileStream(Long fileId) throws IOException {
        FileInfo fileInfo = getFileById(fileId);
        return fileStorageStrategy.getFileStream(fileInfo.getFilePath());
    }

    /**
     * 获取文件的磁盘完整路径（仅用于本地存储的兼容）
     *
     * @param fileInfo 文件信息实体
     * @return 文件的完整磁盘路径
     */
    public Path getFileDiskPath(FileInfo fileInfo) {
        return localFileStorageStrategy.getFullPath(fileInfo.getFilePath());
    }

    /**
     * 获取当前存储类型
     *
     * @return 存储类型标识，如 "local" 或 "minio"
     */
    public String getStorageType() {
        return fileStorageStrategy.getStorageType();
    }

    /**
     * 检查是否为 MinIO 存储模式
     *
     * @return 是否为 MinIO 存储
     */
    public boolean isMinioStorage() {
        return "minio".equals(fileStorageStrategy.getStorageType());
    }

    /**
     * 删除文件
     * <p>
     * 逻辑删除数据库记录，并使用存储策略删除物理文件。
     * 物理文件删除失败时记录警告日志，不影响数据库删除结果。
     * </p>
     *
     * @param id 文件 ID
     * @return 是否删除成功（数据库记录）
     * @throws BusinessException 当文件不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long id) {
        FileInfo fileInfo = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));

        // 逻辑删除数据库记录
        boolean result = fileRepository.deleteById(id);

        // 使用存储策略删除文件
        if (result) {
            boolean deleted = fileStorageStrategy.delete(fileInfo.getFilePath());
            if (!deleted) {
                log.warn("删除存储文件失败：{}", fileInfo.getFilePath());
            }
        }

        return result;
    }

    /**
     * 更新文件信息（文件名、分类、备注）
     * <p>
     * 仅更新文件的元数据信息，不修改物理文件。
     * </p>
     *
     * @param fileInfo 文件信息实体
     * @return 是否更新成功
     * @throws BusinessException 当文件不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFileInfo(FileInfo fileInfo) {
        fileRepository.findById(fileInfo.getId())
                .orElseThrow(() -> new BusinessException("文件不存在"));
        fileInfo.setUpdateTime(LocalDateTime.now());
        return fileRepository.update(fileInfo);
    }

    /**
     * 分页获取文件列表
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 文件列表
     */
    public List<FileInfo> getFilePage(int pageNum, int pageSize) {
        return fileRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计文件总数
     *
     * @return 文件总数
     */
    public long countFiles() {
        return fileRepository.count();
    }

    /**
     * 按条件分页获取文件
     * <p>
     * 支持按文件名、文件分类和扩展名进行条件查询。
     * </p>
     *
     * @param fileName 文件名（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt 文件扩展名（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 文件列表
     */
    public List<FileInfo> getFilePageByCondition(String fileName, String fileCategory, String fileExt, int pageNum, int pageSize) {
        return fileRepository.findPageByCondition(fileName, fileCategory, fileExt, pageNum, pageSize);
    }

    /**
     * 按条件统计文件总数
     * <p>
     * 支持按文件名、文件分类和扩展名进行条件统计。
     * </p>
     *
     * @param fileName 文件名（可选）
     * @param fileCategory 文件分类（可选）
     * @param fileExt 文件扩展名（可选）
     * @return 文件总数
     */
    public long countFilesByCondition(String fileName, String fileCategory, String fileExt) {
        return fileRepository.countByCondition(fileName, fileCategory, fileExt);
    }

    /**
     * 获取所有文件（导出用）
     * <p>
     * 不分页获取所有文件，用于数据导出。
     * </p>
     *
     * @return 文件列表
     */
    public List<FileInfo> getAllFiles() {
        return fileRepository.findAll();
    }

    /**
     * 按条件获取所有文件（导出用）
     * <p>
     * 不分页获取所有符合条件的文件，用于数据导出。
     * </p>
     *
     * @param fileName 文件名（可选）
     * @param fileCategory 文件分类（可选）
     * @return 文件列表
     */
    public List<FileInfo> getAllFilesByCondition(String fileName, String fileCategory) {
        return fileRepository.findAllByCondition(fileName, fileCategory);
    }

    /**
     * 校验文件
     * <p>
     * 检查文件是否为空、大小是否超限、扩展名是否允许。
     * </p>
     *
     * @param file 上传的文件
     * @throws BusinessException 当文件校验失败时抛出
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件大小
        long maxSizeMB = getMaxFileSize();
        long maxSizeBytes = maxSizeMB * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException("文件大小超过限制，最大允许 " + maxSizeMB + "MB");
        }

        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(originalFilename);
        if (ext == null || ext.isEmpty()) {
            throw new BusinessException("无法识别文件类型");
        }

        Set<String> allowedExtensions = getAllowedExtensions();
        if (!allowedExtensions.isEmpty() && !allowedExtensions.contains(ext.toLowerCase())) {
            throw new BusinessException("不允许上传该类型文件：" + ext);
        }
    }

    /**
     * 从系统配置获取最大文件大小（MB）
     * <p>
     * 配置不存在或格式错误时使用默认值 50MB。
     * </p>
     *
     * @return 最大文件大小（MB）
     */
    private long getMaxFileSize() {
        String maxSize = configApplicationService.getConfigValue(CONFIG_KEY_MAX_SIZE);
        if (maxSize != null) {
            try {
                return Long.parseLong(maxSize.trim());
            } catch (NumberFormatException e) {
                log.warn("系统配置 {} 的值无效：{}", CONFIG_KEY_MAX_SIZE, maxSize);
            }
        }
        return DEFAULT_MAX_SIZE_MB;
    }

    /**
     * 从系统配置获取允许的文件扩展名
     * <p>
     * 配置不存在时返回空集合，表示不限制。
     * </p>
     *
     * @return 允许的扩展名集合
     */
    private Set<String> getAllowedExtensions() {
        String extensions = configApplicationService.getConfigValue(CONFIG_KEY_ALLOWED_EXTENSIONS);
        if (extensions != null && !extensions.isEmpty()) {
            return Arrays.stream(extensions.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 根据文件扩展名推断文件分类
     * <p>
     * 支持的分类包括：document（文档）、image（图片）、video（视频）、
     * audio（音频）、archive（压缩包）、other（其他）。
     * </p>
     *
     * @param ext 文件扩展名
     * @return 文件分类标识
     */
    private String inferFileCategory(String ext) {
        if (ext == null) {
            return "other";
        }
        ext = ext.toLowerCase();
        if (Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md", "csv").contains(ext)) {
            return "document";
        } else if (Set.of("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico").contains(ext)) {
            return "image";
        } else if (Set.of("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm").contains(ext)) {
            return "video";
        } else if (Set.of("mp3", "wav", "flac", "aac", "ogg", "wma").contains(ext)) {
            return "audio";
        } else if (Set.of("zip", "rar", "7z", "tar", "gz", "bz2").contains(ext)) {
            return "archive";
        }
        return "other";
    }
}
