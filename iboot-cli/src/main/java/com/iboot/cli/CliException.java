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

package com.iboot.cli;

/**
 * 带退出码的 CLI 异常。
 *
 * <p>命令层抛出该异常后由根命令统一转换为退出码与错误输出；异常消息不得包含
 * 访问令牌、刷新令牌或其他凭据内容。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CliException extends RuntimeException {

    private final int exitCode;

    /**
     * 创建异常。
     *
     * @param exitCode 退出码
     * @param message 面向用户的错误消息
     */
    public CliException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    /**
     * 创建带原因的异常。
     *
     * @param exitCode 退出码
     * @param message 面向用户的错误消息
     * @param cause 原始异常
     */
    public CliException(int exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    /**
     * 获取退出码。
     *
     * @return 退出码
     */
    public int exitCode() {
        return exitCode;
    }
}
