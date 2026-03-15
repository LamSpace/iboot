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
 * 角色响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色响应")
public class RoleResponse {
    
    @Schema(description = "角色ID")
    private Long id;
    
    @Schema(description = "角色编码")
    private String roleCode;
    
    @Schema(description = "角色名称")
    private String roleName;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人")
    private Integer dataScope;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
    
    @Schema(description = "备注")
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
    
    @Schema(description = "部门ID列表")
    private List<Long> deptIds;
}
