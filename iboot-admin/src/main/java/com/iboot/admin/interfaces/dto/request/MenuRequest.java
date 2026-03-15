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
 * 菜单请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "菜单请求")
public class MenuRequest {
    
    @Schema(description = "菜单ID")
    private Long id;
    
    @Schema(description = "父菜单ID")
    private Long parentId;
    
    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    private String menuName;
    
    @Schema(description = "菜单类型：M-目录，C-菜单，F-按钮")
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;
    
    @Schema(description = "路由地址")
    @Size(max = 200, message = "路由地址长度不能超过200")
    private String path;
    
    @Schema(description = "组件路径")
    @Size(max = 255, message = "组件路径长度不能超过255")
    private String component;
    
    @Schema(description = "权限标识")
    @Size(max = 100, message = "权限标识长度不能超过100")
    private String perms;
    
    @Schema(description = "菜单图标")
    @Size(max = 100, message = "菜单图标长度不能超过100")
    private String icon;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "是否为外链：0-否，1-是")
    private Integer isFrame;
    
    @Schema(description = "是否缓存：0-否，1-是")
    private Integer isCache;
    
    @Schema(description = "是否可见：0-隐藏，1-显示")
    private Integer visible;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
    
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
