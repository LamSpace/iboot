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
import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;

/**
 * 数据备份查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "backup", description = "查询与触发数据备份",
        subcommands = {BackupCommand.ListCommand.class, BackupCommand.SummaryCommand.class,
                BackupCommand.TriggerCommand.class})
public class BackupCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot backup [list|summary|trigger]");
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
            print(getPaged("/api/monitor/backup/records", page, Map.of()));
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

    /**
     * 手动触发一次备份。
     */
    @Command(name = "trigger", description = "手动触发一次备份")
    public static class TriggerCommand extends BaseCommand {

        @Option(names = "--wait", description = "等待备份进入终态")
        boolean wait;

        @Option(names = "--interval", defaultValue = "2", description = "轮询间隔（秒）")
        long intervalSeconds;

        @Option(names = "--timeout", defaultValue = "300", description = "等待超时（秒）")
        long timeoutSeconds;

        @Override
        public void run() {
            if (intervalSeconds <= 0 || timeoutSeconds <= 0) {
                throw new CliException(ExitCode.USAGE, "--interval 与 --timeout 必须大于 0");
            }
            ApiResponse response = write("POST", "/api/monitor/backup/records", null);
            if (response == null) {
                return;
            }
            JsonNode data = response.data();
            if (data == null || !data.has("id") || data.get("id").isNull()) {
                throw new CliException(ExitCode.BUSINESS, "备份触发响应缺少记录 ID");
            }
            long id = data.get("id").asLong();
            if (!wait) {
                if (options.json || options.raw) {
                    print(response);
                } else {
                    Output.message("备份已触发：id=" + id + "，status=" + status(data));
                }
                return;
            }
            BackupWaiter waiter = new BackupWaiter(
                    () -> app().request(context(), "GET",
                            "/api/monitor/backup/records/" + id, null, Map.of(), false).data(),
                    Duration.ofSeconds(intervalSeconds), Duration.ofSeconds(timeoutSeconds));
            JsonNode finalData = waiter.awaitTerminal();
            if (options.json || options.raw) {
                Output.print(new ApiResponse(200, null, finalData, finalData.toString()),
                        options.json, options.raw);
            } else {
                Output.message("备份结束：id=" + id + "，status=" + status(finalData));
            }
        }

        private String status(JsonNode data) {
            if (data == null || !data.has("status")) {
                return "未知";
            }
            JsonNode value = data.get("status");
            return value == null || value.isNull() ? "未知" : value.asString();
        }
    }
}
