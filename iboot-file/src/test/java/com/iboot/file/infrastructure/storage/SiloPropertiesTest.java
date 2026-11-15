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
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Silo 配置解析测试。
 *
 * <p>验证环境变量覆盖、数据库兜底与默认值的优先级，以及预签名对外端点的回退规则。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see SiloProperties
 */
class SiloPropertiesTest {

    private final SysConfigReader sysConfigReader = mock(SysConfigReader.class);

    private final SiloProperties properties = new SiloProperties(sysConfigReader);

    @Test
    void environmentEndpointOverridesDatabase() {
        when(sysConfigReader.getString("sys.file.silo.endpoint")).thenReturn("http://db:9000");
        setField("endpointOverride", "http://env:9000");

        assertThat(properties.getEndpoint()).isEqualTo("http://env:9000");
    }

    @Test
    void databaseEndpointIsUsedWhenEnvironmentIsBlank() {
        when(sysConfigReader.getString("sys.file.silo.endpoint")).thenReturn("http://db:9000");
        setField("endpointOverride", " ");

        assertThat(properties.getEndpoint()).isEqualTo("http://db:9000");
    }

    @Test
    void defaultEndpointIsUsedWhenBothAreMissing() {
        assertThat(properties.getEndpoint()).isEqualTo("http://localhost:9000");
    }

    @Test
    void publicEndpointDefaultsToInternalEndpoint() {
        when(sysConfigReader.getString("sys.file.silo.endpoint")).thenReturn("http://db:9000");

        assertThat(properties.getPublicEndpoint()).isEqualTo("http://db:9000");
    }

    @Test
    void publicEndpointOverrideIsUsedForPresigning() {
        setField("endpointOverride", "http://internal:9000");
        setField("publicEndpointOverride", "https://files.example.com");

        assertThat(properties.getPublicEndpoint()).isEqualTo("https://files.example.com");
    }

    @Test
    void regionDefaultsToUsEastOne() {
        assertThat(properties.getRegion()).isEqualTo("us-east-1");
    }

    @Test
    void environmentRegionOverridesDefault() {
        setField("region", "eu-west-1");

        assertThat(properties.getRegion()).isEqualTo("eu-west-1");
    }

    @Test
    void environmentCredentialsAndBucketOverrideDatabase() {
        when(sysConfigReader.getString("sys.file.silo.accessKey")).thenReturn("db-key");
        when(sysConfigReader.getString("sys.file.silo.secretKey")).thenReturn("db-secret");
        when(sysConfigReader.getString("sys.file.silo.bucketName")).thenReturn("db-bucket");
        setField("accessKeyOverride", "env-key");
        setField("secretKeyOverride", "env-secret");
        setField("bucketOverride", "env-bucket");

        assertThat(properties.getAccessKey()).isEqualTo("env-key");
        assertThat(properties.getSecretKey()).isEqualTo("env-secret");
        assertThat(properties.getBucketName()).isEqualTo("env-bucket");
    }

    @Test
    void environmentStorageTypeOverridesDatabase() {
        when(sysConfigReader.getString("sys.file.storageType")).thenReturn("local");
        setField("storageTypeOverride", "silo");

        assertThat(properties.getStorageType()).isEqualTo("silo");
        assertThat(properties.isSiloEnabled()).isTrue();
    }

    private void setField(String name, Object value) {
        ReflectionTestUtils.setField(properties, name, value);
    }
}
