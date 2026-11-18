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
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人访问令牌命令。
 *
 * <p>使用交互式登录会话管理当前用户自己的 PAT；签发与撤销复用 {@link BaseCommand}
 * 的 {@code --yes} 与 {@code --dry-run} 写保护。令牌明文只由签发命令输出一次。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "token", description = "管理个人访问令牌",
        subcommands = {TokenCommand.CreateCommand.class, TokenCommand.ListCommand.class,
                TokenCommand.RevokeCommand.class})
public class TokenCommand extends BaseCommand {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public void run() {
        Output.message("用法：iboot token [create|list|revoke]");
    }

    /**
     * 签发个人访问令牌。
     */
    @Command(name = "create", description = "签发个人访问令牌；明文只显示一次")
    public static class CreateCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<name>", description = "令牌名称")
        String name;

        @Option(names = "--expires-days", paramLabel = "<days>",
                description = "有效期天数；默认 30，最长 90")
        Long expiresDays;

        @Override
        public void run() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", name);
            if (expiresDays != null) {
                body.put("expiresDays", expiresDays);
            }
            ApiResponse response = write("POST", "/api/auth/tokens", JSON.writeValueAsString(body));
            if (response == null) {
                return;
            }
            print(response);
            if (!options.json && !options.raw) {
                Output.message("请立即保存令牌明文；它只显示这一次。");
            }
        }
    }

    /**
     * 列出个人访问令牌。
     */
    @Command(name = "list", description = "列出个人访问令牌")
    public static class ListCommand extends BaseCommand {

        @Override
        public void run() {
            print(get("/api/auth/tokens", Map.of()));
        }
    }

    /**
     * 撤销个人访问令牌。
     */
    @Command(name = "revoke", description = "撤销个人访问令牌")
    public static class RevokeCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<id>", description = "令牌记录 ID")
        Long id;

        @Override
        public void run() {
            ApiResponse response = write("DELETE", "/api/auth/tokens/" + id, null);
            if (response == null) {
                return;
            }
            if (options.json || options.raw) {
                print(response);
            } else {
                Output.message("令牌已撤销：" + id);
            }
        }
    }
}
