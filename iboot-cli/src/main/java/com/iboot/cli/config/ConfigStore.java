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

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * CLI 上下文配置的读写入口。
 *
 * <p>配置文件不存在时返回默认开发上下文；写入走原子替换。配置只保存地址与客户端
 * 标识，不保存任何令牌，令牌由 {@link CredentialStore} 单独管理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ConfigStore {

    private final Path configFile;

    private final JsonMapper mapper = JsonMapper.builder().build();

    /**
     * 使用默认配置路径创建存储。
     */
    public ConfigStore() {
        this(CliPaths.configFile());
    }

    /**
     * 使用指定路径创建存储，便于测试。
     *
     * @param configFile 配置文件路径
     */
    public ConfigStore(Path configFile) {
        this.configFile = configFile;
    }

    /**
     * 读取配置；文件缺失时返回默认配置。
     *
     * @return 配置
     * @throws CliException 文件存在但无法解析时抛出
     */
    public CliConfig load() {
        if (!Files.exists(configFile)) {
            return CliConfig.defaults();
        }
        try {
            CliConfig config = mapper.readValue(Files.readString(configFile), CliConfig.class);
            return config == null ? CliConfig.defaults() : config;
        } catch (IOException | RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "无法读取 CLI 配置：" + configFile, e);
        }
    }

    /**
     * 保存配置。
     *
     * @param config 配置
     */
    public void save(CliConfig config) {
        try {
            AtomicFiles.writeSecret(configFile, mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(config));
        } catch (IOException | RuntimeException e) {
            throw new CliException(ExitCode.BUSINESS, "无法写入 CLI 配置：" + configFile, e);
        }
    }

    /**
     * 获取当前上下文。
     *
     * @return 当前上下文
     */
    public Context current() {
        return load().current();
    }

    /**
     * 列出全部上下文。
     *
     * @return 上下文列表
     */
    public List<Context> contexts() {
        return load().contexts();
    }

    /**
     * 切换当前上下文。
     *
     * @param name 上下文名称
     * @return 切换后的上下文
     */
    public Context use(String name) {
        CliConfig config = load();
        CliConfig updated;
        try {
            updated = config.withCurrent(name);
        } catch (IllegalArgumentException e) {
            throw new CliException(ExitCode.USAGE, e.getMessage());
        }
        save(updated);
        return updated.current();
    }

    /**
     * 新增或替换上下文。
     *
     * @param context 上下文
     * @param select 是否设为当前上下文
     */
    public void put(Context context, boolean select) {
        save(load().withContext(context, select));
    }

    /**
     * 删除指定上下文。
     *
     * @param name 上下文名称
     * @throws CliException 上下文不存在或为当前上下文时抛出
     */
    public void remove(String name) {
        CliConfig updated;
        try {
            updated = load().withoutContext(name);
        } catch (IllegalArgumentException e) {
            throw new CliException(ExitCode.USAGE, e.getMessage());
        }
        save(updated);
    }
}
