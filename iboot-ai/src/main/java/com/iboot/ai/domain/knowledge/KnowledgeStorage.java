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

package com.iboot.ai.domain.knowledge;

import java.time.Duration;

/**
 * 知识库原文件存储端口。
 *
 * <p>生产实现使用 MinIO，测试使用内存实现；调用方只依赖该接口，不感知具体存储。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface KnowledgeStorage {

    /**
     * 写入对象。
     *
     * @param key         对象路径
     * @param content     内容
     * @param contentType MIME 类型
     */
    void put(String key, byte[] content, String contentType);

    /**
     * 读取对象。
     *
     * @param key 对象路径
     * @return 内容
     */
    byte[] get(String key);

    /**
     * 删除对象。
     *
     * @param key 对象路径
     */
    void delete(String key);

    /**
     * 生成短时效下载地址。
     *
     * @param key 对象路径
     * @param ttl 有效期
     * @return 预签名地址
     */
    String presignedGetUrl(String key, Duration ttl);
}
