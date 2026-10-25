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

package com.iboot.ai.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用的 OpenAI 兼容服务。
 *
 * <p>覆盖非流式与流式响应，并支持注入 401/429 等错误，用于验证降级路径而不消耗真实 token。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class FakeOpenAiServer {

    private static final String NON_STREAM_BODY = "{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\","
            + "\"created\":1,\"model\":\"qwen3.8-flash\","
            + "\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"你好\"},"
            + "\"finish_reason\":\"stop\"}],"
            + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":2,\"total_tokens\":12}}";

    private static final String DELTA_ONE = "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\","
            + "\"created\":1,\"model\":\"qwen3.8-flash\","
            + "\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"你\"},"
            + "\"finish_reason\":null}]}\n\n";

    private static final String DELTA_TWO = "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\","
            + "\"created\":1,\"model\":\"qwen3.8-flash\","
            + "\"choices\":[{\"index\":0,\"delta\":{\"content\":\"好\"},\"finish_reason\":\"stop\"}]}\n\n";

    private static final String USAGE_CHUNK = "data: {\"id\":\"chatcmpl-test\",\"object\":\"chat.completion.chunk\","
            + "\"created\":1,\"model\":\"qwen3.8-flash\",\"choices\":[],"
            + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":2,\"total_tokens\":12}}\n\n";

    private final HttpServer server;

    private final AtomicInteger requests = new AtomicInteger();

    private volatile int status = 200;

    private volatile String errorBody = "{\"error\":{\"message\":\"test error\"}}";

    /**
     * 启动服务。
     *
     * @return 已启动的实例
     */
    public static FakeOpenAiServer start() {
        try {
            return new FakeOpenAiServer();
        } catch (IOException e) {
            throw new IllegalStateException("无法启动假 OpenAI 服务", e);
        }
    }

    private FakeOpenAiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::handle);
        server.start();
    }

    /**
     * 返回可作为 base-url 的地址。
     *
     * @return base URL（含 /v1）
     */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    /**
     * 已收到的请求数。
     *
     * @return 请求数
     */
    public int requests() {
        return requests.get();
    }

    /**
     * 重置请求计数与错误状态。
     */
    public void reset() {
        requests.set(0);
        status = 200;
    }

    /**
     * 让后续请求返回指定错误。
     *
     * @param httpStatus HTTP 状态
     * @param body       错误体
     */
    public void failWith(int httpStatus, String body) {
        this.status = httpStatus;
        this.errorBody = body;
    }

    /**
     * 停止服务。
     */
    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        if (status != 200) {
            byte[] body = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
            return;
        }

        if (requestBody.contains("\"stream\":true")) {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(DELTA_ONE.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.write(DELTA_TWO.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.write(USAGE_CHUNK.getBytes(StandardCharsets.UTF_8));
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            return;
        }

        byte[] body = NON_STREAM_BODY.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
