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

package com.iboot.file.common.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器静态访问工具。
 *
 * <p>实现 {@link ApplicationContextAware} 并在容器启动时保存 {@link ApplicationContext}，
 * 供非 Spring 管理的对象（例如静态工具、反序列化回调）按名称或类型获取 Bean。
 *
 * <p>业务代码优先使用构造器注入，仅在无法注入时使用本类；容器尚未初始化时
 * 静态方法会因上下文为 {@code null} 而失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 获取当前 Spring 容器上下文。
     *
     * @return 当前 {@link ApplicationContext}；容器尚未初始化时为 {@code null}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 由 Spring 容器回调，保存应用上下文。
     *
     * @param applicationContext 当前容器上下文
     * @throws BeansException 容器回调失败时抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.applicationContext = applicationContext;
    }

    /**
     * 按名称获取 Bean。
     *
     * @param name Bean 名称
     * @param <T> Bean 类型
     * @return 对应的 Bean 实例
     * @throws BeansException 找不到 Bean 或创建失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T) applicationContext.getBean(name);
    }

    /**
     * 按类型获取 Bean。
     *
     * @param clazz Bean 类型
     * @param <T> Bean 类型
     * @return 对应类型的 Bean 实例
     * @throws BeansException 找不到 Bean、存在多个候选或创建失败时抛出
     */
    public static <T> T getBean(Class<T> clazz) throws BeansException {
        return applicationContext.getBean(clazz);
    }

    /**
     * 按名称和类型获取 Bean。
     *
     * @param name Bean 名称
     * @param clazz Bean 类型
     * @param <T> Bean 类型
     * @return 对应的 Bean 实例
     * @throws BeansException 找不到 Bean、类型不匹配或创建失败时抛出
     */
    public static <T> T getBean(String name, Class<T> clazz) throws BeansException {
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 判断容器中是否存在指定名称的 Bean。
     *
     * @param name Bean 名称
     * @return 存在返回 {@code true}，否则返回 {@code false}
     */
    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    /**
     * 判断指定 Bean 是否为单例。
     *
     * @param name Bean 名称
     * @return 单例返回 {@code true}，否则返回 {@code false}
     * @throws NoSuchBeanDefinitionException Bean 不存在时抛出
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return applicationContext.isSingleton(name);
    }

    /**
     * 获取 Bean 的类型。
     *
     * @param name Bean 名称
     * @return Bean 的类型
     * @throws NoSuchBeanDefinitionException Bean 不存在时抛出
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return applicationContext.getType(name);
    }

    /**
     * 获取 Bean 的所有别名。
     *
     * @param name Bean 名称
     * @return 别名数组；没有别名时返回空数组
     * @throws NoSuchBeanDefinitionException Bean 不存在时抛出
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
        return applicationContext.getAliases(name);
    }

}
