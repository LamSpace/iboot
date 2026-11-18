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

import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.Map;

/**
 * 参数配置查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "param", description = "查询参数配置",
        subcommands = {ParamCommand.ListCommand.class, ParamCommand.GetCommand.class})
public class ParamCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot param [list|get]");
    }

    /**
     * 参数列表。
     */
    @Command(name = "list", description = "参数列表")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/config/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }

    /**
     * 按 key 查询参数。
     */
    @Command(name = "get", description = "按配置键查询参数")
    public static class GetCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<key>", description = "配置键")
        String key;

        @Override
        public void run() {
            print(get("/api/config/key/" + key, Map.of()));
        }
    }
}
