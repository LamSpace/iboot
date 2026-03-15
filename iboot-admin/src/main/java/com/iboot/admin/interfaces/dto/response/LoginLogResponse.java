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

import java.time.LocalDateTime;

/**
 * 登录日志响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录日志响应")
public class LoginLogResponse {
    
    @Schema(description = "日志ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "登录IP")
    private String ipAddress;
    
    @Schema(description = "登录地点")
    private String loginLocation;
    
    @Schema(description = "浏览器")
    private String browser;
    
    @Schema(description = "操作系统")
    private String os;
    
    @Schema(description = "登录状态：0-失败，1-成功")
    private Integer status;
    
    @Schema(description = "提示消息")
    private String msg;
    
    @Schema(description = "登录时间")
    private LocalDateTime loginTime;
}
