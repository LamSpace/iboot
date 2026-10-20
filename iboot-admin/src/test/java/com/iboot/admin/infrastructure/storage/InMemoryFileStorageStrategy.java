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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.domain.system.service.FileStorageStrategy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存存储策略。
 *
 * <p>仅用于测试：把文件内容保存在进程内 Map，不依赖磁盘或网络。它不属于对象存储语义，
 * 下载时按本地模式输出文件流。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see FileStorageStrategy
 */
public class InMemoryFileStorageStrategy implements FileStorageStrategy {

    private final Map<String, StoredContent> contents = new ConcurrentHashMap<>();

    @Override
    /**
     * 保存文件内容到内存。
     *
     * @param source 待保存的内容与元数据
     * @param relativePath 对象键
     * @return 保存后的对象键
     * @throws IOException 读取输入流失败时抛出
     */
    public String save(StorageObject source, String relativePath) throws IOException {
        try (InputStream inputStream = source.getInputStream()) {
            contents.put(relativePath, new StoredContent(inputStream.readAllBytes(),
                    source.getContentType(), source.getFileName()));
        }
        return relativePath;
    }

    @Override
    /**
     * 获取内存对象 URL。
     *
     * @param filePath 对象键
     * @return 形如 {@code memory://<path>} 的标识
     */
    public String getFileUrl(String filePath) {
        return "memory://" + filePath;
    }

    @Override
    /**
     * 获取文件流和元数据。
     *
     * @param filePath 对象键
     * @return 存储对象
     */
    public StorageObject getFileStream(String filePath) {
        StoredContent content = contents.get(filePath);
        if (content == null) {
            throw new BusinessException("文件不存在: " + filePath);
        }
        return StorageObject.builder()
                .inputStream(new ByteArrayInputStream(content.bytes))
                .size((long) content.bytes.length)
                .contentType(content.contentType)
                .fileName(content.fileName)
                .build();
    }

    @Override
    /**
     * 删除内存对象。
     *
     * @param filePath 对象键
     * @return 删除成功返回 {@code true}；对象不存在返回 {@code false}
     */
    public boolean delete(String filePath) {
        return contents.remove(filePath) != null;
    }

    @Override
    /**
     * 判断对象是否存在。
     *
     * @param filePath 对象键
     * @return 存在返回 {@code true}
     */
    public boolean exists(String filePath) {
        return contents.containsKey(filePath);
    }

    @Override
    /**
     * 获取存储类型标识。
     *
     * @return 固定返回 {@code memory}
     */
    public String getStorageType() {
        return "memory";
    }

    /**
     * 内存实现不属于对象存储语义。
     *
     * @return 固定返回 {@code false}
     */
    @Override
    public boolean isObjectStorage() {
        return false;
    }

    /**
     * 内存中的文件内容与元数据。
     *
     * @param bytes 文件字节
     * @param contentType MIME 类型
     * @param fileName 文件名
     */
    private record StoredContent(byte[] bytes, String contentType, String fileName) {
    }
}
