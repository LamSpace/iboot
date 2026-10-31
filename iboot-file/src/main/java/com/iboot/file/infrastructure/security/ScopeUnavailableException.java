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

package com.iboot.file.infrastructure.security;

/**
 * 权限范围不可用异常。
 *
 * <p>管理端 scope 接口不可用、响应非法或当前请求缺少凭证时抛出；调用方必须按失败关闭处理，
 * 不得退化成「不过滤」。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ScopeUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 创建异常。
     *
     * @param message 错误说明
     */
    public ScopeUnavailableException(String message) {
        super(message);
    }

    /**
     * 创建异常。
     *
     * @param message 错误说明
     * @param cause 根因
     */
    public ScopeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
