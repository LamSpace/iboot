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

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 授权码回调的本地 loopback 服务。
 *
 * <p>只监听 {@code 127.0.0.1}，校验 {@code state} 后把 {@code code} 交给等待中的登录命令。
 * 登录结束必须调用 {@link #close()}，避免端口长期占用。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class LoopbackServer implements AutoCloseable {

    private final HttpServer server;

    private final String expectedState;

    private final CompletableFuture<String> code = new CompletableFuture<>();

    /**
     * 创建并启动 loopback 服务。
     *
     * @param port 监听端口；{@code 0} 表示由系统分配
     * @param expectedState 期望的 OAuth2 state
     * @throws IOException 端口无法绑定时抛出
     */
    public LoopbackServer(int port, String expectedState) throws IOException {
        this.expectedState = expectedState;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.createContext("/callback", this::handle);
        this.server.start();
    }

    /**
     * 获取实际监听端口。
     *
     * @return 端口
     */
    public int port() {
        return server.getAddress().getPort();
    }

    /**
     * 等待授权码。
     *
     * @param timeout 超时时间
     * @return 授权码
     * @throws CliException 超时、回调错误或 state 不匹配时抛出
     */
    public String await(Duration timeout) {
        try {
            return code.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new CliException(ExitCode.AUTH, "等待浏览器登录超时，请重新执行 iboot login");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(ExitCode.AUTH, "登录被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CliException cliException) {
                throw cliException;
            }
            throw new CliException(ExitCode.AUTH, "登录失败：" + cause.getMessage(), cause);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> params = query(exchange.getRequestURI().getRawQuery());
            String error = params.get("error");
            if (error != null) {
                code.completeExceptionally(new CliException(ExitCode.AUTH,
                        "授权失败：" + error + describe(params.get("error_description"))));
                respond(exchange, 400, "登录失败，可以关闭此页面。");
                return;
            }
            String state = params.get("state");
            String value = params.get("code");
            if (value == null || value.isBlank()) {
                code.completeExceptionally(new CliException(ExitCode.AUTH, "回调缺少授权码"));
                respond(exchange, 400, "回调缺少授权码。");
                return;
            }
            if (expectedState != null && !expectedState.equals(state)) {
                code.completeExceptionally(new CliException(ExitCode.AUTH, "回调 state 不匹配"));
                respond(exchange, 400, "登录状态校验失败，请重新执行 iboot login。");
                return;
            }
            code.complete(value);
            respond(exchange, 200, "登录完成，可以关闭此页面并回到终端。");
        } finally {
            exchange.close();
        }
    }

    private void respond(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = ("<!doctype html><html><body><p>" + message + "</p></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private Map<String, String> query(String rawQuery) {
        Map<String, String> result = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            int index = pair.indexOf('=');
            if (index < 0) {
                result.put(decode(pair), "");
            } else {
                result.put(decode(pair.substring(0, index)), decode(pair.substring(index + 1)));
            }
        }
        return result;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String describe(String value) {
        return value == null || value.isBlank() ? "" : "（" + value + "）";
    }
}
