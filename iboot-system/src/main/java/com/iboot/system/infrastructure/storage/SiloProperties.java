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

package com.iboot.system.infrastructure.storage;

import com.iboot.system.application.service.ConfigApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Silo 配置属性。
 *
 * <p>从 {@code sys_config} 动态读取 Silo 端点、凭证、桶名等配置；部署环境可用
 * {@code IBOOT_FILE_*} 环境变量覆盖，环境变量优先于 {@code sys_config}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SiloProperties {

    private static final Logger log = LoggerFactory.getLogger(SiloProperties.class);

    private static final String CONFIG_KEY_STORAGE_TYPE = "sys.file.storageType";

    private static final String CONFIG_KEY_ENDPOINT = "sys.file.silo.endpoint";

    private static final String CONFIG_KEY_ACCESS_KEY = "sys.file.silo.accessKey";

    private static final String CONFIG_KEY_SECRET_KEY = "sys.file.silo.secretKey";

    private static final String CONFIG_KEY_BUCKET_NAME = "sys.file.silo.bucketName";

    private static final String CONFIG_KEY_URL_EXPIRE_SECONDS = "sys.file.silo.urlExpireSeconds";

    private static final String CONFIG_KEY_MONITOR_ENABLED = "sys.silo.monitor.enabled";

    private static final String CONFIG_KEY_CONSOLE_URL = "sys.silo.console.url";

    private static final String DEFAULT_STORAGE_TYPE = "local";

    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";

    private static final String DEFAULT_BUCKET_NAME = "iboot-files";

    private static final int DEFAULT_URL_EXPIRE_SECONDS = 3600;

    private final ConfigApplicationService configApplicationService;

    /** 部署环境覆盖的存储类型，非空时优先于 sys_config。 */
    @Value("${iboot.file.storage-type:}")
    private String storageTypeOverride;

    /** 部署环境覆盖的内网 Silo 端点，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.endpoint:}")
    private String endpointOverride;

    /** 部署环境覆盖的访问密钥，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.access-key:}")
    private String accessKeyOverride;

    /** 部署环境覆盖的秘密密钥，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.secret-key:}")
    private String secretKeyOverride;

    /** 部署环境覆盖的桶名，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.bucket:}")
    private String bucketOverride;

    @SuppressWarnings("all")
    public SiloProperties(final ConfigApplicationService configApplicationService) {
        this.configApplicationService = configApplicationService;
    }

    /**
     * 获取存储类型
     */
    public String getStorageType() {
        if (hasText(storageTypeOverride)) {
            return storageTypeOverride;
        }
        String value = configApplicationService.getConfigValue(CONFIG_KEY_STORAGE_TYPE);
        return value != null ? value : DEFAULT_STORAGE_TYPE;
    }

    /**
     * 获取Silo服务端点
     */
    public String getEndpoint() {
        if (hasText(endpointOverride)) {
            return endpointOverride;
        }
        String value = configApplicationService.getConfigValue(CONFIG_KEY_ENDPOINT);
        return value != null ? value : DEFAULT_ENDPOINT;
    }

    /**
     * 获取Silo访问密钥
     */
    public String getAccessKey() {
        if (hasText(accessKeyOverride)) {
            return accessKeyOverride;
        }
        return configApplicationService.getConfigValue(CONFIG_KEY_ACCESS_KEY);
    }

    /**
     * 获取Silo秘密密钥
     */
    public String getSecretKey() {
        if (hasText(secretKeyOverride)) {
            return secretKeyOverride;
        }
        return configApplicationService.getConfigValue(CONFIG_KEY_SECRET_KEY);
    }

    /**
     * 获取Silo存储桶名称
     */
    public String getBucketName() {
        if (hasText(bucketOverride)) {
            return bucketOverride;
        }
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
     * 判断是否使用Silo存储
     */
    public boolean isSiloEnabled() {
        return "silo".equalsIgnoreCase(getStorageType());
    }

    /**
     * 获取Silo监控是否启用
     */
    public boolean isMonitorEnabled() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_MONITOR_ENABLED);
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 获取Silo控制台URL
     */
    public String getConsoleUrl() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_CONSOLE_URL);
        return value != null ? value : "http://localhost:9001";
    }

    /**
     * 判断覆盖值是否可用。
     *
     * @param value 环境变量注入的覆盖值
     * @return 非空白时返回 {@code true}
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
