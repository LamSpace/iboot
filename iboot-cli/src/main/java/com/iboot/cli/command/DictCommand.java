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
 * 字典查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "dict", description = "查询字典",
        subcommands = {DictCommand.TypesCommand.class, DictCommand.DataCommand.class})
public class DictCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot dict [types|data]");
    }

    /**
     * 字典类型列表。
     */
    @Command(name = "types", description = "字典类型列表")
    public static class TypesCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/dict/type/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }

    /**
     * 指定类型的字典数据。
     */
    @Command(name = "data", description = "按字典类型查询数据")
    public static class DataCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<dictType>", description = "字典类型")
        String dictType;

        @Override
        public void run() {
            print(get("/api/dict/data/type/" + dictType, Map.of()));
        }
    }
}
