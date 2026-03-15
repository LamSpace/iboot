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
 * 部门请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "部门请求")
public class DeptRequest {
    
    @Schema(description = "部门ID")
    private Long id;
    
    @Schema(description = "父部门ID")
    private Long parentId;
    
    @Schema(description = "部门编码")
    @NotBlank(message = "部门编码不能为空")
    @Size(max = 50, message = "部门编码长度不能超过50")
    private String deptCode;
    
    @Schema(description = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50")
    private String deptName;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "负责人")
    @Size(max = 30, message = "负责人长度不能超过30")
    private String leader;
    
    @Schema(description = "负责人电话")
    @Size(max = 11, message = "负责人电话长度不能超过11")
    private String phone;
    
    @Schema(description = "负责人邮箱")
    @Size(max = 50, message = "负责人邮箱长度不能超过50")
    private String email;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
}
