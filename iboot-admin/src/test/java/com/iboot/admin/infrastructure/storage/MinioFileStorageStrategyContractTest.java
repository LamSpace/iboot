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

import com.iboot.admin.domain.system.service.FileStorageStrategy;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MinIO 存储策略契约测试。
 *
 * <p>使用 Testcontainers 启动真实 MinIO 容器，验证对象存储实现满足通用契约。
 * 运行环境必须提供 Docker，缺少 Docker 时测试失败而不是跳过。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MinioFileStorageStrategy
 */
@Testcontainers
class MinioFileStorageStrategyContractTest extends FileStorageStrategyContractTest {

    private static final String ACCESS_KEY = "minioadmin";

    private static final String SECRET_KEY = "minioadmin";

    private static final String BUCKET = "iboot-test";

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(
            "minio/minio:RELEASE.2025-09-07T16-13-09Z")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server /data");

    @Override
    protected FileStorageStrategy createStrategy() {
        MinioProperties properties = mock(MinioProperties.class);
        when(properties.getEndpoint()).thenReturn(endpoint());
        when(properties.getAccessKey()).thenReturn(ACCESS_KEY);
        when(properties.getSecretKey()).thenReturn(SECRET_KEY);
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(properties.getUrlExpireSeconds()).thenReturn(3600);
        MinioFileStorageStrategy strategy = new MinioFileStorageStrategy(properties);
        strategy.init();
        return strategy;
    }

    @Override
    protected boolean expectsObjectStorage() {
        return true;
    }

    @Test
    void containerIsReachable() {
        assertTrue(MINIO.isRunning());
    }

    private String endpoint() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }
}
