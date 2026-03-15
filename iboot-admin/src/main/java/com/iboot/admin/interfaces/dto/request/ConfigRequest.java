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
 * 配置请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "配置请求")
public class ConfigRequest {
    
    @Schema(description = "配置ID")
    private Long id;
    
    @Schema(description = "配置名称")
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100")
    private String configName;
    
    @Schema(description = "配置键")
    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过100")
    private String configKey;
    
    @Schema(description = "配置值")
    @NotBlank(message = "配置值不能为空")
    @Size(max = 500, message = "配置值长度不能超过500")
    private String configValue;
    
    @Schema(description = "配置类型：0-默认，1-系统内置")
    private Integer configType;
    
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
