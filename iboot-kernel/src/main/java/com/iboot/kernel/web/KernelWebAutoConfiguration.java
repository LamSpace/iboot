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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

/**
 * 共享内核的 Web 层自动配置。
 *
 * <p>宿主服务（包名通常是 {@code com.iboot.admin} 或 {@code com.iboot.file}）的组件扫描
 * 覆盖不到 {@code com.iboot.kernel}，因此内核组件必须经此自动配置注册；漏注册的后果是
 * 过滤器与异常处理器静默失效，而不是启动失败。
 *
 * <p>每个 Bean 都带 {@link ConditionalOnMissingBean}，宿主可以按需覆盖默认实现。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@AutoConfiguration
public class KernelWebAutoConfiguration {

    /**
     * 注册全局异常处理器。
     *
     * <p>{@link GlobalExceptionHandler} 带 {@code @RestControllerAdvice}，但它的包不在宿主的
     * 组件扫描路径下，必须在这里显式注册，否则业务异常会以 500 原始堆栈返回。
     *
     * <p>需要保持既有异常契约不变的服务（例如已有自有 advice 的 AI 服务）可用
     * {@code iboot.kernel.exception-handler.enabled=false} 关闭。
     *
     * @return 全局异常处理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "iboot.kernel.exception-handler.enabled", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册国际化消息工具。
     *
     * <p>{@link I18nUtil} 通过构造器把 {@link MessageSource} 保存为静态引用，供无法注入依赖的
     * 代码调用；缺少这个 Bean 时消息码不会被解析，接口会直接返回消息码本身。
     *
     * @param messageSource Spring 消息源
     * @return 国际化消息工具
     */
    @Bean
    @ConditionalOnMissingBean
    public I18nUtil i18nUtil(MessageSource messageSource) {
        return new I18nUtil(messageSource);
    }
}
