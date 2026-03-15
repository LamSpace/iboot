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
 * 消息模板请求 DTO
 *
 * @author iBoot
 */
@Data
@Schema(description = "消息模板请求")
public class MessageTemplateRequest {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 100, message = "模板编码长度不能超过100")
    private String templateCode;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 200, message = "模板名称长度不能超过200")
    private String templateName;

    @Schema(description = "模板内容（支持${变量名}占位符）")
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    @Schema(description = "消息类型（字典 sys_message_type）")
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
