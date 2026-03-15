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
 * 字典数据请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "字典数据请求")
public class DictDataRequest {
    
    @Schema(description = "字典数据ID")
    private Long id;
    
    @Schema(description = "字典类型")
    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100")
    private String dictType;
    
    @Schema(description = "字典标签")
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100")
    private String dictLabel;
    
    @Schema(description = "字典值")
    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值长度不能超过100")
    private String dictValue;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "样式属性")
    @Size(max = 100, message = "样式属性长度不能超过100")
    private String cssClass;
    
    @Schema(description = "表格回显样式")
    @Size(max = 100, message = "表格回显样式长度不能超过100")
    private String listClass;
    
    @Schema(description = "是否默认：0-否，1-是")
    private Integer isDefault;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
    
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
