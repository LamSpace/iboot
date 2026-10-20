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

/**
 * 内存存储策略契约测试。
 *
 * <p>内存实现只用于测试，必须与本地、MinIO、OSS 实现满足同一套行为契约。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see InMemoryFileStorageStrategy
 */
class InMemoryFileStorageStrategyContractTest extends FileStorageStrategyContractTest {

    @Override
    protected FileStorageStrategy createStrategy() {
        return new InMemoryFileStorageStrategy();
    }

    @Override
    protected boolean expectsObjectStorage() {
        return false;
    }
}
