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

package com.iboot.monitor.application.service;

import com.iboot.monitor.infrastructure.directory.DictLabelLookup;
import com.iboot.monitor.infrastructure.directory.SysConfigReader;
import com.iboot.monitor.interfaces.dto.response.ServerInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ServerMonitorApplicationService} 的行为测试。
 *
 * <p>覆盖中间件不可达时的降级语义：单个检查项标记为不可用，整个资源检查仍然返回，
 * 不因为中间件故障抛出异常。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ServerMonitorApplicationServiceTest {

    private SysConfigReader configReader;

    private DictLabelLookup dictLabelLookup;

    @BeforeEach
    void setUp() {
        configReader = mock(SysConfigReader.class);
        dictLabelLookup = mock(DictLabelLookup.class);
        when(configReader.getConfigValue(anyString())).thenReturn(null);
        when(dictLabelLookup.labelOf(anyString(), anyString())).thenReturn("不可用");
    }

    /**
     * 数据库与 Redis 均不可达时，两项检查标记为 DOWN，响应仍完整返回。
     */
    @Test
    void middlewareFailureKeepsEndpointSuccessful() throws Exception {
        DataSource brokenDataSource = mock(DataSource.class);
        when(brokenDataSource.getConnection()).thenThrow(new IllegalStateException("db down"));
        RedisConnectionFactory brokenRedis = mock(RedisConnectionFactory.class);
        when(brokenRedis.getConnection()).thenThrow(new IllegalStateException("redis down"));

        ServerMonitorApplicationService service =
                new ServerMonitorApplicationService(configReader, dictLabelLookup, brokenDataSource, brokenRedis);

        ServerInfoResponse response = service.getServerInfo();

        assertThat(response).isNotNull();
        assertThat(response.getServiceChecks()).hasSize(2);
        assertThat(response.getServiceChecks()).allSatisfy(check ->
                assertThat(check.getStatus()).isEqualTo("DOWN"));
        assertThat(response.getServiceChecks()).extracting(ServerInfoResponse.ServiceCheckInfo::getType)
                .containsExactlyInAnyOrder("db", "redis");
    }

    /**
     * 未配置 Spring Boot Admin 地址时回退到默认路径。
     */
    @Test
    void sbaUrlFallsBackToDefault() {
        ServerMonitorApplicationService service =
                new ServerMonitorApplicationService(configReader, dictLabelLookup, mock(DataSource.class),
                        mock(RedisConnectionFactory.class));

        assertThat(service.getServerInfo().getSbaUrl()).isEqualTo("/sba");
    }
}
