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

package com.iboot.file.infrastructure.config;

import com.iboot.file.infrastructure.persistence.SysConfigReader;
import com.iboot.file.domain.system.service.FileStorageStrategy;
import com.iboot.file.infrastructure.storage.LocalFileStorageStrategy;
import com.iboot.file.infrastructure.storage.MinioFileStorageStrategy;
import com.iboot.file.infrastructure.storage.MinioProperties;
import com.iboot.file.infrastructure.storage.OssFileStorageStrategy;
import com.iboot.file.infrastructure.storage.OssProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Locale;

/**
 * 文件存储配置。
 *
 * <p>按 {@code sys.file.storageType} 在启动期选择唯一生效的存储策略：
 * {@code local}、{@code minio} 或 {@code oss}；缺省为 {@code local}，非法值直接
 * 阻止应用启动。切换提供方需要重启应用。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.file.domain.system.service.FileStorageStrategy
 */
@Configuration
public class FileStorageConfig {

    private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

    private static final String CONFIG_KEY_STORAGE_TYPE = "sys.file.storageType";

    private static final String STORAGE_TYPE_LOCAL = "local";

    private static final String STORAGE_TYPE_MINIO = "minio";

    private static final String STORAGE_TYPE_OSS = "oss";

    private final SysConfigReader sysConfigReader;

    private final MinioProperties minioProperties;

    private final OssProperties ossProperties;

    @SuppressWarnings("all")
    public FileStorageConfig(final SysConfigReader sysConfigReader,
                             final MinioProperties minioProperties, final OssProperties ossProperties) {
        this.sysConfigReader = sysConfigReader;
        this.minioProperties = minioProperties;
        this.ossProperties = ossProperties;
    }

    /**
     * 创建唯一生效的文件存储策略。
     *
     * @return 配置指定的存储策略
     * @throws IllegalStateException 配置了不支持的存储类型时抛出
     */
    @Bean
    @Primary
    public FileStorageStrategy fileStorageStrategy() {
        String storageType = sysConfigReader.getString(CONFIG_KEY_STORAGE_TYPE);
        if (storageType == null || storageType.isBlank()) {
            storageType = STORAGE_TYPE_LOCAL;
        }
        return switch (storageType.toLowerCase(Locale.ROOT)) {
            case STORAGE_TYPE_LOCAL -> {
                log.info("使用本地文件存储策略");
                yield new LocalFileStorageStrategy(sysConfigReader);
            }
            case STORAGE_TYPE_MINIO -> {
                log.info("使用 MinIO 对象存储策略");
                yield new MinioFileStorageStrategy(minioProperties);
            }
            case STORAGE_TYPE_OSS -> {
                log.info("使用阿里云 OSS 对象存储策略");
                yield new OssFileStorageStrategy(ossProperties);
            }
            default -> throw new IllegalStateException("不支持的存储类型: " + storageType
                    + "，可选值: local、minio、oss");
        };
    }

}
