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
import com.iboot.cli.auth.JwtPayload;
import com.iboot.cli.auth.TokenSet;
import com.iboot.cli.config.Context;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;

/**
 * 浏览器登录命令。
 *
 * <p>启动 loopback 回调并打开系统浏览器；{@code --no-browser} 时只打印授权地址，
 * 便于无头环境或脚本完成回调。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "login", description = "通过浏览器完成 OAuth2 授权码登录")
public class LoginCommand extends BaseCommand {

    /**
     * 不自动打开浏览器。
     */
    @Option(names = "--no-browser", description = "只打印授权地址，不打开浏览器")
    boolean noBrowser;

    /**
     * 等待回调的超时秒数。
     */
    @Option(names = "--timeout", defaultValue = "300", description = "等待回调的超时秒数")
    long timeoutSeconds;

    @Override
    public void run() {
        if (timeoutSeconds <= 0) {
            throw new CliException(ExitCode.USAGE, "--timeout 必须大于 0");
        }
        Context context = context();
        TokenSet tokens = app().login(context, !noBrowser, Duration.ofSeconds(timeoutSeconds));
        JwtPayload payload = JwtPayload.parse(tokens.accessToken());
        String username = payload == null || payload.username() == null
                ? "当前用户" : payload.username();
        if (options.json) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("context", context.name());
            node.put("username", username);
            node.put("expiresAt", tokens.expiresAt());
            Output.print(new com.iboot.cli.http.ApiResponse(200, null, node, node.toString()),
                    true, false);
        } else {
            Output.message("登录成功：" + username + "（context=" + context.name() + "）");
        }
    }
}
