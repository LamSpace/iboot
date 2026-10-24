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

package com.iboot.admin.domain.backup.model;

/**
 * 备份记录状态。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum BackupStatus {

    /**
     * 正在执行。
     */
    RUNNING,

    /**
     * 备份成功，产物可用于恢复演练。
     */
    SUCCESS,

    /**
     * 备份失败，无可用产物。
     */
    FAILED;

    /**
     * 判断是否处于终态。
     *
     * @return 成功或失败返回 {@code true}
     */
    public boolean isTerminal() {
        return this != RUNNING;
    }
}
