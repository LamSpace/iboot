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

package com.iboot.admin.infrastructure.config;

import com.iboot.admin.application.service.ConfigApplicationService;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.storage.LocalFileStorageStrategy;
import com.iboot.admin.infrastructure.storage.MinioFileStorageStrategy;
import com.iboot.admin.infrastructure.storage.OssFileStorageStrategy;
import com.iboot.admin.infrastructure.storage.OssProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 存储提供方选择逻辑测试。
 *
 * <p>验证 {@code sys.file.storageType} 的四个取值分支：local、minio、oss 与非法值。
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
    void selectsMinioStrategy() {
        assertInstanceOf(MinioFileStorageStrategy.class, strategyFor("minio"));
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

    private FileStorageStrategy strategyFor(String storageType) {
        ConfigApplicationService configApplicationService = mock(ConfigApplicationService.class);
        when(configApplicationService.getConfigValue("sys.file.storageType")).thenReturn(storageType);
        FileStorageConfig config = new FileStorageConfig(
                configApplicationService,
                new com.iboot.admin.infrastructure.storage.MinioProperties(configApplicationService),
                new OssProperties(configApplicationService));
        return config.fileStorageStrategy();
    }
}
