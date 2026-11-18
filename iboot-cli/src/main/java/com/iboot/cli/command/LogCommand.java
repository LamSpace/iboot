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

import java.util.Map;

/**
 * 日志查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "log", description = "查询登录与操作日志",
        subcommands = {LogCommand.LoginCommand.class, LogCommand.OperateCommand.class})
public class LogCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot log [login|operate]");
    }

    /**
     * 登录日志。
     */
    @Command(name = "login", description = "登录日志")
    public static class LoginCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/log/login/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }

    /**
     * 操作日志。
     */
    @Command(name = "operate", description = "操作日志")
    public static class OperateCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/log/operate/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }
}
