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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性。
 *
 * <p>endpoint、bucket 与预签名有效期从 {@code sys_config} 读取；AccessKeyId 与
 * AccessKeySecret 只从环境变量读取，不落库、不入仓。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class OssProperties {

    private static final Logger log = LoggerFactory.getLogger(OssProperties.class);

    private static final String CONFIG_KEY_ENDPOINT = "sys.file.oss.endpoint";

    private static final String CONFIG_KEY_BUCKET_NAME = "sys.file.oss.bucketName";

    private static final String CONFIG_KEY_URL_EXPIRE_SECONDS = "sys.file.oss.urlExpireSeconds";

    private static final String ENV_ACCESS_KEY_ID = "IBOOT_OSS_ACCESS_KEY_ID";

    private static final String ENV_ACCESS_KEY_SECRET = "IBOOT_OSS_ACCESS_KEY_SECRET";

    private static final int DEFAULT_URL_EXPIRE_SECONDS = 3600;

    private final ConfigApplicationService configApplicationService;

    @SuppressWarnings("all")
    public OssProperties(final ConfigApplicationService configApplicationService) {
        this.configApplicationService = configApplicationService;
    }

    /**
     * 获取 OSS 服务端点。
     *
     * @return 形如 {@code https://oss-cn-hangzhou.aliyuncs.com} 的端点
     */
    public String getEndpoint() {
        return configApplicationService.getConfigValue(CONFIG_KEY_ENDPOINT);
    }

    /**
     * 获取 OSS 存储桶名称。
     *
     * @return 存储桶名称
     */
    public String getBucketName() {
        return configApplicationService.getConfigValue(CONFIG_KEY_BUCKET_NAME);
    }

    /**
     * 获取 OSS AccessKeyId。
     *
     * @return 环境变量 {@code IBOOT_OSS_ACCESS_KEY_ID} 的值，未配置时为 {@code null}
     */
    public String getAccessKeyId() {
        return System.getenv(ENV_ACCESS_KEY_ID);
    }

    /**
     * 获取 OSS AccessKeySecret。
     *
     * @return 环境变量 {@code IBOOT_OSS_ACCESS_KEY_SECRET} 的值，未配置时为 {@code null}
     */
    public String getAccessKeySecret() {
        return System.getenv(ENV_ACCESS_KEY_SECRET);
    }

    /**
     * 获取预签名 URL 有效期（秒）。
     *
     * @return 有效秒数，配置缺失或非法时返回默认 3600
     */
    public int getUrlExpireSeconds() {
        String value = configApplicationService.getConfigValue(CONFIG_KEY_URL_EXPIRE_SECONDS);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                log.warn("无效的 OSS URL 有效期配置: {}, 使用默认值: {}", value, DEFAULT_URL_EXPIRE_SECONDS);
            }
        }
        return DEFAULT_URL_EXPIRE_SECONDS;
    }

    /**
     * 获取密钥环境变量名，用于启动失败时给出可操作的错误信息。
     *
     * @return 固定返回 {@code IBOOT_OSS_ACCESS_KEY_ID}
     */
    public String getAccessKeyIdEnvName() {
        return ENV_ACCESS_KEY_ID;
    }

    /**
     * 获取密钥环境变量名，用于启动失败时给出可操作的错误信息。
     *
     * @return 固定返回 {@code IBOOT_OSS_ACCESS_KEY_SECRET}
     */
    public String getAccessKeySecretEnvName() {
        return ENV_ACCESS_KEY_SECRET;
    }
}
