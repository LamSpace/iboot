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

package com.iboot.admin.domain.system.service;

import com.iboot.admin.infrastructure.storage.StorageObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件存储策略接口 定义文件存储的核心操作，支持本地存储和对象存储（如MinIO）
 *
 * @author iBoot
 */
public interface FileStorageStrategy {

    /**
     * 保存文件
     *
     * @param file         上传的文件
     * @param relativePath 相对路径（如 2024/02/27/uuid.jpg）
     *
     * @return 存储后的文件路径
     *
     * @throws IOException 文件保存失败时抛出
     */
    String save(MultipartFile file, String relativePath) throws IOException;

    /**
     * 获取文件访问URL 本地存储返回下载接口路径，对象存储返回预签名URL
     *
     * @param filePath 文件路径
     *
     * @return 文件访问URL
     */
    String getFileUrl(String filePath);

    /**
     * 获取文件流（用于下载）
     *
     * @param filePath 文件路径
     *
     * @return 存储对象（包含输入流和元数据）
     *
     * @throws IOException 文件读取失败时抛出
     */
    StorageObject getFileStream(String filePath) throws IOException;

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     *
     * @return 是否删除成功
     */
    boolean delete(String filePath);

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     *
     * @return 文件是否存在
     */
    boolean exists(String filePath);

    /**
     * 获取存储类型标识
     *
     * @return 存储类型（如 "local" 或 "minio"）
     */
    String getStorageType();

}
