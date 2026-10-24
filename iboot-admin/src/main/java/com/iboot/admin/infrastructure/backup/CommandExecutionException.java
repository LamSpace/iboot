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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.backup;

import java.io.IOException;

/**
 * 外部命令非零退出时抛出的异常。
 *
 * <p>异常消息中的标准错误已由调用方脱敏，可以直接持久化到备份记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CommandExecutionException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * 进程退出码。
     */
    private final int exitCode;

    /**
     * 脱敏后的标准错误。
     */
    private final String stderr;

    /**
     * 创建命令执行异常。
     *
     * @param stage   执行阶段描述，例如“导出”或“还原”
     * @param exitCode 退出码
     * @param stderr   脱敏后的标准错误
     */
    public CommandExecutionException(final String stage, final int exitCode, final String stderr) {
        super(stage + "命令执行失败，退出码: " + exitCode + (stderr == null || stderr.isBlank()
                ? "" : "，错误: " + stderr));
        this.exitCode = exitCode;
        this.stderr = stderr;
    }

    /**
     * 获取退出码。
     *
     * @return 退出码
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * 获取脱敏后的标准错误。
     *
     * @return 标准错误
     */
    public String getStderr() {
        return stderr;
    }
}
