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

package com.iboot.admin.infrastructure.persistence.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务持久化对象
 * <p>
 * 对应数据库表：sys_job
 * </p>
 *
 * @author iBoot
 */
@Data
public class JobPO {

    /**
     * 任务 ID
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
     * 所属部门 ID
     */
    private Long deptId;

    /**
     * 调用目标字符串
     */
    private String invokeTarget;

    /**
     * Cron 执行表达式
     */
    private String cronExpression;

    /**
     * 计划执行错误策略：1-立即执行，2-放弃执行
     */
    private Integer misfirePolicy;

    /**
     * 是否并发执行：0-禁止，1-允许
     */
    private Integer concurrent;

    /**
     * 状态：0-停用，1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;
}
