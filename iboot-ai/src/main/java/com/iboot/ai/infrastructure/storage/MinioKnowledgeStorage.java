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

package com.iboot.ai.infrastructure.storage;

import com.iboot.ai.domain.knowledge.KnowledgeStorage;
import com.iboot.ai.infrastructure.config.RagProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 MinIO 的知识库原文件存储实现。
 *
 * <p>不隐式创建 bucket；bucket 缺失时在首次读写时抛出明确错误。仅在知识库能力启用时装配。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioKnowledgeStorage implements KnowledgeStorage {

    private final MinioClient client;

    private final String bucket;

    /**
     * 创建存储实现。
     *
     * @param properties 知识库配置
     */
    public MinioKnowledgeStorage(RagProperties properties) {
        RagProperties.Storage storage = properties.getStorage();
        this.bucket = storage.getBucket();
        this.client = MinioClient.builder()
                .endpoint(storage.getEndpoint())
                .credentials(storage.getAccessKey(), storage.getSecretKey())
                .build();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("写入 MinIO 失败：" + key, e);
        }
    }

    @Override
    public byte[] get(String key) {
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("读取 MinIO 失败：" + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("删除 MinIO 对象失败：" + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("生成 MinIO 下载地址失败：" + key, e);
        }
    }

    /**
     * 检查 bucket 是否存在。
     *
     * @return 存在时返回 {@code true}
     */
    public boolean bucketExists() {
        try {
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        } catch (Exception e) {
            throw new IllegalStateException("检查 MinIO bucket 失败：" + bucket, e);
        }
    }
}
