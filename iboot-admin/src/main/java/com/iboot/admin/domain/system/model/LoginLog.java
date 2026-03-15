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
 * 登录日志实体（值对象）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog {
    
    /**
     * 日志ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 登录IP
     */
    private String ipAddress;
    
    /**
     * 登录地点
     */
    private String loginLocation;
    
    /**
     * 浏览器类型
     */
    private String browser;
    
    /**
     * 操作系统
     */
    private String os;
    
    /**
     * 登录状态：0-失败，1-成功
     */
    private Integer status;
    
    /**
     * 提示消息
     */
    private String message;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 检查是否登录成功
     */
    public boolean isSuccess() {
        return this.status != null && this.status == 1;
    }
}
