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
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件信息更新请求 DTO
 * 
 * @author iBoot
 */
@Data
@Schema(description = "文件信息更新请求")
public class FileUpdateRequest {
    
    @Schema(description = "文件ID")
    private Long id;
    
    @Schema(description = "文件名称")
    @Size(max = 255, message = "文件名称长度不能超过255")
    private String fileName;
    
    @Schema(description = "文件分类")
    @Size(max = 100, message = "文件分类长度不能超过100")
    private String fileCategory;
    
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
