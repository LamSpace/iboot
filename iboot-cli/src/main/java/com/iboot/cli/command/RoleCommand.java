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
 * 角色查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "role", description = "查询角色", subcommands = {RoleCommand.ListCommand.class})
public class RoleCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot role [list]");
    }

    /**
     * 角色列表。
     */
    @Command(name = "list", description = "角色列表")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(getPaged("/api/role/list", page, Map.of()));
        }
    }
}
