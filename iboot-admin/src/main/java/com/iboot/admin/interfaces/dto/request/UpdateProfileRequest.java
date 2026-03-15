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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人信息请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "更新个人信息请求")
public class UpdateProfileRequest {
    
    @Schema(description = "昵称")
    @Size(max = 30, message = "昵称长度不能超过30")
    private String nickname;
    
    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50")
    private String email;
    
    @Schema(description = "手机号码")
    @Size(max = 11, message = "手机号码长度不能超过11")
    private String phone;
    
    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;
}
