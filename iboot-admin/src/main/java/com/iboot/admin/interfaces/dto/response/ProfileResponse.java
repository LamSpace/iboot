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
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人信息响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@Schema(description = "个人信息响应")
public class ProfileResponse {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "手机号码")
    private String phone;
    
    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "部门名称")
    private String deptName;
    
    @Schema(description = "岗位名称列表")
    private List<String> postNames;
    
    @Schema(description = "角色名称列表")
    private List<String> roleNames;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
