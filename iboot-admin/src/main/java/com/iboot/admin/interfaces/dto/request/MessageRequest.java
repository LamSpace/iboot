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
 * 消息请求 DTO
 *
 * @author iBoot
 */
@Data
@Schema(description = "消息请求")
public class MessageRequest {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息标题")
    @NotBlank(message = "消息标题不能为空")
    @Size(max = 200, message = "消息标题长度不能超过200")
    private String title;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "消息类型（字典 sys_message_type）")
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    @Schema(description = "优先级（字典 sys_message_priority）")
    private String priority;

    @Schema(description = "接收者类型（字典 sys_message_receiver_type）")
    @NotBlank(message = "接收者类型不能为空")
    private String receiverType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
