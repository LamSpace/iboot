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

package com.iboot.ai.application.service.graph;

/**
 * 图能力未启用。\n *\n * <p>开关关闭时访问图端点抛出，由全局异常处理映射为 503。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class GraphDisabledException extends RuntimeException {

    /**
     * 创建异常。
     *
     * @param message 说明
     */
    public GraphDisabledException(String message) {
        super(message);
    }

    /**
     * 创建异常。
     *
     * @param message 说明
     * @param cause   原因
     */
    public GraphDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
