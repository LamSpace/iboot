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

package com.iboot.cli.command;

import picocli.CommandLine.Option;

/**
 * 所有子命令共享的选项。
 *
 * <p>{@code --yes} 与 {@code --dry-run} 只对写操作生效；查询命令忽略它们。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class GlobalOptions {

    /**
     * 指定上下文名称。
     */
    @Option(names = "--context", paramLabel = "<name>", description = "使用指定 context")
    public String context;

    /**
     * 输出解包后的 JSON。
     */
    @Option(names = "--json", description = "输出解包后的业务 JSON")
    public boolean json;

    /**
     * 输出原始响应。
     */
    @Option(names = "--raw", description = "输出原始响应，不做解包")
    public boolean raw;

    /**
     * 确认写操作。
     */
    @Option(names = "--yes", description = "确认执行写操作")
    public boolean yes;

    /**
     * 只打印请求，不发送。
     */
    @Option(names = "--dry-run", description = "只打印将要发送的请求")
    public boolean dryRun;
}
