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

package com.iboot.system.interfaces.controller;

import com.iboot.system.application.service.ConfigApplicationService;
import com.iboot.system.application.service.DictApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 参数配置控制器测试。
 *
 * <p>覆盖配置查询接口的环境变量优先级与数据库回退。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ConfigControllerTest {

    private ConfigApplicationService configApplicationService;

    private MockEnvironment environment;

    private ConfigController controller;

    @BeforeEach
    void setUp() {
        configApplicationService = mock(ConfigApplicationService.class);
        environment = new MockEnvironment();
        controller = new ConfigController(configApplicationService, mock(DictApplicationService.class), environment);
    }

    /**
     * 验证同名环境变量优先于数据库配置值。
     */
    @Test
    void getByKeyPrefersEnvironmentOverride() {
        environment.setProperty("SYS_NACOS_CONSOLE_URL", "http://localhost:8849");

        assertThat(controller.getByKey("sys.nacos.console.url").getData())
                .isEqualTo("http://localhost:8849");
        verify(configApplicationService, never()).getConfigValue(anyString());
    }

    /**
     * 验证环境变量未设置时回退到数据库配置值。
     */
    @Test
    void getByKeyFallsBackToConfigService() {
        when(configApplicationService.getConfigValue("sys.nacos.console.embed")).thenReturn("true");

        assertThat(controller.getByKey("sys.nacos.console.embed").getData()).isEqualTo("true");
    }
}
