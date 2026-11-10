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
 * <p>仅由测试 profile 下的过滤器与切面使用，通过请求头把“在第几步之后失败”的意图
 * 从 HTTP 层传到应用层。默认 profile 不注册相关 Bean，标记始终为空。
 *
 * <p>标记是线程本地的，请求结束必须清理，避免线程复用造成串扰。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SeataDemoFaultContext {

    /**
     * 远程额度扣减成功后失败。
     */
    public static final String AFTER_REMOTE = "after-remote";

    /**
     * TCC Try 成功后失败。
     */
    public static final String AFTER_TCC_TRY = "after-tcc-try";

    private static final ThreadLocal<String> MODE = new ThreadLocal<>();

    private SeataDemoFaultContext() {
    }

    /**
     * 标记当前请求的故障模式。
     *
     * @param mode 故障模式
     */
    public static void request(final String mode) {
        MODE.set(mode);
    }

    /**
     * 返回当前请求是否标记了指定故障模式。
     *
     * @param mode 故障模式
     * @return 模式匹配时返回 {@code true}
     */
    public static boolean isRequested(final String mode) {
        return mode.equals(MODE.get());
    }

    /**
     * 清理当前线程的标记。
     */
    public static void clear() {
        MODE.remove();
    }
}
