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

package com.iboot.admin.infrastructure.job;

/**
 * 定时任务调度常量。
 *
 * <p>定义任务状态、并发策略、错过执行策略以及调度器使用的键名。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ScheduleConstants {

    /**
     * 任务调度参数 key
     */
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /**
     * 任务名前缀
     */
    public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";

    /**
     * 执行目标 key
     */
    public static final String JOB_DATA_KEY = "JOB_DATA_KEY";

    /**
     * 默认任务组
     */
    public static final String DEFAULT_JOB_GROUP = "DEFAULT";

    /**
     * 系统任务组
     */
    public static final String SYSTEM_JOB_GROUP = "SYSTEM";

    private ScheduleConstants() {
    }

}
