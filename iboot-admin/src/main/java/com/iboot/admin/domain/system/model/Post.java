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

/**
 * 岗位实体（聚合根）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    /**
     * 岗位ID
     */
    private Long id;
    
    /**
     * 岗位编码
     */
    private String postCode;
    
    /**
     * 岗位名称
     */
    private String postName;
    
    /**
     * 岗位排序
     */
    private Integer postSort;
    
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
     * 启用岗位
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用岗位
     */
    public void disable() {
        this.status = 0;
    }
    
    /**
     * 检查岗位是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
}
