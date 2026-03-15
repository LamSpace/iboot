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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储策略实现
 * 将文件存储到MinIO对象存储服务
 *
 * @author iBoot
 */
@Slf4j
@RequiredArgsConstructor
public class MinioFileStorageStrategy implements FileStorageStrategy {

    private final MinioProperties minioProperties;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            this.minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();

            ensureBucketExists();
            log.info("MinIO客户端初始化成功，endpoint: {}, bucket: {}",
                    minioProperties.getEndpoint(), minioProperties.getBucketName());
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败", e);
            throw new BusinessException("MinIO客户端初始化失败: " + e.getMessage());
        }
    }

    /**
     * 确保存储桶存在，不存在则创建
     */
    private void ensureBucketExists() {
        try {
            String bucketName = minioProperties.getBucketName();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("MinIO存储桶已创建: {}", bucketName);
            }
        } catch (Exception e) {
            throw new BusinessException("检查/创建MinIO存储桶失败: " + e.getMessage());
        }
    }

    @Override
    public String save(MultipartFile file, String relativePath) throws IOException {
        try {
            String bucketName = minioProperties.getBucketName();
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            long fileSize = file.getSize();
            log.info("准备上传文件到 MinIO: bucket={}, object={}, size={}, contentType={}",
                bucketName, relativePath, fileSize, contentType);

            // 验证文件大小
            if (fileSize <= 0) {
                log.error("文件大小为 0 或负数，无法上传：bucket={}, object={}, size={}",
                    bucketName, relativePath, fileSize);
                throw new IOException("文件大小为 0 或负数，无法上传");
            }

            // 获取输入流
            InputStream inputStream = file.getInputStream();

            // 包装为带缓冲的流，确保数据完整读取
            java.io.BufferedInputStream bufferedStream = new java.io.BufferedInputStream(inputStream);

            // 上传文件（流会在上传完成后关闭）
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(relativePath)
                    .stream(bufferedStream, fileSize, -1)
                    .contentType(contentType)
                    .build());

            // 关闭流
            bufferedStream.close();
            inputStream.close();

            log.info("文件上传成功：bucket={}, object={}, size={}", bucketName, relativePath, fileSize);

            // 验证上传是否成功
            boolean exists = exists(relativePath);
            if (!exists) {
                log.error("文件上传后验证失败：bucket={}, object={}", bucketName, relativePath);
                throw new IOException("文件上传后验证失败");
            }

            // 验证文件大小
            try {
                var stat = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(relativePath)
                        .build());
                log.info("文件验证成功：bucket={}, object={}, statSize={}", bucketName, relativePath, stat.size());
            } catch (Exception e) {
                log.warn("文件验证失败：bucket={}, object={}, error={}", bucketName, relativePath, e.getMessage());
            }

            return relativePath;
        } catch (Exception e) {
            log.error("上传文件到 MinIO 失败：{}, relativePath={}", e.getMessage(), relativePath, e);
            throw new IOException("上传文件到 MinIO 失败：" + e.getMessage(), e);
        }
    }


    @Override
    public String getFileUrl(String filePath) {
        try {
            int expireSeconds = minioProperties.getUrlExpireSeconds();
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucketName())
                    .object(filePath)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build());

            log.debug("生成MinIO预签名URL: {}, 有效期: {}秒", filePath, expireSeconds);
            return url;
        } catch (Exception e) {
            log.error("生成MinIO预签名URL失败: {}", filePath, e);
            throw new BusinessException("生成文件访问URL失败: " + e.getMessage());
        }
    }

    @Override
    public StorageObject getFileStream(String filePath) throws IOException {
        try {
            String bucketName = minioProperties.getBucketName();

            // 获取对象信息
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build());

            // 获取对象流
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filePath)
                    .build());

            return StorageObject.builder()
                    .inputStream(inputStream)
                    .size(stat.size())
                    .contentType(stat.contentType())
                    .fileName(filePath.substring(filePath.lastIndexOf('/') + 1))
                    .build();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BusinessException("文件不存在: " + filePath);
            }
            throw new IOException("获取MinIO文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("获取MinIO文件流失败: {}", filePath, e);
            throw new IOException("获取MinIO文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(filePath)
                    .build());

            log.debug("MinIO文件已删除: {}", filePath);
            return true;
        } catch (Exception e) {
            log.warn("删除MinIO文件失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String filePath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(filePath)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            log.warn("检查MinIO文件存在性失败: {}", filePath, e);
            return false;
        } catch (Exception e) {
            log.warn("检查MinIO文件存在性失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
}
