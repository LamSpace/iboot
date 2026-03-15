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

package com.iboot.admin.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 定时任务请求 DTO
 *
 * @author iBoot
 */
@Data
@Schema(description = "定时任务请求")
public class JobRequest {

    @Schema(description = "任务ID")
    private Long id;

    @Schema(description = "任务名称")
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 64, message = "任务名称长度不能超过64")
    private String jobName;

    @Schema(description = "任务组名")
    @NotBlank(message = "任务组名不能为空")
    @Size(max = 64, message = "任务组名长度不能超过64")
    private String jobGroup;

    @Schema(description = "调用目标字符串(beanName.methodName)")
    @NotBlank(message = "调用目标不能为空")
    @Size(max = 500, message = "调用目标长度不能超过500")
    private String invokeTarget;

    @Schema(description = "cron执行表达式")
    @NotBlank(message = "cron表达式不能为空")
    @Size(max = 255, message = "cron表达式长度不能超过255")
    private String cronExpression;

    @Schema(description = "计划执行错误策略(1立即执行 2执行一次 3放弃执行)")
    private Integer misfirePolicy = 3;

    @Schema(description = "是否并发执行(0禁止 1允许)")
    private Integer concurrent = 0;

    @Schema(description = "状态(0暂停 1正常)")
    private Integer status = 1;

    @Schema(description = "备注信息")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
