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
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 备份记录终态等待器。
 *
 * <p>按固定间隔轮询备份记录，直到状态为 {@code SUCCESS} 或 {@code FAILED}；超时以业务
 * 错误结束。轮询使用单次短请求，避免长时间占用网关连接。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupWaiter {

    static final String STATUS_RUNNING = "RUNNING";

    static final String STATUS_SUCCESS = "SUCCESS";

    static final String STATUS_FAILED = "FAILED";

    private final Supplier<JsonNode> poller;

    private final Duration interval;

    private final Duration timeout;

    private final Sleeper sleeper;

    /**
     * 创建等待器。
     *
     * @param poller 读取备份记录详情的函数
     * @param interval 轮询间隔
     * @param timeout 等待超时
     */
    public BackupWaiter(Supplier<JsonNode> poller, Duration interval, Duration timeout) {
        this(poller, interval, timeout, Thread::sleep);
    }

    BackupWaiter(Supplier<JsonNode> poller, Duration interval, Duration timeout, Sleeper sleeper) {
        this.poller = poller;
        this.interval = interval;
        this.timeout = timeout;
        this.sleeper = sleeper;
    }

    /**
     * 等待备份进入终态。
     *
     * @return 最终备份记录
     * @throws CliException 备份失败、超时或等待被中断时抛出
     */
    public JsonNode awaitTerminal() {
        long deadline = System.nanoTime() + timeout.toNanos();
        JsonNode last = null;
        while (true) {
            last = poller.get();
            String status = status(last);
            if (STATUS_SUCCESS.equals(status)) {
                return last;
            }
            if (STATUS_FAILED.equals(status)) {
                throw new CliException(ExitCode.BUSINESS, failureMessage(last));
            }
            if (System.nanoTime() >= deadline) {
                throw new CliException(ExitCode.BUSINESS,
                        "等待备份超时，当前状态：" + (status == null ? "未知" : status));
            }
            sleep();
        }
    }

    private void sleep() {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return;
        }
        try {
            sleeper.sleep(interval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(ExitCode.BUSINESS, "等待备份被中断", e);
        }
    }

    private String status(JsonNode data) {
        if (data == null || !data.has("status")) {
            return null;
        }
        JsonNode value = data.get("status");
        return value == null || value.isNull() ? null : value.asString();
    }

    private String failureMessage(JsonNode data) {
        if (data != null && data.has("errorMessage")) {
            JsonNode value = data.get("errorMessage");
            if (value != null && !value.isNull() && !value.asString().isBlank()) {
                return "备份失败：" + value.asString();
            }
        }
        return "备份失败，请查看备份记录";
    }

    /**
     * 可替换的休眠函数，便于测试。
     */
    @FunctionalInterface
    interface Sleeper {

        /**
         * 休眠指定毫秒。
         *
         * @param millis 毫秒
         * @throws InterruptedException 被中断时抛出
         */
        void sleep(long millis) throws InterruptedException;
    }
}
