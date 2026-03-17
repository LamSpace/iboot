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
import com.iboot.admin.infrastructure.storage.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 文件存储配置类 根据系统配置动态选择文件存储策略
 *
 * @author iBoot
 */
@Configuration
public class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    private final ConfigApplicationService configApplicationService;

    private final MinioProperties minioProperties;

    @SuppressWarnings("all")
    public FileStorageConfig(final ConfigApplicationService configApplicationService,
                             final MinioProperties minioProperties) {
        this.configApplicationService = configApplicationService;
        this.minioProperties = minioProperties;
    }

    /**
     * 创建文件存储策略Bean 根据配置选择本地存储或MinIO存储
     */
    @Bean
    @Primary
    public FileStorageStrategy fileStorageStrategy() {
        if (minioProperties.isMinioEnabled()) {
            log.info("使用MinIO对象存储策略");
            return new MinioFileStorageStrategy(minioProperties);
        } else {
            log.info("使用本地文件存储策略");
            return new LocalFileStorageStrategy(configApplicationService);
        }
    }

    /**
     * 本地文件存储策略（始终可用，用于兼容旧文件下载）
     */
    @Bean
    public LocalFileStorageStrategy localFileStorageStrategy() {
        return new LocalFileStorageStrategy(configApplicationService);
    }

}
