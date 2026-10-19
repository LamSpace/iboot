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
 * 匿名访问标记。
 *
 * <p>标记在 Controller 类或方法上，表示对应接口跳过认证过滤器，允许未登录访问；
 * 通常用于登录、刷新令牌等公开接口。
 *
 * <p>该注解只影响认证，不改变授权规则；标记后仍需确认接口不会暴露敏感数据。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {

}
