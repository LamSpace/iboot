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

import com.iboot.cli.auth.JwtPayload;
import com.iboot.cli.auth.TokenSet;
import com.iboot.cli.config.Context;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 登录状态命令。
 *
 * <p>只展示 context、用户与到期时间，不打印任何令牌原文。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "status", description = "显示当前登录状态")
public class StatusCommand extends BaseCommand {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public void run() {
        Context context = context();
        TokenSet tokens = app().tokens(context.name());
        JwtPayload payload = tokens == null ? null : JwtPayload.parse(tokens.accessToken());
        String source = app().hasTokenOverride() ? "stdin"
                : System.getenv("IBOOT_TOKEN") == null ? "credentials" : "IBOOT_TOKEN";
        if (options.json) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("context", context.name());
            node.put("baseUrl", context.baseUrl());
            node.put("issuer", context.issuer());
            node.put("loggedIn", tokens != null);
            if (payload != null) {
                node.put("username", payload.username());
                node.put("nickname", payload.nickname());
                node.put("userId", payload.userId());
            }
            if (tokens != null) {
                node.put("expiresAt", tokens.expiresAt());
                node.put("refreshAvailable", tokens.hasRefreshToken());
                node.put("sessionRenewal", tokens.hasRefreshToken()
                        ? "refresh_token" : "sliding_session");
                node.put("source", source);
            }
            Output.print(new com.iboot.cli.http.ApiResponse(200, null, node, node.toString()),
                    true, false);
            return;
        }
        Output.message("context    : " + context.name());
        Output.message("baseUrl    : " + context.baseUrl());
        Output.message("issuer     : " + context.issuer());
        if (tokens == null) {
            Output.message("登录状态   : 未登录");
            return;
        }
        Output.message("登录状态   : 已登录（" + source + "）");
        if (payload != null) {
            Output.message("用户       : " + payload.username()
                    + (payload.nickname() == null ? "" : "（" + payload.nickname() + "）"));
        }
        Output.message("JWT 到期   : " + TIME_FORMAT.format(Instant.ofEpochMilli(tokens.expiresAt())));
        Output.message("会话续期   : " + (tokens.hasRefreshToken()
                ? "刷新令牌" : "共享会话滑动（公共客户端不签发刷新令牌）"));
    }
}
