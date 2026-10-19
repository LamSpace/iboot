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

package com.iboot.admin.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤标记。
 *
 * <p>标记在查询方法或类上，由 {@code DataScopeInterceptor} 根据当前用户所有角色的
 * 数据范围向 SQL 追加部门或用户过滤条件。{@code deptAlias} 和 {@code userAlias}
 * 用于指定 SQL 中部门表、用户表的别名。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表在 SQL 中的别名，默认 {@code d}。
     */
    String deptAlias() default "d";

    /**
     * 用户表在 SQL 中的别名，默认 {@code u}。
     */
    String userAlias() default "u";

}
