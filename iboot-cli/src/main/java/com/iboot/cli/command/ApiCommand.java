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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 任意接口逃生口。
 *
 * <p>只读方法默认放行；其他方法必须带 {@code --yes}，{@code --dry-run} 只打印请求。
 * 该命令仍使用当前用户的 Bearer 令牌，不绕过服务端权限判断。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "api", description = "调用任意已存在接口")
public class ApiCommand extends BaseCommand {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /**
     * HTTP 方法。
     */
    @Parameters(index = "0", paramLabel = "<METHOD>", description = "HTTP 方法")
    String method;

    /**
     * 接口路径。
     */
    @Parameters(index = "1", paramLabel = "<PATH>", description = "以 / 开头的接口路径")
    String path;

    /**
     * JSON 请求体。
     */
    @Option(names = {"-d", "--body"}, description = "JSON 请求体")
    String body;

    @Override
    public void run() {
        String normalized = method.toUpperCase(Locale.ROOT);
        if (SAFE_METHODS.contains(normalized)) {
            print(app().request(context(), normalized, path, body, Map.of(), options.raw));
            return;
        }
        ApiResponse response = write(normalized, path, body);
        if (response != null) {
            print(response);
        }
    }
}
