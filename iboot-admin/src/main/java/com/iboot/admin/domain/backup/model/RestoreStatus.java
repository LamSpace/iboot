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
 * 恢复演练状态。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum RestoreStatus {

    /**
     * 正在演练。
     */
    RUNNING,

    /**
     * 还原成功且逐表行数一致。
     */
    SUCCESS,

    /**
     * 还原完成但行数快照不一致。
     */
    MISMATCH,

    /**
     * 演练执行失败。
     */
    FAILED;

    /**
     * 判断是否处于终态。
     *
     * @return 非运行中返回 {@code true}
     */
    public boolean isTerminal() {
        return this != RUNNING;
    }
}
