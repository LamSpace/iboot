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

import java.util.List;

/**
 * 角色请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "角色请求")
public class RoleRequest {
    
    @Schema(description = "角色ID")
    private Long id;
    
    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50")
    private String roleCode;
    
    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    private String roleName;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人")
    private Integer dataScope;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
    
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
    
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
    
    @Schema(description = "部门ID列表（自定义数据权限使用）")
    private List<Long> deptIds;
}
