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

package com.iboot.file.infrastructure.storage;

import com.iboot.file.infrastructure.persistence.SysConfigReader;
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

    private final SysConfigReader sysConfigReader;

    /** 部署环境覆盖的存储类型，非空时优先于 sys_config。 */
    @Value("${iboot.file.storage-type:}")
    private String storageTypeOverride;

    /** 部署环境覆盖的内网 Silo 端点，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.endpoint:}")
    private String endpointOverride;

    /** 部署环境覆盖的预签名对外端点，未配置时回退到内网端点。 */
    @Value("${iboot.file.silo.public-endpoint:}")
    private String publicEndpointOverride;

    /** 对象存储区域，显式指定可避免预签名前的区域查询。 */
    @Value("${iboot.file.silo.region:us-east-1}")
    private String region;

    /** 部署环境覆盖的访问密钥，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.access-key:}")
    private String accessKeyOverride;

    /** 部署环境覆盖的秘密密钥，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.secret-key:}")
    private String secretKeyOverride;

    /** 部署环境覆盖的桶名，非空时优先于 sys_config。 */
    @Value("${iboot.file.silo.bucket:}")
    private String bucketOverride;

    /** 是否使用每服务客户端证书向 Silo 换取临时凭据。 */
    @Value("${iboot.file.silo.client-cert-enabled:false}")
    private boolean clientCertEnabled;

    /** 客户端证书 PKCS12 keystore 路径；启用客户端证书时必填。 */
    @Value("${iboot.file.silo.client-cert-keystore:/certs/client.p12}")
    private String clientCertKeystore;

    /** 客户端证书 PKCS12 keystore 口令。 */
    @Value("${iboot.file.silo.client-cert-keystore-password:changeit}")
    private String clientCertKeystorePassword;

    @SuppressWarnings("all")
    public SiloProperties(final SysConfigReader sysConfigReader) {
        this.sysConfigReader = sysConfigReader;
    }

    /**
     * 获取存储类型
     */
    public String getStorageType() {
        if (hasText(storageTypeOverride)) {
            return storageTypeOverride;
        }
        String value = sysConfigReader.getString(CONFIG_KEY_STORAGE_TYPE);
        return value != null ? value : DEFAULT_STORAGE_TYPE;
    }

    /**
     * 获取Silo服务端点
     */
    public String getEndpoint() {
        if (hasText(endpointOverride)) {
            return endpointOverride;
        }
        String value = sysConfigReader.getString(CONFIG_KEY_ENDPOINT);
        return value != null ? value : DEFAULT_ENDPOINT;
    }

    /**
     * 获取预签名使用的对外端点。
     *
     * <p>容器内服务端读写走内网端点，浏览器下载走该对外地址；
     * 未配置时与内网端点一致。
     *
     * @return 浏览器可达的 Silo 地址
     */
    public String getPublicEndpoint() {
        if (hasText(publicEndpointOverride)) {
            return publicEndpointOverride;
        }
        return getEndpoint();
    }

    /**
     * 获取对象存储区域。
     *
     * <p>显式指定区域后，预签名不再向 endpoint 发起 bucket 区域查询；
     * 容器内的浏览器侧地址无法访问，这一步必须避免。
     *
     * @return 区域标识，默认 {@code us-east-1}
     */
    public String getRegion() {
        return hasText(region) ? region : "us-east-1";
    }

    /**
     * 获取Silo访问密钥
     */
    public String getAccessKey() {
        if (hasText(accessKeyOverride)) {
            return accessKeyOverride;
        }
        return sysConfigReader.getString(CONFIG_KEY_ACCESS_KEY);
    }

    /**
     * 获取Silo秘密密钥
     */
    public String getSecretKey() {
        if (hasText(secretKeyOverride)) {
            return secretKeyOverride;
        }
        return sysConfigReader.getString(CONFIG_KEY_SECRET_KEY);
    }

    /**
     * 获取Silo存储桶名称
     */
    public String getBucketName() {
        if (hasText(bucketOverride)) {
            return bucketOverride;
        }
        String value = sysConfigReader.getString(CONFIG_KEY_BUCKET_NAME);
        return value != null ? value : DEFAULT_BUCKET_NAME;
    }

    /**
     * 判断是否启用 Silo 客户端证书认证。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isClientCertEnabled() {
        return clientCertEnabled;
    }

    /**
     * 获取客户端证书 PKCS12 keystore 路径。
     *
     * @return keystore 路径
     */
    public String getClientCertKeystore() {
        return clientCertKeystore;
    }

    /**
     * 获取客户端证书 keystore 口令。
     *
     * @return keystore 口令
     */
    public String getClientCertKeystorePassword() {
        return clientCertKeystorePassword;
    }

    /**
     * 获取预签名URL有效期（秒）
     */
    public int getUrlExpireSeconds() {
        String value = sysConfigReader.getString(CONFIG_KEY_URL_EXPIRE_SECONDS);
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
        String value = sysConfigReader.getString(CONFIG_KEY_MONITOR_ENABLED);
        return "true".equalsIgnoreCase(value);
    }

    /**
     * 获取Silo控制台URL
     */
    public String getConsoleUrl() {
        String value = sysConfigReader.getString(CONFIG_KEY_CONSOLE_URL);
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
