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

import com.iboot.cli.CliApplication;
import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.Main;
import com.iboot.cli.config.Context;
import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.util.Map;
import java.util.Set;

/**
 * 子命令基类。
 *
 * <p>通过 {@link Spec} 找到根命令中的应用服务，统一处理 context 解析、请求发送与输出；
 * 写操作在发送前检查 {@code --yes} 与 {@code --dry-run}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public abstract class BaseCommand implements Runnable {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Spec
    private CommandSpec spec;

    @Mixin
    protected GlobalOptions options = new GlobalOptions();

    @Mixin
    protected HelpOptions help = new HelpOptions();

    /**
     * 获取应用服务。
     *
     * @return 应用服务
     */
    protected CliApplication app() {
        CliApplication application = root().application();
        if (options.tokenStdin) {
            application.useTokenFromStdin();
        }
        return application;
    }

    /**
     * 获取当前上下文。
     *
     * @return 上下文
     */
    protected Context context() {
        return app().context(options.context);
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 接口路径
     * @param query 查询参数
     * @return 响应
     */
    protected ApiResponse get(String path, Map<String, String> query) {
        return app().request(context(), "GET", path, null, query, options.raw);
    }

    /**
     * 发送写请求，并执行确认检查。
     *
     * @param method HTTP 方法
     * @param path 接口路径
     * @param body 请求体
     * @return 响应；{@code --dry-run} 时返回 {@code null}
     */
    protected ApiResponse write(String method, String path, String body) {
        if (!allowWrite(method, path, body)) {
            return null;
        }
        return app().request(context(), method, path, body, Map.of(), options.raw);
    }

    /**
     * 输出响应。
     *
     * @param response 响应
     */
    protected void print(ApiResponse response) {
        if (response != null) {
            Output.print(response, options.json, options.raw);
        }
    }

    private Main root() {
        return (Main) spec.root().userObject();
    }

    private boolean allowWrite(String method, String path, String body) {
        if (SAFE_METHODS.contains(method)) {
            return true;
        }
        if (options.dryRun) {
            System.out.println(method + " " + path);
            if (body != null && !body.isBlank()) {
                System.out.println(body);
            }
            return false;
        }
        if (!options.yes) {
            throw new CliException(ExitCode.CONFIRM,
                    "写操作需要 --yes 确认；可先用 --dry-run 查看请求");
        }
        return true;
    }
}
