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

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Silo 预签名客户端测试。
 *
 * <p>验证读写客户端使用内网端点、预签名客户端使用对外端点，以及端点相同时复用客户端。
 * 客户端显式指定区域，预签名不需要访问任何端点即可完成。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see SiloFileStorageStrategy
 */
class SiloFileStorageStrategyTest {

    @Test
    void presignedUrlUsesPublicEndpoint() {
        SiloFileStorageStrategy strategy = strategyFor("http://silo:9000", "http://localhost:9000");

        strategy.initClients();

        assertThat(strategy.getFileUrl("docs/report.pdf"))
                .startsWith("http://localhost:9000/iboot-files/docs/report.pdf");
    }

    @Test
    void presignedUrlUsesInternalEndpointWhenPublicEndpointMatches() {
        SiloFileStorageStrategy strategy = strategyFor("http://silo:9000", "http://silo:9000");

        strategy.initClients();

        assertThat(strategy.getFileUrl("docs/report.pdf"))
                .startsWith("http://silo:9000/iboot-files/docs/report.pdf");
        assertThat(ReflectionTestUtils.getField(strategy, "presignClient"))
                .isSameAs(ReflectionTestUtils.getField(strategy, "siloClient"));
    }

    @Test
    void createsSeparateClientWhenEndpointsDiffer() {
        SiloFileStorageStrategy strategy = strategyFor("http://silo:9000", "http://localhost:9000");

        strategy.initClients();

        assertThat(ReflectionTestUtils.getField(strategy, "presignClient"))
                .isNotSameAs(ReflectionTestUtils.getField(strategy, "siloClient"));
    }

    private SiloFileStorageStrategy strategyFor(String internalEndpoint, String publicEndpoint) {
        SiloProperties properties = mock(SiloProperties.class);
        when(properties.getEndpoint()).thenReturn(internalEndpoint);
        when(properties.getPublicEndpoint()).thenReturn(publicEndpoint);
        when(properties.getRegion()).thenReturn("us-east-1");
        when(properties.getAccessKey()).thenReturn("minioadmin");
        when(properties.getSecretKey()).thenReturn("minioadmin");
        when(properties.getBucketName()).thenReturn("iboot-files");
        when(properties.getUrlExpireSeconds()).thenReturn(3600);
        return new SiloFileStorageStrategy(properties);
    }
}
