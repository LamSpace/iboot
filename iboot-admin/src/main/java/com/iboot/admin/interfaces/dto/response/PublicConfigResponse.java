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

/**
 * 公开系统配置响应 DTO
 * 用于前端登录页、页头等场景获取系统公开配置
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "公开系统配置响应")
public class PublicConfigResponse {
    
    @Schema(description = "系统名称")
    private String systemName;
    
    @Schema(description = "是否允许用户注册")
    private Boolean registerEnabled;
}
