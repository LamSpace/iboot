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
import java.util.List;

/**
 * 用户实体（聚合根）
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户 ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号码
     */
    private String phone;
    
    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;
    
    /**
     * 头像地址
     */
    private String avatar;
    
    /**
     * 用户类型：1-系统用户，2-普通用户
     */
    private Integer userType;
    
    /**
     * 部门 ID
     */
    private Long deptId;
    
    /**
     * 岗位 ID 列表
     */
    private List<Long> postIds;
    
    /**
     * 角色 ID 列表
     */
    private List<Long> roleIds;
    
    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;
    
    /**
     * 最后登录 IP
     */
    private String loginIp;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime loginTime;
    
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
     * 修改用户密码
     *
     * @param newPassword 新密码
     */
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
    
    /**
     * 启用用户
     *
     * @return 启用后的用户对象
     */
    public void enable() {
        this.status = 1;
    }
    
    /**
     * 停用用户
     *
     * @return 停用后的用户对象
     */
    public void disable() {
        this.status = 0;
    }
    
    /**
     * 检查用户是否启用
     *
     * @return 如果用户启用则返回true，否则返回false
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
    
    /**
     * 更新登录信息
     *
     * @param loginIp 登录IP地址
     * @param loginTime 登录时间
     */
    public void updateLoginInfo(String loginIp, LocalDateTime loginTime) {
        this.loginIp = loginIp;
        this.loginTime = loginTime;
    }
}
