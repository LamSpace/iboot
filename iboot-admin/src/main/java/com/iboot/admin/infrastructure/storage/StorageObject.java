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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * 存储对象DTO
 * 封装从存储系统获取的文件流和元数据
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageObject {

    /**
     * 文件输入流
     */
    private InputStream inputStream;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * MIME类型（如 image/png）
     */
    private String contentType;

    /**
     * 文件名
     */
    private String fileName;
}
