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

import com.iboot.ai.infrastructure.config.RagProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MinioKnowledgeStorage} 的集成测试。
 *
 * <p>使用 Testcontainers MinIO 验证保存、读取、删除与预签名地址。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class MinioKnowledgeStorageTest {

    private static final int MINIO_PORT = 9000;

    private static final String BUCKET = "iboot-ai-test";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(
            "minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withExposedPorts(MINIO_PORT)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live")
                    .forPort(MINIO_PORT)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static MinioKnowledgeStorage storage;

    @BeforeAll
    static void setUp() throws Exception {
        String endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(MINIO_PORT);
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials("minioadmin", "minioadmin")
                .build();
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
        RagProperties properties = new RagProperties();
        properties.getStorage().setEndpoint(endpoint);
        properties.getStorage().setAccessKey("minioadmin");
        properties.getStorage().setSecretKey("minioadmin");
        properties.getStorage().setBucket(BUCKET);
        storage = new MinioKnowledgeStorage(properties);
    }

    /**
     * 保存后可读、可删除、可生成预签名地址。
     */
    @Test
    void supportsPutGetDeleteAndPresign() {
        String key = "kb/1/a.txt";
        byte[] content = "知识库内容".getBytes(StandardCharsets.UTF_8);

        storage.put(key, content, "text/plain");
        assertThat(storage.get(key)).isEqualTo(content);
        assertThat(storage.presignedGetUrl(key, Duration.ofMinutes(5))).contains(BUCKET).contains(key);

        storage.delete(key);
        assertThat(storage.bucketExists()).isTrue();
    }
}
