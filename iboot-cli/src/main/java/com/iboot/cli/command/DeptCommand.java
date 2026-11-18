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

import java.util.Map;

/**
 * 部门查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "dept", description = "查询部门",
        subcommands = {DeptCommand.TreeCommand.class, DeptCommand.OrgChartCommand.class})
public class DeptCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot dept [tree|org-chart]");
    }

    /**
     * 部门树。
     */
    @Command(name = "tree", description = "部门树")
    public static class TreeCommand extends BaseCommand {

        @Override
        public void run() {
            print(get("/api/dept/tree", Map.of()));
        }
    }

    /**
     * 组织架构图数据。
     */
    @Command(name = "org-chart", description = "组织架构图数据")
    public static class OrgChartCommand extends BaseCommand {

        @Override
        public void run() {
            print(get("/api/dept/orgChart", Map.of()));
        }
    }
}
