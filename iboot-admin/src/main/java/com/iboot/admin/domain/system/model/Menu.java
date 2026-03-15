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

package com.iboot.admin.domain.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单实体（聚合根）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    
    private Long id;
    private String menuName;
    private Long parentId;
    private String menuType;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Integer orderNum;
    private Integer isFrame;
    private Integer isCache;
    private Integer visible;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;
    private String remark;
    
    /**
     * 子菜单列表（非持久化字段，用于树形结构构建）
     */
    @Builder.Default
    private List<Menu> children = null;
    
    /**
     * 检查是否为目录类型菜单
     *
     * @return 如果是目录类型则返回true，否则返回false
     */
    public boolean isDirectory() {
        return "M".equals(this.menuType);
    }
    
    /**
     * 检查是否为菜单类型
     *
     * @return 如果是菜单类型则返回true，否则返回false
     */
    public boolean isMenu() {
        return "C".equals(this.menuType);
    }
    
    /**
     * 检查是否为按钮类型
     *
     * @return 如果是按钮类型则返回true，否则返回false
     */
    public boolean isButton() {
        return "F".equals(this.menuType);
    }
    
    /**
     * 检查菜单是否可见
     *
     * @return 如果菜单可见则返回true，否则返回false
     */
    public boolean isVisible() {
        return this.visible != null && this.visible == 1;
    }
    
    /**
     * 启用菜单
     *
     * @return 启用后的菜单对象
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用菜单
     *
     * @return 停用后的菜单对象
     */
    public void disable() {
        this.status = 0;
    }
}
