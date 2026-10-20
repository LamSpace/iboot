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

import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件存储策略行为契约。
 *
 * <p>所有实现（本地、MinIO、OSS、内存）都必须通过同一组用例，以保证存储介质可替换而行为一致。
 * 子类负责提供具体实现与对象存储语义标识。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
abstract class FileStorageStrategyContractTest {

    private static final String CONTENT_TYPE = "text/plain";

    /**
     * 创建待测存储策略。
     *
     * @return 存储策略实例
     */
    protected abstract FileStorageStrategy createStrategy();

    /**
     * 当前实现是否属于对象存储语义。
     *
     * @return 对象存储返回 {@code true}
     */
    protected abstract boolean expectsObjectStorage();

    @Test
    void saveThenExists() throws IOException {
        FileStorageStrategy strategy = createStrategy();
        String path = uniquePath();

        strategy.save(content("hello".getBytes(StandardCharsets.UTF_8)), path);

        assertTrue(strategy.exists(path));
    }

    @Test
    void readReturnsSavedContentAndMetadata() throws IOException {
        FileStorageStrategy strategy = createStrategy();
        String path = uniquePath();
        byte[] expected = "hello iboot storage".getBytes(StandardCharsets.UTF_8);

        strategy.save(content(expected), path);
        StorageObject stored = strategy.getFileStream(path);

        assertEquals(expected.length, stored.getSize());
        assertEquals(CONTENT_TYPE, stored.getContentType());
        assertArrayEquals(expected, readAll(stored));
    }

    @Test
    void overwriteReplacesPriorContent() throws IOException {
        FileStorageStrategy strategy = createStrategy();
        String path = uniquePath();
        byte[] second = "second content".getBytes(StandardCharsets.UTF_8);

        strategy.save(content("first content".getBytes(StandardCharsets.UTF_8)), path);
        strategy.save(content(second), path);

        assertArrayEquals(second, readAll(strategy.getFileStream(path)));
    }

    @Test
    void deleteExistingThenMissing() throws IOException {
        FileStorageStrategy strategy = createStrategy();
        String path = uniquePath();
        strategy.save(content("to be deleted".getBytes(StandardCharsets.UTF_8)), path);

        assertTrue(strategy.delete(path));
        assertFalse(strategy.exists(path));
        assertFalse(strategy.delete(path));
    }

    @Test
    void fileUrlIsNotBlank() throws IOException {
        FileStorageStrategy strategy = createStrategy();
        String path = uniquePath();
        strategy.save(content("url content".getBytes(StandardCharsets.UTF_8)), path);

        String url = strategy.getFileUrl(path);

        assertNotNull(url);
        assertFalse(url.isBlank());
    }

    @Test
    void objectStorageFlagMatchesProvider() {
        assertEquals(expectsObjectStorage(), createStrategy().isObjectStorage());
    }

    /**
     * 生成当前用例独占的存储路径，避免实现之间或用例之间互相干扰。
     *
     * @return 形如 {@code contract/<uuid>.txt} 的相对路径
     */
    protected String uniquePath() {
        return "contract/" + UUID.randomUUID() + ".txt";
    }

    /**
     * 构造待保存内容。
     *
     * @param bytes 文件字节
     * @return 带固定 content-type 的存储对象
     */
    protected StorageObject content(byte[] bytes) {
        return StorageObject.builder()
                .inputStream(new ByteArrayInputStream(bytes))
                .size((long) bytes.length)
                .contentType(CONTENT_TYPE)
                .fileName("contract.txt")
                .build();
    }

    /**
     * 读完存储对象并关闭流。
     *
     * @param stored 读取结果
     * @return 文件字节
     * @throws IOException 读取失败时抛出
     */
    protected byte[] readAll(StorageObject stored) throws IOException {
        try (InputStream inputStream = stored.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}
