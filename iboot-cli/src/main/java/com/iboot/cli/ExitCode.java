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
 * CLI 进程退出码。
 *
 * <p>退出码是 CLI 与脚本之间的稳定契约：认证失效与越权必须可区分，写操作缺少确认
 * 也必须独立表达，避免调用方只能用“非零”判断失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ExitCode {

    /**
     * 成功。
     */
    public static final int OK = 0;

    /**
     * 用法错误或参数非法。
     */
    public static final int USAGE = 2;

    /**
     * 认证失效，需要重新登录。
     */
    public static final int AUTH = 3;

    /**
     * 越权，服务端返回 403。
     */
    public static final int FORBIDDEN = 4;

    /**
     * 网络错误或服务端 5xx。
     */
    public static final int NETWORK = 5;

    /**
     * 业务错误或本地配置错误。
     */
    public static final int BUSINESS = 6;

    /**
     * 写操作缺少 {@code --yes} 确认。
     */
    public static final int CONFIRM = 7;

    private ExitCode() {
    }
}
