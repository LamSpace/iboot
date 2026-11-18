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
import com.iboot.cli.output.Output;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

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
            print(get("/api/job/list", Map.of(
                    "pageNum", String.valueOf(page.page),
                    "pageSize", String.valueOf(page.size))));
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
            query.put("pageNum", String.valueOf(page.page));
            query.put("pageSize", String.valueOf(page.size));
            if (jobId != null) {
                query.put("jobId", String.valueOf(jobId));
            }
            print(get("/api/job/log/list", query));
        }
    }

    /**
     * 立即执行一次任务。
     */
    @Command(name = "run", description = "立即执行一次任务")
    public static class RunCommand extends BaseCommand {

        @Parameters(index = "0", paramLabel = "<id>", description = "任务 ID")
        Long id;

        @Override
        public void run() {
            ApiResponse response = write("POST", "/api/job/" + id + "/run", null);
            if (response == null) {
                return;
            }
            if (options.json || options.raw) {
                print(response);
            } else {
                Output.message("任务已触发：" + id + "（可用 iboot job logs --job-id " + id + " 查看）");
            }
        }
    }
}
