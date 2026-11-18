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

import com.iboot.cli.config.Context;
import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 上下文管理命令。
 *
 * <p>只管理地址与客户端标识；令牌仍由 {@code login}/{@code logout} 按 context 隔离。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "config", description = "管理 context",
        subcommands = {ConfigCommand.ListCommand.class, ConfigCommand.UseCommand.class,
                ConfigCommand.ShowCommand.class})
public class ConfigCommand extends BaseCommand {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public void run() {
        Output.message("用法：iboot config [list|use|show]");
    }

    /**
     * 列出上下文。
     */
    @Command(name = "list", description = "列出全部 context")
    public static class ListCommand extends BaseCommand {

        @Override
        public void run() {
            String current = app().context(null).name();
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (Context context : app().contexts()) {
                ObjectNode node = JsonNodeFactory.instance.objectNode();
                node.put("name", context.name());
                node.put("baseUrl", context.baseUrl());
                node.put("issuer", context.issuer());
                node.put("current", context.name().equals(current));
                array.add(node);
            }
            Output.print(new ApiResponse(200, null, array, array.toString()), options.json, false);
        }
    }

    /**
     * 切换上下文。
     */
    @Command(name = "use", description = "切换当前 context")
    public static class UseCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<name>", description = "context 名称")
        String name;

        @Override
        public void run() {
            Context context = app().useContext(name);
            Output.message("已切换到 context：" + context.name() + "（" + context.baseUrl() + "）");
        }
    }

    /**
     * 显示当前上下文。
     */
    @Command(name = "show", description = "显示当前 context")
    public static class ShowCommand extends BaseCommand {

        @Override
        public void run() {
            Context context = context();
            Output.print(new ApiResponse(200, null, MAPPER.valueToTree(context),
                    MAPPER.valueToTree(context).toString()), options.json, false);
        }
    }
}
