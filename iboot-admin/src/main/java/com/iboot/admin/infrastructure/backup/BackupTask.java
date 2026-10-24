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

import com.iboot.admin.application.service.backup.BackupApplicationService;
import org.springframework.stereotype.Component;

/**
 * Quartz 数据库备份任务。
 *
 * <p>Bean 名称固定为 {@code backupTask}，与 {@code sys_job.invoke_target} 的
 * {@code backupTask.run} 对应；实际调度由任务管理页维护。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupApplicationService
 */
@Component("backupTask")
public class BackupTask {

    /**
     * 备份应用服务。
     */
    private final BackupApplicationService backupApplicationService;

    @SuppressWarnings("all")
    public BackupTask(final BackupApplicationService backupApplicationService) {
        this.backupApplicationService = backupApplicationService;
    }

    /**
     * 执行一次定时备份。
     *
     * <p>功能关闭或已有备份运行时由应用服务内部处理，不向调度器抛出异常。
     */
    public void run() {
        backupApplicationService.triggerScheduled();
    }
}
