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

package com.iboot.admin.infrastructure.config;

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
 * <p>白名单从内核的硬编码数组改成了 {@code application.yml} 中的配置项。这份列表逐条决定
 * 哪些接口可以匿名访问，抄漏或抄错都会造成越权或登录断裂，因此用测试把它钉在值上。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SecurityWhitelistBindingTest {

    /**
     * 迁移前 {@code SecurityWhitelistConfig.WHITE_LIST_PATHS} 的完整内容。
     */
    private static final List<String> LEGACY_WHITELIST = List.of(
            "/api/auth/login", "/api/auth/logout", "/api/auth/register",
            "/api/config/public",
            "/api/i18n/**",
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config", "/swagger-resources/**", "/webjars/**", "/doc.html",
            "/favicon.ico", "/error", "/static/**",
            "/**/*.ico", "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.gif", "/**/*.css", "/**/*.js",
            "/actuator/**", "/druid/**",
            "/sba/**", "/instances/**", "/applications/**");

    /**
     * application.yml 中的白名单必须与迁移前的数组完全一致（顺序无关）。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void whitelistMatchesLegacyArray() throws IOException {
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
