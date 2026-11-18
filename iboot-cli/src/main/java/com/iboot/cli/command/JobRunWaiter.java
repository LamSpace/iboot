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
 * 任务执行结果等待器。
 *
 * <p>轮询该任务在触发后新产生的执行日志，直到状态为成功或失败；超时以业务错误结束。
 * 轮询使用单次短请求，避免长时间占用网关连接。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class JobRunWaiter {

    static final int STATUS_SUCCESS = 1;

    static final int STATUS_FAIL = 0;

    private final Supplier<JsonNode> poller;

    private final Duration interval;

    private final Duration timeout;

    private final Sleeper sleeper;

    /**
     * 创建等待器。
     *
     * @param poller 读取本次执行日志的函数；尚无新日志时返回 {@code null}
     * @param interval 轮询间隔
     * @param timeout 等待超时
     */
    public JobRunWaiter(Supplier<JsonNode> poller, Duration interval, Duration timeout) {
        this(poller, interval, timeout, Thread::sleep);
    }

    JobRunWaiter(Supplier<JsonNode> poller, Duration interval, Duration timeout, Sleeper sleeper) {
        this.poller = poller;
        this.interval = interval;
        this.timeout = timeout;
        this.sleeper = sleeper;
    }

    /**
     * 等待任务进入终态。
     *
     * @return 最终执行日志
     * @throws CliException 执行失败、超时或等待被中断时抛出
     */
    public JsonNode awaitTerminal() {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            JsonNode log = poller.get();
            if (log != null) {
                int status = status(log);
                if (status == STATUS_SUCCESS) {
                    return log;
                }
                if (status == STATUS_FAIL) {
                    throw new CliException(ExitCode.BUSINESS, failureMessage(log));
                }
            }
            if (System.nanoTime() >= deadline) {
                throw new CliException(ExitCode.BUSINESS,
                        "等待任务执行超时，未出现新的终态日志");
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
            throw new CliException(ExitCode.BUSINESS, "等待任务执行被中断", e);
        }
    }

    private int status(JsonNode log) {
        return log.has("status") && log.get("status").isNumber()
                ? log.get("status").asInt()
                : -1;
    }

    private String failureMessage(JsonNode log) {
        String exception = text(log, "exceptionInfo");
        if (exception != null && !exception.isBlank()) {
            return "任务执行失败：" + exception;
        }
        String message = text(log, "jobMessage");
        return message == null || message.isBlank()
                ? "任务执行失败，请查看任务执行日志"
                : "任务执行失败：" + message;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString();
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
