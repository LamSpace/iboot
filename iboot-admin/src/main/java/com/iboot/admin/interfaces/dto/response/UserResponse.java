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
import java.util.List;

/**
 * 用户响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户响应")
public class UserResponse {
    
    @Schema(description = "用户ID")
    private Long id; // 用户唯一标识ID
    
    @Schema(description = "用户名")
    private String username; // 用户名，用于登录系统
    
    @Schema(description = "昵称")
    private String nickname; // 用户昵称
    
    @Schema(description = "邮箱")
    private String email; // 用户邮箱地址
    
    @Schema(description = "手机号码")
    private String phone; // 用户手机号码
    
    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender; // 性别：0-未知，1-男，2-女
    
    @Schema(description = "头像地址")
    private String avatar; // 头像地址
    
    @Schema(description = "用户类型：1-系统用户，2-普通用户")
    private Integer userType; // 用户类型：1-系统用户，2-普通用户
    
    @Schema(description = "部门ID")
    private Long deptId; // 所属部门ID
    
    @Schema(description = "岗位ID列表")
    private List<Long> postIds; // 岗位ID列表
    
    @Schema(description = "角色ID列表")
    private List<Long> roleIds; // 角色ID列表
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status; // 状态：0-停用，1-正常
    
    @Schema(description = "最后登录IP")
    private String loginIp; // 最后登录IP地址
    
    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime; // 最后登录时间
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime; // 创建时间
    
    @Schema(description = "备注")
    private String remark; // 备注信息
}
