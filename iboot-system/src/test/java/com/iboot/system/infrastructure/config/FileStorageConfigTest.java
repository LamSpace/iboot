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

package com.iboot.system.infrastructure.config;

import com.iboot.system.application.service.ConfigApplicationService;
import com.iboot.system.domain.system.service.FileStorageStrategy;
import com.iboot.system.infrastructure.storage.LocalFileStorageStrategy;
import com.iboot.system.infrastructure.storage.SiloFileStorageStrategy;
import com.iboot.system.infrastructure.storage.OssFileStorageStrategy;
import com.iboot.system.infrastructure.storage.OssProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 存储提供方选择逻辑测试。
 *
 * <p>验证 {@code sys.file.storageType} 的四个取值分支：local、silo、旧值 minio 与非法值，
 * 以及 {@code IBOOT_FILE_STORAGE_TYPE} 对数据库值的覆盖优先级。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see FileStorageConfig
 */
class FileStorageConfigTest {

    @Test
    void selectsLocalStrategyByDefault() {
        assertInstanceOf(LocalFileStorageStrategy.class, strategyFor(null));
    }

    @Test
    void selectsSiloStrategy() {
        assertInstanceOf(SiloFileStorageStrategy.class, strategyFor("silo"));
    }

    @Test
    void selectsOssStrategy() {
        assertInstanceOf(OssFileStorageStrategy.class, strategyFor("oss"));
    }

    @Test
    void rejectsUnknownStorageType() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> strategyFor("s3"));

        assertTrue(error.getMessage().contains("s3"));
    }

    @Test
    void rejectsLegacyMinioStorageType() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> strategyFor("minio"));

        assertTrue(error.getMessage().contains("minio"));
    }

    @Test
    void environmentStorageTypeOverridesDatabase() {
        assertInstanceOf(SiloFileStorageStrategy.class, strategyFor("local", "silo"));
    }

    @Test
    void environmentLocalOverridesSiloDefault() {
        assertInstanceOf(LocalFileStorageStrategy.class, strategyFor("silo", "local"));
    }

    @Test
    void rejectsInvalidEnvironmentStorageType() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> strategyFor("silo", "s3"));

        assertTrue(error.getMessage().contains("s3"));
    }

    private FileStorageStrategy strategyFor(String storageType) {
        return strategyFor(storageType, null);
    }

    private FileStorageStrategy strategyFor(String storageType, String environmentStorageType) {
        ConfigApplicationService configApplicationService = mock(ConfigApplicationService.class);
        when(configApplicationService.getConfigValue("sys.file.storageType")).thenReturn(storageType);
        com.iboot.system.infrastructure.storage.SiloProperties siloProperties =
                new com.iboot.system.infrastructure.storage.SiloProperties(configApplicationService);
        if (environmentStorageType != null) {
            ReflectionTestUtils.setField(siloProperties, "storageTypeOverride", environmentStorageType);
        }
        FileStorageConfig config = new FileStorageConfig(
                configApplicationService, siloProperties, new OssProperties(configApplicationService));
        return config.fileStorageStrategy();
    }
}
