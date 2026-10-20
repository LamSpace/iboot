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
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本地文件存储策略契约测试。
 *
 * <p>使用 JUnit 临时目录作为上传根路径，验证本地实现满足通用存储契约。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see LocalFileStorageStrategy
 */
class LocalFileStorageStrategyContractTest extends FileStorageStrategyContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected FileStorageStrategy createStrategy() {
        ConfigApplicationService configApplicationService = mock(ConfigApplicationService.class);
        when(configApplicationService.getConfigValue("sys.file.uploadPath")).thenReturn(tempDir.toString());
        return new LocalFileStorageStrategy(configApplicationService);
    }

    @Override
    protected boolean expectsObjectStorage() {
        return false;
    }
}
