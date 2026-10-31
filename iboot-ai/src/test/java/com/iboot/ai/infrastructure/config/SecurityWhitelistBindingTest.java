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

package com.iboot.ai.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 白名单配置绑定测试。
 *
 * <p>AI 服务只允许健康检查与指标端点匿名访问；这份列表从内核硬编码改为配置项后必须保持不变，
 * 否则会把业务接口暴露成免鉴权路径。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SecurityWhitelistBindingTest {

    /**
     * 迁移前 {@code com.iboot.ai.infrastructure.security.SecurityWhitelistConfig} 的完整内容。
     */
    private static final List<String> LEGACY_WHITELIST = List.of(
            "/actuator/health",
            "/actuator/prometheus");

    /**
     * application.yml 中的白名单必须与迁移前的集合完全一致（顺序无关）。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void whitelistMatchesLegacySet() throws IOException {
        assertThat(loadWhitelist("application.yml"))
                .containsExactlyInAnyOrderElementsOf(LEGACY_WHITELIST);
    }

    private List<String> loadWhitelist(String resource) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource(resource));
        assertThat(sources).isNotEmpty();
        return new Binder(ConfigurationPropertySources.from(sources))
                .bind("iboot.security.whitelist", Bindable.listOf(String.class))
                .orElse(List.of());
    }
}
