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

package com.iboot.workflow.infrastructure.demo;

/**
 * Seata 演练故障标记。
 *
 * <p>仅由 {@code seata-demo} profile 下的过滤器与切面使用，通过请求头把“在远程扣减
 * 成功后失败”的意图从 HTTP 层传到应用层。默认 profile 不注册相关 Bean，标记始终为假。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SeataDemoFaultContext {

    private static final ThreadLocal<Boolean> REQUESTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private SeataDemoFaultContext() {
    }

    /**
     * 标记当前请求需要在远程扣减后失败。
     */
    public static void request() {
        REQUESTED.set(Boolean.TRUE);
    }

    /**
     * 返回当前请求是否标记了故障。
     *
     * @return 是否需要在远程扣减后失败
     */
    public static boolean isRequested() {
        return Boolean.TRUE.equals(REQUESTED.get());
    }

    /**
     * 清理当前线程的标记。
     */
    public static void clear() {
        REQUESTED.remove();
    }
}
