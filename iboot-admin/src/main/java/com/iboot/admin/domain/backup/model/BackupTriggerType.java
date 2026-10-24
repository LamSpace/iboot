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
 * 备份触发方式。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum BackupTriggerType {

    /**
     * 由 Quartz 定时任务触发。
     */
    SCHEDULED,

    /**
     * 由管理员手动触发。
     */
    MANUAL
}
