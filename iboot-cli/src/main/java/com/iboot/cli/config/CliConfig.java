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

package com.iboot.cli.config;

import java.util.List;
import java.util.Optional;

/**
 * CLI 的持久化配置。
 *
 * <p>保存全部 context 与当前选中的 context。配置文件缺失时由 {@link ConfigStore}
 * 返回 {@link #defaults()}，不会在读取阶段隐式写盘。
 *
 * @param contexts 上下文列表
 * @param currentContext 当前上下文名称
 * @author iBoot Team
 * @since 1.0.0
 */
public record CliConfig(List<Context> contexts, String currentContext) {

    /**
     * 创建并规范化配置。
     */
    public CliConfig {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
        if (contexts.isEmpty()) {
            Context fallback = Context.defaults();
            contexts = List.of(fallback);
            currentContext = fallback.name();
        }
        String selected = currentContext;
        if (selected == null || selected.isBlank()
                || contexts.stream().noneMatch(context -> context.name().equals(selected))) {
            currentContext = contexts.getFirst().name();
        }
    }

    /**
     * 默认配置。
     *
     * @return 仅包含开发上下文的配置
     */
    public static CliConfig defaults() {
        Context context = Context.defaults();
        return new CliConfig(List.of(context), context.name());
    }

    /**
     * 查找当前上下文。
     *
     * @return 当前上下文
     */
    public Context current() {
        return find(currentContext).orElseGet(() -> contexts.getFirst());
    }

    /**
     * 按名称查找上下文。
     *
     * @param name 上下文名称
     * @return 匹配的上下文
     */
    public Optional<Context> find(String name) {
        return contexts.stream().filter(context -> context.name().equals(name)).findFirst();
    }

    /**
     * 切换当前上下文。
     *
     * @param name 目标上下文名称
     * @return 切换后的配置
     * @throws IllegalArgumentException 上下文不存在时抛出
     */
    public CliConfig withCurrent(String name) {
        if (find(name).isEmpty()) {
            throw new IllegalArgumentException("context 不存在：" + name);
        }
        return new CliConfig(contexts, name);
    }

    /**
     * 新增或替换上下文。
     *
     * @param context 上下文
     * @param select 是否同时设为当前上下文
     * @return 更新后的配置
     */
    public CliConfig withContext(Context context, boolean select) {
        List<Context> updated = contexts.stream()
                .filter(existing -> !existing.name().equals(context.name()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        updated.add(context);
        return new CliConfig(updated, select ? context.name() : currentContext);
    }

    /**
     * 删除指定上下文。
     *
     * @param name 上下文名称
     * @return 删除后的配置
     * @throws IllegalArgumentException 上下文不存在或为当前上下文时抛出
     */
    public CliConfig withoutContext(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("context 名称不能为空");
        }
        if (name.equals(currentContext)) {
            throw new IllegalArgumentException("不能删除当前 context：" + name + "，请先切换到其他 context");
        }
        if (find(name).isEmpty()) {
            throw new IllegalArgumentException("context 不存在：" + name);
        }
        List<Context> remaining = contexts.stream()
                .filter(context -> !context.name().equals(name))
                .toList();
        return new CliConfig(remaining, currentContext);
    }
}
