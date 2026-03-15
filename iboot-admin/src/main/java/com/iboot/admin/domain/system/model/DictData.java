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
 * 字典数据实体
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictData {
    
    /**
     * 字典数据ID
     */
    private Long id;
    
    /**
     * 字典类型
     */
    private String dictType;
    
    /**
     * 字典标签
     */
    private String dictLabel;
    
    /**
     * 字典键值
     */
    private String dictValue;
    
    /**
     * 字典排序
     */
    private Integer dictSort;
    
    /**
     * 样式属性
     */
    private String cssClass;
    
    /**
     * 表格回显样式
     */
    private String listClass;
    
    /**
     * 是否默认：0-否，1-是
     */
    private Integer isDefault;
    
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
     * 设为默认
     */
    public void setAsDefault() {
        this.isDefault = 1;
    }
    
    /**
     * 取消默认
     */
    public void unsetDefault() {
        this.isDefault = 0;
    }
    
    /**
     * 检查是否为默认值
     */
    public boolean isDefaultValue() {
        return this.isDefault != null && this.isDefault == 1;
    }
    
    /**
     * 启用
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用
     */
    public void disable() {
        this.status = 0;
    }
}
