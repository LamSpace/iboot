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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MinIO配置属性
 * 从sys_config表动态读取MinIO相关配置
 *
 * @author iBoot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioProperties {

    private final ConfigApplicationService configApplicationService;

    private static final String CONFIG_KEY_STORAGE_TYPE = "sys.file.storageType";
    private static final String CONFIG_KEY_ENDPOINT = "sys.file.minio.endpoint";
    private static final String CONFIG_KEY_ACCESS_KEY = "sys.file.minio.accessKey";
    private static final String CONFIG_KEY_SECRET_KEY = "sys.file.minio.secretKey";
    private static final String CONFIG_KEY_BUCKET_NAME = "sys.file.minio.bucketName";
    private static final String CONFIG_KEY_URL_EXPIRE_SECONDS = "sys.file.minio.urlExpireSeconds";
    private static final String CONFIG_KEY_MONITOR_ENABLED = "sys.minio.monitor.enabled";
    private static final String CONFIG_KEY_CONSOLE_URL = "sys.minio.console.url";

    private static final String DEFAULT_STORAGE_TYPE = "local";
    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";
    private static final String DEFAULT_BUCKET_NAME = "iboot-files";
    private static final int DEFAULT_URL_EXPIRE_SECONDS = 3600;

    /**
     * 获取存储类型
     */
    public String getStorageType() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_STORAGE_TYPE);
        return value != null ? value : DEFAULT_STORAGE_TYPE;
    }

    /**
     * 获取MinIO服务端点
     */
    public String getEndpoint() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_ENDPOINT);
        return value != null ? value : DEFAULT_ENDPOINT;
    }

    /**
     * 获取MinIO访问密钥
     */
    public String getAccessKey() {
        return configApplicationService.getConfigValue(CONFIG_KEY_ACCESS_KEY);
    }

    /**
     * 获取MinIO秘密密钥
     */
    public String getSecretKey() {
        return configApplicationService.getConfigValue(CONFIG_KEY_SECRET_KEY);
    }

    /**
     * 获取MinIO存储桶名称
     */
    public String getBucketName() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_BUCKET_NAME);
        return value != null ? value : DEFAULT_BUCKET_NAME;
    }

    /**
     * 获取预签名URL有效期（秒）
     */
    public int getUrlExpireSeconds() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_URL_EXPIRE_SECONDS);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("无效的URL有效期配置: {}, 使用默认值: {}", value, DEFAULT_URL_EXPIRE_SECONDS);
            }
        }
        return DEFAULT_URL_EXPIRE_SECONDS;
    }

    /**
     * 判断是否使用MinIO存储
     */
    public boolean isMinioEnabled() {
        return "minio".equalsIgnoreCase(getStorageType());
    }

    /**
     * 获取MinIO监控是否启用
     */
    public boolean isMonitorEnabled() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_MONITOR_ENABLED);
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 获取MinIO控制台URL
     */
    public String getConsoleUrl() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_CONSOLE_URL);
        return value != null ? value : "http://localhost:9001";
    }
}
