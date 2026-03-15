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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 运行日志响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "运行日志响应")
public class RunLogResponse {
    
    @Schema(description = "ES 文档ID")
    private String id;
    
    @Schema(description = "日志时间")
    private String timestamp;
    
    @Schema(description = "日志级别")
    private String level;
    
    @Schema(description = "线程名")
    private String thread;
    
    @Schema(description = "Logger 类名")
    private String logger;
    
    @Schema(description = "日志消息")
    private String message;
}
