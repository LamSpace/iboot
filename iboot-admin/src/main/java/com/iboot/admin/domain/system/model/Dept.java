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
import java.util.ArrayList;
import java.util.List;

/**
 * 部门实体（聚合根）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dept {
    
    /**
     * 部门ID
     */
    private Long id;
    
    /**
     * 父部门ID
     */
    private Long parentId;
    
    /**
     * 部门名称
     */
    private String deptName;
    
    /**
     * 部门编码
     */
    private String deptCode;
    
    /**
     * 显示顺序
     */
    private Integer orderNum;
    
    /**
     * 负责人
     */
    private String leader;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
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
     * 子部门列表（用于树形结构）
     */
    @Builder.Default
    private List<Dept> children = new ArrayList<>();
    
    /**
     * 启用部门
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用部门
     */
    public void disable() {
        this.status = 0;
    }
    
    /**
     * 检查部门是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
    
    /**
     * 更新负责人信息
     */
    public void updateLeader(String leader, String phone, String email) {
        this.leader = leader;
        this.phone = phone;
        this.email = email;
    }
    
    /**
     * 检查是否为顶级部门
     */
    public boolean isRoot() {
        return this.parentId == null || this.parentId == 0L;
    }
}
