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
import picocli.CommandLine.Parameters;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定时任务查询命令。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Command(name = "job", description = "查询与执行定时任务",
        subcommands = {JobCommand.ListCommand.class, JobCommand.LogsCommand.class,
                JobCommand.RunCommand.class})
public class JobCommand extends BaseCommand {

    @Override
    public void run() {
        Output.message("用法：iboot job [list|logs|run]");
    }

    /**
     * 任务列表。
     */
    @Command(name = "list", description = "任务列表")
    public static class ListCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Override
        public void run() {
            print(getPaged("/api/job/list", page, Map.of()));
        }
    }

    /**
     * 执行日志。
     */
    @Command(name = "logs", description = "任务执行日志")
    public static class LogsCommand extends BaseCommand {

        @Mixin
        PageOptions page = new PageOptions();

        @Option(names = "--job-id", description = "按任务 ID 过滤")
        Long jobId;

        @Override
        public void run() {
            Map<String, String> query = new LinkedHashMap<>();
            if (jobId != null) {
                query.put("jobId", String.valueOf(jobId));
            }
            print(getPaged("/api/job/log/list", page, query));
        }
    }

    /**
     * 立即执行一次任务。
     */
    @Command(name = "run", description = "立即执行一次任务")
    public static class RunCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<id>", description = "任务 ID")
        Long id;

        @Option(names = "--wait", description = "等待本次执行进入终态")
        boolean wait;

        @Option(names = "--interval", paramLabel = "<seconds>", defaultValue = "2",
                description = "轮询间隔秒数，默认 2")
        long interval;

        @Option(names = "--timeout", paramLabel = "<seconds>", defaultValue = "300",
                description = "等待超时秒数，默认 300")
        long timeout;

        @Override
        public void run() {
            if (wait && (interval <= 0 || timeout <= 0)) {
                throw new CliException(ExitCode.USAGE, "--interval 与 --timeout 必须大于 0");
            }
            if (wait && !options.dryRun && !options.yes) {
                throw new CliException(ExitCode.CONFIRM,
                        "写操作需要 --yes 确认；可先用 --dry-run 查看请求");
            }
            long baseline = wait && !options.dryRun ? latestLogId() : 0L;
            ApiResponse response = write("POST", "/api/job/" + id + "/run", null);
            if (response == null) {
                return;
            }
            if (!wait) {
                if (options.json || options.raw) {
                    print(response);
                } else {
                    Output.message("任务已触发：" + id + "（可用 iboot job logs --job-id " + id + " 查看）");
                }
                return;
            }
            JobRunWaiter waiter = new JobRunWaiter(
                    () -> latestLogAfter(baseline),
                    Duration.ofSeconds(interval), Duration.ofSeconds(timeout));
            JsonNode log = waiter.awaitTerminal();
            if (options.json || options.raw) {
                print(new ApiResponse(200, null, log, log.toString()));
            } else {
                Output.message("任务执行成功：" + id + "（日志 ID " + log.path("id").asLong(0) + "）");
            }
        }

        /**
         * 读取该任务当前最新日志 ID，作为本次触发的基线。
         *
         * @return 最新日志 ID；没有日志时返回 0
         */
        private long latestLogId() {
            JsonNode rows = rows(get("/api/job/log/list", query()).data());
            return rows == null || rows.size() == 0 ? 0L : rows.get(0).path("id").asLong(0);
        }

        /**
         * 读取触发后新产生的日志。
         *
         * @param baseline 触发前的最新日志 ID
         * @return 新日志；尚无新日志时返回 {@code null}
         */
        private JsonNode latestLogAfter(long baseline) {
            JsonNode rows = rows(get("/api/job/log/list", query()).data());
            if (rows == null || rows.size() == 0) {
                return null;
            }
            JsonNode latest = rows.get(0);
            return latest.path("id").asLong(0) > baseline ? latest : null;
        }

        private Map<String, String> query() {
            return Map.of("jobId", String.valueOf(id), "pageNum", "1", "pageSize", "1");
        }

        private JsonNode rows(JsonNode data) {
            if (data == null || !data.has("data") || !data.get("data").isArray()) {
                return null;
            }
            return data.get("data");
        }
    }
}
