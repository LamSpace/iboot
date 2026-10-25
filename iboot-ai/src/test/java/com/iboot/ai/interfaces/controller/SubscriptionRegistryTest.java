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

package com.iboot.ai.interfaces.controller;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上游订阅释放测试。
 *
 * <p>连接完成或客户端断开时，控制器通过该注册表取消模型流，避免继续生成与计费。
 */
class SubscriptionRegistryTest {

    @Test
    void disposesAttachedSubscription() {
        SubscriptionRegistry registry = new SubscriptionRegistry();
        AtomicBoolean cancelled = new AtomicBoolean();
        Disposable subscription = Flux.never()
                .doOnCancel(() -> cancelled.set(true))
                .subscribe();

        registry.attach(subscription);
        registry.dispose();

        assertThat(cancelled).isTrue();
    }

    @Test
    void disposeBeforeAttachIsSafe() {
        new SubscriptionRegistry().dispose();
    }
}
