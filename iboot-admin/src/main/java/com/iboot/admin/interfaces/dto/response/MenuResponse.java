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
 * 菜单响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜单响应")
public class MenuResponse {
    
    @Schema(description = "菜单ID")
    private Long id;
    
    @Schema(description = "父菜单ID")
    private Long parentId;
    
    @Schema(description = "菜单名称")
    private String menuName;
    
    @Schema(description = "菜单类型：M-目录，C-菜单，F-按钮")
    private String menuType;
    
    @Schema(description = "路由地址")
    private String path;
    
    @Schema(description = "组件路径")
    private String component;
    
    @Schema(description = "权限标识")
    private String perms;
    
    @Schema(description = "菜单图标")
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
    private String remark;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "子菜单列表")
    private List<MenuResponse> children;
}
