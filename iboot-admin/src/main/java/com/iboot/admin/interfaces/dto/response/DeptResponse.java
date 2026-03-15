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
 * 部门响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "部门响应")
public class DeptResponse {
    
    @Schema(description = "部门ID")
    private Long id;
    
    @Schema(description = "父部门ID")
    private Long parentId;
    
    @Schema(description = "部门编码")
    private String deptCode;
    
    @Schema(description = "部门名称")
    private String deptName;
    
    @Schema(description = "显示顺序")
    private Integer orderNum;
    
    @Schema(description = "负责人")
    private String leader;
    
    @Schema(description = "负责人电话")
    private String phone;
    
    @Schema(description = "负责人邮箱")
    private String email;
    
    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "子部门列表")
    private List<DeptResponse> children;
}
