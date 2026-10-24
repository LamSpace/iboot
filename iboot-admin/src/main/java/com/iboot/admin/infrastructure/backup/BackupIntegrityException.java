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
 * 备份对象完整性校验失败异常。
 *
 * <p>在解密过程中发现文件头不合法、认证标签校验失败或数据被截断时抛出，
 * 调用方应将其视为备份不可用，不得继续执行还原。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupIntegrityException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用指定消息创建异常。
     *
     * @param message 错误消息
     */
    public BackupIntegrityException(final String message) {
        super(message);
    }

    /**
     * 使用指定消息和根因创建异常。
     *
     * @param message 错误消息
     * @param cause   根因
     */
    public BackupIntegrityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
