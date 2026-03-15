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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定时任务导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExportVO {

    @ExcelColumn(name = "任务ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "任务名称", order = 2, width = 25)
    private String jobName;

    @ExcelColumn(name = "任务组名", order = 3, width = 15, dictType = "sys_job_group")
    private String jobGroup;

    @ExcelColumn(name = "调用目标", order = 4, width = 40)
    private String invokeTarget;

    @ExcelColumn(name = "cron表达式", order = 5, width = 20)
    private String cronExpression;

    @ExcelColumn(name = "错过策略", order = 6, width = 12, dictType = "sys_job_misfire")
    private Integer misfirePolicy;

    @ExcelColumn(name = "是否并发", order = 7, width = 10, dictType = "sys_job_concurrent")
    private Integer concurrent;

    @ExcelColumn(name = "状态", order = 8, width = 8, dictType = "sys_job_status")
    private Integer status;

    @ExcelColumn(name = "备注", order = 9, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;
}
