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

/**
 * 操作日志响应 DTO
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "操作日志响应")
public class OperateLogResponse {
    
    @Schema(description = "日志ID")
    private Long id;
    
    @Schema(description = "模块标题")
    private String title;
    
    @Schema(description = "业务类型：0-其它，1-新增，2-修改，3-删除，4-授权，5-导出，6-导入，7-强退，8-清空")
    private Integer businessType;
    
    @Schema(description = "请求方法")
    private String method;
    
    @Schema(description = "请求方式")
    private String requestMethod;
    
    @Schema(description = "操作人员")
    private String operatorName;
    
    @Schema(description = "部门名称")
    private String deptName;
    
    @Schema(description = "请求URL")
    private String operUrl;
    
    @Schema(description = "操作IP")
    private String operIp;
    
    @Schema(description = "操作地点")
    private String operLocation;
    
    @Schema(description = "请求参数")
    private String operParam;
    
    @Schema(description = "返回结果")
    private String jsonResult;
    
    @Schema(description = "操作状态：0-失败，1-成功")
    private Integer status;
    
    @Schema(description = "错误消息")
    private String errorMsg;
    
    @Schema(description = "操作耗时（毫秒）")
    private Long costTime;
    
    @Schema(description = "操作时间")
    private LocalDateTime operTime;
}
