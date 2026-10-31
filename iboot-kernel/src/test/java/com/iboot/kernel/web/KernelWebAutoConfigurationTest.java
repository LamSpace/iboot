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

package com.iboot.kernel.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KernelWebAutoConfiguration} 的注册测试。
 *
 * <p>宿主服务的组件扫描覆盖不到内核包，内核组件只能经自动配置注册。漏注册不会让应用启动
 * 失败，只会让异常处理与国际化静默失效，因此这里把「注册确实发生」单独作为断言。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class KernelWebAutoConfigurationTest {

    /**
     * 只加载内核自动配置的上下文，模拟宿主只加依赖、不改启动类的场景。
     */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KernelWebAutoConfiguration.class))
            .withBean(MessageSource.class, StaticMessageSource::new);

    /**
     * 自动配置应注册全局异常处理器与国际化工具。
     */
    @Test
    void registersGlobalExceptionHandlerAndI18nUtil() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context).hasSingleBean(I18nUtil.class);
        });
    }

    /**
     * 服务可通过配置关闭内核异常处理器，以保持既有的错误响应契约。
     */
    @Test
    void exceptionHandlerCanBeDisabled() {
        runner.withPropertyValues("iboot.kernel.exception-handler.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class));
    }

    /**
     * 成功响应应序列化为含 code / message / data / timestamp 四个字段的结构。
     */
    @Test
    void resultSerializesWithUnifiedEnvelope() {
        String json = JsonMapper.builder().build().writeValueAsString(Result.success("hello", null));

        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"message\":\"hello\"");
        assertThat(json).contains("\"data\":null");
        assertThat(json).contains("\"timestamp\":");
    }
}
