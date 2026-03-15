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

package com.iboot.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 * 
 * @author iBoot
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {
    
    /**
     * 系统用户
     */
    SYSTEM(1, "系统用户"),
    
    /**
     * 普通用户
     */
    NORMAL(2, "普通用户");
    
    /**
     * 用户类型码
     */
    private final Integer code;
    
    /**
     * 用户类型描述
     */
    private final String description;
}
