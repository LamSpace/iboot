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

package com.iboot.notify.infrastructure.push;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SseEmitterManager} 的重连测试。
 *
 * <p>同一用户重连时，旧连接的完成、超时、错误与发送失败都不能移除新连接；
 * 这是实时推送在客户端自动重连下仍然可用的前提。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SseEmitterManagerTest {

    /**
     * 旧连接的完成回调不能移除新连接。
     */
    @Test
    void oldCompletionCallbackDoesNotRemoveNewConnection() {
        SseEmitterManager manager = new SseEmitterManager(null, null);
        try {
            SseEmitter first = manager.connect(1L);
            SseEmitter second = manager.connect(1L);

            completionCallback(first).run();

            assertThat(manager.isOnline(1L)).isTrue();
            assertThat(emitters(manager).get(1L)).isSameAs(second);
        } finally {
            manager.disconnect(1L);
        }
    }

    /**
     * 旧连接的超时回调不能移除新连接。
     */
    @Test
    void oldTimeoutCallbackDoesNotRemoveNewConnection() {
        SseEmitterManager manager = new SseEmitterManager(null, null);
        try {
            SseEmitter first = manager.connect(1L);
            SseEmitter second = manager.connect(1L);

            timeoutCallback(first).run();

            assertThat(manager.isOnline(1L)).isTrue();
            assertThat(emitters(manager).get(1L)).isSameAs(second);
        } finally {
            manager.disconnect(1L);
        }
    }

    /**
     * 旧连接的错误回调不能移除新连接。
     */
    @Test
    void oldErrorCallbackDoesNotRemoveNewConnection() {
        SseEmitterManager manager = new SseEmitterManager(null, null);
        try {
            SseEmitter first = manager.connect(1L);
            SseEmitter second = manager.connect(1L);

            errorCallback(first).accept(new IllegalStateException("connection reset"));

            assertThat(manager.isOnline(1L)).isTrue();
            assertThat(emitters(manager).get(1L)).isSameAs(second);
        } finally {
            manager.disconnect(1L);
        }
    }

    /**
     * 对旧连接执行条件断开会话，不影响当前连接。
     */
    @Test
    void staleDisconnectDoesNotRemoveNewConnection() {
        SseEmitterManager manager = new SseEmitterManager(null, null);
        try {
            SseEmitter stale = manager.connect(1L);
            SseEmitter current = manager.connect(1L);

            ReflectionTestUtils.invokeMethod(manager, "disconnect", 1L, stale);

            assertThat(manager.isOnline(1L)).isTrue();
            assertThat(emitters(manager).get(1L)).isSameAs(current);
        } finally {
            manager.disconnect(1L);
        }
    }

    /**
     * 发送失败只断开失败的那个连接。
     */
    @Test
    void sendFailureRemovesOnlyTheFailingEmitter() {
        SseEmitterManager manager = new SseEmitterManager(null, null);
        SseEmitter failing = new SseEmitter() {
            @Override
            public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("broken pipe");
            }
        };
        emitters(manager).put(1L, failing);

        boolean sent = manager.send(1L, PushEvent.builder()
                .id("event-1")
                .type("com.iboot.push.new.message")
                .source("/api/messages")
                .data("payload")
                .build());

        assertThat(sent).isFalse();
        assertThat(manager.isOnline(1L)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Map<Long, SseEmitter> emitters(SseEmitterManager manager) {
        return (Map<Long, SseEmitter>) ReflectionTestUtils.getField(manager, "userEmitters");
    }

    private Runnable completionCallback(SseEmitter emitter) {
        return (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
    }

    private Runnable timeoutCallback(SseEmitter emitter) {
        return (Runnable) ReflectionTestUtils.getField(emitter, "timeoutCallback");
    }

    @SuppressWarnings("unchecked")
    private Consumer<Throwable> errorCallback(SseEmitter emitter) {
        return (Consumer<Throwable>) ReflectionTestUtils.getField(emitter, "errorCallback");
    }
}
