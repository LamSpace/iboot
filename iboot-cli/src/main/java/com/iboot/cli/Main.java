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

package com.iboot.cli;

import com.iboot.cli.command.ApiCommand;
import com.iboot.cli.command.BackupCommand;
import com.iboot.cli.command.ConfigCommand;
import com.iboot.cli.command.DeptCommand;
import com.iboot.cli.command.DictCommand;
import com.iboot.cli.command.JobCommand;
import com.iboot.cli.command.LogCommand;
import com.iboot.cli.command.LoginCommand;
import com.iboot.cli.command.LogoutCommand;
import com.iboot.cli.command.OnlineCommand;
import com.iboot.cli.command.ParamCommand;
import com.iboot.cli.command.RoleCommand;
import com.iboot.cli.command.StatusCommand;
import com.iboot.cli.command.TokenCommand;
import com.iboot.cli.command.UserCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * iBoot 开发者命令行入口。
 *
 * <p>只负责注册根命令并将执行结果转换为进程退出码；具体子命令位于
 * {@code com.iboot.cli.command} 包。CLI 不依赖 Spring 或内核模块，避免把服务端
 * 运行时一并引入。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(
        name = "iboot",
        mixinStandardHelpOptions = true,
        version = "iboot 1.0.0",
        description = "iBoot 开发者命令行工具",
        subcommands = {
                LoginCommand.class,
                LogoutCommand.class,
                StatusCommand.class,
                ConfigCommand.class,
                UserCommand.class,
                RoleCommand.class,
                DeptCommand.class,
                DictCommand.class,
                ParamCommand.class,
                JobCommand.class,
                BackupCommand.class,
                LogCommand.class,
                OnlineCommand.class,
                TokenCommand.class,
                ApiCommand.class
        }
)
public class Main implements Runnable {

    private final CliApplication application;

    /**
     * 创建根命令。
     */
    public Main() {
        this(new CliApplication());
    }

    Main(CliApplication application) {
        this.application = application;
    }

    /**
     * 获取应用服务。
     *
     * @return 应用服务
     */
    public CliApplication application() {
        return application;
    }

    /**
     * 未指定子命令时打印用法。
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.exit(commandLine(new Main()).execute(args));
    }

    static CommandLine commandLine(Main main) {
        CommandLine commandLine = new CommandLine(main);
        commandLine.setParameterExceptionHandler((ex, arguments) -> {
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
            return ExitCode.USAGE;
        });
        commandLine.setExecutionExceptionHandler((ex, command, parseResult) -> {
            if (ex instanceof CliException cliException) {
                System.err.println(cliException.getMessage());
                return cliException.exitCode();
            }
            System.err.println("命令执行失败：" + ex.getMessage());
            return ExitCode.BUSINESS;
        });
        return commandLine;
    }
}
