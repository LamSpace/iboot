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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 消息发送请求 DTO
 *
 * @author iBoot
 */
@Data
@Schema(description = "消息发送请求")
public class MessageSendRequest {

    @Schema(description = "消息ID")
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    @Schema(description = "接收用户ID列表（接收类型为指定用户时必填）")
    private List<Long> userIds;
}
