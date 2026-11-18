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

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.Map;

/**
 * 在线会话查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "online", description = "查询在线会话与强制下线",
        subcommands = {OnlineCommand.ListCommand.class, OnlineCommand.KickCommand.class})
public class OnlineCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot online [list|kick]");
    }

    /**
     * 在线会话列表。
     */
    @Command(name = "list", description = "在线会话列表")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/online/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }

    /**
     * 强制下线在线会话。
     */
    @Command(name = "kick", description = "强制下线指定在线会话")
    public static class KickCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<tokenId>", description = "online list 返回的脱敏 tokenId")
        String tokenId;

        @Override
        public void run() {
            if (tokenId == null || tokenId.isBlank()) {
                throw new CliException(ExitCode.USAGE, "tokenId 不能为空");
            }
            ApiResponse response = write("DELETE", "/api/online/" + tokenId, null);
            if (response == null) {
                return;
            }
            if (options.json || options.raw) {
                print(response);
            } else {
                Output.message("已强制下线：" + tokenId);
            }
        }
    }
}
