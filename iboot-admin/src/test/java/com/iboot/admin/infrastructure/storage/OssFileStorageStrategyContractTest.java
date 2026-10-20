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
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 阿里云 OSS 真实环境契约测试。
 *
 * <p>仅在设置 {@code IBOOT_OSS_ACCESS_KEY_ID} 时启用，且需要额外的
 * {@code IBOOT_OSS_ACCESS_KEY_SECRET}、{@code IBOOT_OSS_TEST_ENDPOINT}、
 * {@code IBOOT_OSS_TEST_BUCKET}。默认通过 surefire 的 {@code cloud} 分组排除。
 * 测试结束后只删除本次用例创建的契约对象。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see OssFileStorageStrategy
 */
@Tag("cloud")
@EnabledIfEnvironmentVariable(named = "IBOOT_OSS_ACCESS_KEY_ID", matches = ".+")
class OssFileStorageStrategyContractTest extends FileStorageStrategyContractTest {

    private static final String ENV_ACCESS_KEY_ID = "IBOOT_OSS_ACCESS_KEY_ID";

    private static final String ENV_ACCESS_KEY_SECRET = "IBOOT_OSS_ACCESS_KEY_SECRET";

    private static final String ENV_ENDPOINT = "IBOOT_OSS_TEST_ENDPOINT";

    private static final String ENV_BUCKET = "IBOOT_OSS_TEST_BUCKET";

    private static final Set<String> CREATED_PATHS = ConcurrentHashMap.newKeySet();

    @Override
    protected FileStorageStrategy createStrategy() {
        OssProperties properties = mock(OssProperties.class);
        when(properties.getEndpoint()).thenReturn(System.getenv(ENV_ENDPOINT));
        when(properties.getBucketName()).thenReturn(System.getenv(ENV_BUCKET));
        when(properties.getAccessKeyId()).thenReturn(System.getenv(ENV_ACCESS_KEY_ID));
        when(properties.getAccessKeySecret()).thenReturn(System.getenv(ENV_ACCESS_KEY_SECRET));
        when(properties.getAccessKeyIdEnvName()).thenReturn(ENV_ACCESS_KEY_ID);
        when(properties.getAccessKeySecretEnvName()).thenReturn(ENV_ACCESS_KEY_SECRET);
        when(properties.getUrlExpireSeconds()).thenReturn(3600);
        OssFileStorageStrategy strategy = new OssFileStorageStrategy(properties);
        strategy.init();
        return strategy;
    }

    @Override
    protected boolean expectsObjectStorage() {
        return true;
    }

    @Override
    protected String uniquePath() {
        String path = super.uniquePath();
        CREATED_PATHS.add(path);
        return path;
    }

    /**
     * 清理本次测试创建的对象，避免在测试 bucket 中留下垃圾数据。
     */
    @AfterAll
    static void deleteCreatedObjects() {
        if (CREATED_PATHS.isEmpty()) {
            return;
        }
        OSS ossClient = new OSSClientBuilder().build(
                System.getenv(ENV_ENDPOINT),
                System.getenv(ENV_ACCESS_KEY_ID),
                System.getenv(ENV_ACCESS_KEY_SECRET));
        try {
            int deleted = 0;
            for (String path : CREATED_PATHS) {
                ossClient.deleteObject(System.getenv(ENV_BUCKET), path);
                deleted++;
            }
            System.out.println("deleted_oss_contract_objects=" + deleted);
        } finally {
            ossClient.shutdown();
        }
    }
}
