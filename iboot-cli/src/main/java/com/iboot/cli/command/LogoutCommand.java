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
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 登出命令。
 *
 * <p>先调用认证服务撤销当前令牌，再删除本地凭据；服务端不可达时仍删除本地凭据并提示。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "logout", description = "撤销当前会话并删除本地凭据")
public class LogoutCommand extends BaseCommand {

    @Override
    public void run() {
        Context context = context();
        boolean revoked = app().logout(context);
        if (!revoked) {
            System.err.println("本地凭据已删除，但服务端会话撤销失败（网络或认证服务不可用）");
        }
        if (options.json) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("context", context.name());
            node.put("revoked", revoked);
            Output.print(new com.iboot.cli.http.ApiResponse(200, null, node, node.toString()),
                    true, false);
        } else {
            Output.message("已退出：" + context.name());
        }
    }
}
