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
 * 操作日志实体（值对象）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperateLog {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 模块标题
     */
    private String title;
    
    /**
     * 业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
     */
    private Integer businessType;
    
    /**
     * 方法名称
     */
    private String method;
    
    /**
     * 请求方式
     */
    private String requestMethod;
    
    /**
     * 操作类别：0-其它，1-后台用户，2-手机端用户
     */
    private Integer operatorType;
    
    /**
     * 操作人员
     */
    private String operatorName;
    
    /**
     * 部门名称
     */
    private String deptName;
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * 主机地址
     */
    private String ip;
    
    /**
     * 操作地点
     */
    private String location;
    
    /**
     * 请求参数
     */
    private String param;
    
    /**
     * 返回参数
     */
    private String result;
    
    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;
    
    /**
     * 错误消息
     */
    private String errorMsg;
    
    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
    
    /**
     * 耗时（毫秒）
     */
    private Long costTime;
    
    /**
     * 检查是否操作成功
     */
    public boolean isSuccess() {
        return this.status != null && this.status == 1;
    }
}
