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

/**
 * 备份错误信息脱敏工具。
 *
 * <p>把数据库密码、主密钥等敏感值替换为固定掩码，并截断到数据库列允许的长度，
 * 避免错误信息在记录、日志或接口响应中泄漏凭据。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class BackupErrorSanitizer {

    /**
     * 脱敏后的固定掩码。
     */
    private static final String MASK = "******";

    /**
     * 错误信息最大保留长度。
     */
    private static final int MAX_LENGTH = 1000;

    private BackupErrorSanitizer() {
    }

    /**
     * 对异常消息脱敏。
     *
     * @param error   异常
     * @param secrets 需要替换的敏感值
     * @return 脱敏并截断后的消息
     */
    public static String sanitize(final Throwable error, final String... secrets) {
        if (error == null) {
            return null;
        }
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }
        return sanitize(message, secrets);
    }

    /**
     * 对文本脱敏。
     *
     * @param message 原始文本
     * @param secrets 需要替换的敏感值
     * @return 脱敏并截断后的文本
     */
    public static String sanitize(final String message, final String... secrets) {
        if (message == null) {
            return null;
        }
        String result = message;
        if (secrets != null) {
            for (String secret : secrets) {
                if (secret != null && !secret.isBlank()) {
                    result = result.replace(secret, MASK);
                }
            }
        }
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
    }
}
