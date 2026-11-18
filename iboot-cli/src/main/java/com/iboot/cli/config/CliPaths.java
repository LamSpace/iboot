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

import java.nio.file.Path;

/**
 * CLI 配置与凭据路径。
 *
 * <p>按优先级解析：{@code IBOOT_CONFIG_DIR} → {@code XDG_CONFIG_HOME/iboot} →
 * {@code ~/.config/iboot}。测试通过构造器传入临时目录，不依赖真实用户目录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class CliPaths {

    private CliPaths() {
    }

    /**
     * 解析配置目录。
     *
     * @return 配置目录
     */
    public static Path configDir() {
        String override = System.getenv("IBOOT_CONFIG_DIR");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, "iboot");
        }
        return Path.of(System.getProperty("user.home"), ".config", "iboot");
    }

    /**
     * 配置文件路径。
     *
     * @return {@code config.json} 路径
     */
    public static Path configFile() {
        return configDir().resolve("config.json");
    }

    /**
     * 凭据文件路径。
     *
     * @return {@code credentials.json} 路径
     */
    public static Path credentialsFile() {
        return configDir().resolve("credentials.json");
    }

    /**
     * 凭据文件锁路径。
     *
     * @return {@code credentials.lock} 路径
     */
    public static Path credentialsLockFile() {
        return configDir().resolve("credentials.lock");
    }
}
