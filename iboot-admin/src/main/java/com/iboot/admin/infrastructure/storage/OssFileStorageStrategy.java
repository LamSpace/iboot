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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.iboot.kernel.web.BusinessException;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * 阿里云 OSS 存储策略。
 *
 * <p>把文件保存到阿里云 OSS，并返回预签名访问 URL。启动时校验必需配置并检查
 * bucket 是否存在；不会自动创建 bucket。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see FileStorageStrategy
 * @see OssProperties
 */
public class OssFileStorageStrategy implements FileStorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(OssFileStorageStrategy.class);

    private static final String NO_SUCH_KEY = "NoSuchKey";

    private final OssProperties ossProperties;

    private OSS ossClient;

    @SuppressWarnings("all")
    public OssFileStorageStrategy(final OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    /**
     * 创建使用指定客户端实例的策略，供单元测试注入 mock 客户端。
     *
     * @param ossProperties OSS 配置
     * @param ossClient     OSS 客户端
     */
    OssFileStorageStrategy(final OssProperties ossProperties, final OSS ossClient) {
        this.ossProperties = ossProperties;
        this.ossClient = ossClient;
    }

    /**
     * 初始化 OSS 客户端并校验配置。
     *
     * <p>配置缺失或 bucket 不存在时抛出 {@link IllegalStateException}，阻止应用在
     * 错误配置下启动；不会创建 bucket。
     */
    @PostConstruct
    public void init() {
        String endpoint = requireConfig(ossProperties.getEndpoint(), "sys.file.oss.endpoint");
        String bucketName = requireConfig(ossProperties.getBucketName(), "sys.file.oss.bucketName");
        String accessKeyId = requireEnv(ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeyIdEnvName());
        String accessKeySecret = requireEnv(ossProperties.getAccessKeySecret(),
                ossProperties.getAccessKeySecretEnvName());
        try {
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            if (!ossClient.doesBucketExist(bucketName)) {
                throw new IllegalStateException("OSS bucket 不存在: " + bucketName);
            }
            log.info("OSS客户端初始化成功，endpoint: {}, bucket: {}", endpoint, bucketName);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("OSS客户端初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    /**
     * 保存文件到 OSS。
     *
     * @param source 待保存的内容与元数据
     * @param relativePath 对象键
     * @return 保存后的对象键
     * @throws IOException 上传失败时抛出
     */
    public String save(StorageObject source, String relativePath) throws IOException {
        if (source == null || source.getSize() == null) {
            throw new IOException("文件大小不能为空");
        }
        String bucketName = ossProperties.getBucketName();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(source.getSize());
        metadata.setContentType(source.getContentType() != null
                ? source.getContentType() : "application/octet-stream");
        try (InputStream inputStream = source.getInputStream()) {
            ossClient.putObject(bucketName, relativePath, inputStream, metadata);
            log.debug("文件已上传到 OSS: bucket={}, object={}, size={}", bucketName, relativePath, source.getSize());
            return relativePath;
        } catch (Exception e) {
            log.error("上传文件到 OSS 失败: bucket={}, object={}", bucketName, relativePath, e);
            throw new IOException("上传文件到 OSS 失败: " + e.getMessage(), e);
        }
    }

    @Override
    /**
     * 获取预签名访问 URL。
     *
     * @param filePath 对象键
     * @return 预签名 URL
     */
    public String getFileUrl(String filePath) {
        try {
            Date expiration = new Date(System.currentTimeMillis()
                    + ossProperties.getUrlExpireSeconds() * 1000L);
            return ossClient.generatePresignedUrl(ossProperties.getBucketName(), filePath, expiration).toString();
        } catch (Exception e) {
            log.error("生成 OSS 预签名 URL 失败: {}", filePath, e);
            throw new BusinessException("生成文件访问URL失败: " + e.getMessage());
        }
    }

    @Override
    /**
     * 获取文件流和元数据。
     *
     * @param filePath 对象键
     * @return 存储对象
     * @throws IOException 读取失败时抛出
     */
    public StorageObject getFileStream(String filePath) throws IOException {
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), filePath);
            ObjectMetadata metadata = ossObject.getObjectMetadata();
            return StorageObject.builder()
                    .inputStream(ossObject.getObjectContent())
                    .size(metadata.getContentLength())
                    .contentType(metadata.getContentType())
                    .fileName(fileName(filePath))
                    .build();
        } catch (OSSException e) {
            if (NO_SUCH_KEY.equals(e.getErrorCode())) {
                throw new BusinessException("文件不存在: " + filePath);
            }
            throw new IOException("获取OSS文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("获取 OSS 文件流失败: {}", filePath, e);
            throw new IOException("获取OSS文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    /**
     * 删除对象。
     *
     * @param filePath 对象键
     * @return 删除成功返回 {@code true}；对象不存在返回 {@code false}
     */
    public boolean delete(String filePath) {
        try {
            if (!exists(filePath)) {
                log.debug("OSS 对象不存在，跳过删除: {}", filePath);
                return false;
            }
            ossClient.deleteObject(ossProperties.getBucketName(), filePath);
            log.debug("OSS 对象已删除: {}", filePath);
            return true;
        } catch (Exception e) {
            log.warn("删除 OSS 对象失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    /**
     * 判断对象是否存在。
     *
     * @param filePath 对象键
     * @return 存在返回 {@code true}
     */
    public boolean exists(String filePath) {
        try {
            return ossClient.doesObjectExist(ossProperties.getBucketName(), filePath);
        } catch (Exception e) {
            log.warn("检查 OSS 对象存在性失败: {}", filePath, e);
            return false;
        }
    }

    @Override
    /**
     * 获取存储类型标识。
     *
     * @return 固定返回 {@code oss}
     */
    public String getStorageType() {
        return "oss";
    }

    /**
     * OSS 属于对象存储语义。
     *
     * @return 固定返回 {@code true}
     */
    @Override
    public boolean isObjectStorage() {
        return true;
    }

    private String requireConfig(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少 OSS 配置: " + key);
        }
        return value;
    }

    private String requireEnv(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少 OSS 环境变量: " + envName);
        }
        return value;
    }

    private String fileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}
