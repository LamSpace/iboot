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
 * 数据备份查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "backup", description = "查询数据备份",
        subcommands = {BackupCommand.ListCommand.class, BackupCommand.SummaryCommand.class})
public class BackupCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot backup [list|summary]");
    }

    /**
     * 备份记录。
     */
    @Command(name = "list", description = "备份记录")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(get("/api/monitor/backup/records", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
        }
    }

    /**
     * 备份汇总。
     */
    @Command(name = "summary", description = "备份汇总")
    public static class SummaryCommand extends BaseCommand {

        @Override
        public void run() {
            print(get("/api/monitor/backup/summary", Map.of()));
        }
    }
}
