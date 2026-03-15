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

package com.iboot.admin.domain.job.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 定时任务领域模型
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务组名
     */
    private String jobGroup;

    /**
     * 调用目标字符串(beanName.methodName)
     */
    private String invokeTarget;

    /**
     * cron执行表达式
     */
    private String cronExpression;

    /**
     * 计划执行错误策略：1-立即执行，2-执行一次，3-放弃执行
     */
    private Integer misfirePolicy;

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    private Integer concurrent;

    /**
     * 状态：0-暂停，1-正常
     */
    private Integer status;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 错过执行策略：立即执行
     */
    public static final int MISFIRE_DO_NOTHING = 1;

    /**
     * 错过执行策略：执行一次
     */
    public static final int MISFIRE_FIRE_AND_PROCEED = 2;

    /**
     * 错过执行策略：放弃执行
     */
    public static final int MISFIRE_IGNORE_MISFIRES = 3;

    /**
     * 任务状态：暂停
     */
    public static final int STATUS_PAUSE = 0;

    /**
     * 任务状态：正常
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 禁止并发执行
     */
    public static final int CONCURRENT_NO = 0;

    /**
     * 允许并发执行
     */
    public static final int CONCURRENT_YES = 1;

    /**
     * 判断任务是否正常运行
     */
    public boolean isNormal() {
        return STATUS_NORMAL == this.status;
    }

    /**
     * 判断任务是否暂停
     */
    public boolean isPaused() {
        return STATUS_PAUSE == this.status;
    }

    /**
     * 判断是否禁止并发
     */
    public boolean isConcurrentDisallowed() {
        return CONCURRENT_NO == this.concurrent;
    }
}
