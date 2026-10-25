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

import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE 上游订阅的持有者。
 *
 * <p>连接完成、超时或出错时释放订阅，避免模型继续生成。先于订阅创建而触发释放是安全的。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SubscriptionRegistry {

    private final AtomicReference<Disposable> subscription = new AtomicReference<>();

    /**
     * 绑定订阅。
     *
     * @param disposable 订阅
     */
    public void attach(Disposable disposable) {
        subscription.set(disposable);
    }

    /**
     * 释放订阅；未绑定时为空操作。
     */
    public void dispose() {
        Disposable disposable = subscription.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
