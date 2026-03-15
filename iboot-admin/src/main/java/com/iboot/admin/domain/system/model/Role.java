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
 * 角色实体（聚合根）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    /**
     * 角色 ID
     */
    private Long id;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 角色排序
     */
    private Integer roleSort;
    
    /**
     * 数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
     */
    private Integer dataScope;
    
    /**
     * 菜单 ID 列表
     */
    private List<Long> menuIds;
    
    /**
     * 部门 ID 列表（数据权限）
     */
    private List<Long> deptIds;
    
    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;
    
    /**
     * 创建人
     */
    private String createBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新人
     */
    private String updateBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 启用角色
     *
     * @return 启用后的角色对象
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用角色
     *
     * @return 停用后的角色对象
     */
    public void disable() {
        this.status = 0;
    }
    
    /**
     * 检查角色是否启用
     *
     * @return 如果角色启用则返回true，否则返回false
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
    
    /**
     * 分配菜单权限
     *
     * @param menuIds 菜单ID列表
     */
    public void assignMenus(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
    
    /**
     * 设置数据权限
     *
     * @param dataScope 数据权限范围
     * @param deptIds 部门ID列表
     */
    public void setDataPermission(Integer dataScope, List<Long> deptIds) {
        this.dataScope = dataScope;
        this.deptIds = deptIds;
    }
}
