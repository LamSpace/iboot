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

package com.iboot.system.application.service;

import com.iboot.system.domain.system.model.Config;
import com.iboot.system.domain.system.repository.ConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 参数配置应用服务测试。
 *
 * <p>覆盖分组默认值、分组计数与更新时保留原分组的行为。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ConfigApplicationServiceTest {

    private ConfigRepository repository;

    private ConfigApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConfigRepository.class);
        service = new ConfigApplicationService(repository);
    }

    /**
     * 验证新增配置未指定分组时落到 other。
     */
    @Test
    void createConfigDefaultsToOtherGroup() {
        when(repository.existsByConfigKey("custom.key")).thenReturn(false);
        when(repository.save(any(Config.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Config saved = service.createConfig(Config.builder()
                .configName("自定义配置")
                .configKey("custom.key")
                .configValue("value")
                .configType(0)
                .build());

        assertThat(saved.getConfigGroup()).isEqualTo("other");
    }

    /**
     * 验证新增配置保留显式分组。
     */
    @Test
    void createConfigKeepsExplicitGroup() {
        when(repository.existsByConfigKey("custom.key")).thenReturn(false);
        when(repository.save(any(Config.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Config saved = service.createConfig(Config.builder()
                .configName("自定义配置")
                .configKey("custom.key")
                .configValue("value")
                .configType(0)
                .configGroup("storage")
                .build());

        assertThat(saved.getConfigGroup()).isEqualTo("storage");
    }

    /**
     * 验证分组计数按 configGroup 聚合，缺失分组归入 other。
     */
    @Test
    void groupCountsAggregateByGroup() {
        when(repository.findAll()).thenReturn(List.of(
                Config.builder().configKey("a").configGroup("basic").build(),
                Config.builder().configKey("b").configGroup("basic").build(),
                Config.builder().configKey("c").configGroup("storage").build(),
                Config.builder().configKey("d").build()));

        Map<String, Long> counts = service.getGroupCounts();

        assertThat(counts).containsEntry("basic", 2L)
                .containsEntry("storage", 1L)
                .containsEntry("other", 1L);
    }

    /**
     * 验证更新时未传分组不会由服务层补默认值，交由 Mapper 保持原值。
     */
    @Test
    void updateConfigDoesNotDefaultMissingGroup() {
        Config existing = Config.builder().id(1L).configKey("custom.key").configGroup("basic").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.update(any(Config.class))).thenReturn(true);

        Config update = Config.builder()
                .id(1L)
                .configName("自定义配置")
                .configKey("custom.key")
                .configValue("value")
                .configType(0)
                .build();
        service.updateConfig(update);

        assertThat(update.getConfigGroup()).isNull();
        verify(repository).update(update);
    }
}
