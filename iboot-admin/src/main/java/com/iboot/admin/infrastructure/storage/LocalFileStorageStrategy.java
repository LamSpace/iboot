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

package com.iboot.admin.infrastructure.storage;

import com.iboot.admin.application.service.ConfigApplicationService;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件存储策略实现
 * 将文件存储到本地文件系统
 *
 * @author iBoot
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFileStorageStrategy implements FileStorageStrategy {

    private final ConfigApplicationService configApplicationService;

    private static final String CONFIG_KEY_UPLOAD_PATH = "sys.file.uploadPath";
    private static final String DEFAULT_UPLOAD_PATH = "./uploads";

    @Override
    public String save(MultipartFile file, String relativePath) throws IOException {
        Path uploadPath = getUploadBasePath();
        Path targetFile = uploadPath.resolve(relativePath);

        // 创建目录
        Files.createDirectories(targetFile.getParent());

        // 保存文件
        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

        log.debug("文件已保存到本地: {}", targetFile);
        return relativePath;
    }

    @Override
    public String getFileUrl(String filePath) {
        // 本地存储返回相对路径，由Controller处理实际下载
        return filePath;
    }

    @Override
    public StorageObject getFileStream(String filePath) throws IOException {
        Path fullPath = getUploadBasePath().resolve(filePath);

        if (!Files.exists(fullPath)) {
            throw new BusinessException("文件不存在: " + filePath);
        }

        InputStream inputStream = Files.newInputStream(fullPath);
        long size = Files.size(fullPath);
        String contentType = Files.probeContentType(fullPath);

        return StorageObject.builder()
                .inputStream(inputStream)
                .size(size)
                .contentType(contentType)
                .fileName(fullPath.getFileName().toString())
                .build();
    }

    @Override
    public boolean delete(String filePath) {
        try {
            Path fullPath = getUploadBasePath().resolve(filePath);
            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                log.debug("本地文件已删除: {}", fullPath);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("删除本地文件失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String filePath) {
        Path fullPath = getUploadBasePath().resolve(filePath);
        return Files.exists(fullPath);
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    /**
     * 获取上传文件的基础路径
     */
    private Path getUploadBasePath() {
        String path = configApplicationService.getConfigValue(CONFIG_KEY_UPLOAD_PATH);
        return Paths.get(path != null ? path : DEFAULT_UPLOAD_PATH);
    }

    /**
     * 获取文件的完整磁盘路径
     */
    public Path getFullPath(String filePath) {
        return getUploadBasePath().resolve(filePath);
    }
}
