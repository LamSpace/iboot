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

import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "user", description = "查询用户",
        subcommands = {UserCommand.ListCommand.class, UserCommand.GetCommand.class})
public class UserCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot user [list|get]");
    }

    /**
     * 用户列表。
     */
    @Command(name = "list", description = "用户列表")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Option(names = "--username", description = "用户名关键字")
        String username;

        @Option(names = "--status", description = "状态：0-停用，1-启用")
        Integer status;

        @Override
        public void run() {
            Map<String, String> query = new LinkedHashMap<>();
            query.put("pageNum", String.valueOf(page.page));
            query.put("pageSize", String.valueOf(page.size));
            if (username != null) {
                query.put("username", username);
            }
            if (status != null) {
                query.put("status", String.valueOf(status));
            }
            print(get("/api/user/list", query));
        }
    }

    /**
     * 用户详情。
     */
    @Command(name = "get", description = "按 ID 查询用户")
    public static class GetCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<id>", description = "用户 ID")
        Long id;

        @Override
        public void run() {
            ApiResponse response = get("/api/user/" + id, Map.of());
            print(response);
        }
    }
}
