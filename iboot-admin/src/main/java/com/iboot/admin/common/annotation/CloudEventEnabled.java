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
 * CloudEvents 启用注解
 * <p>
 * 标记在 Controller 类或方法上，表示该接口的响应需要包装为 CloudEvents 格式
 * <p>
 * 使用方式： <pre>
 * {@code @RestController}
 * {@code @RequestMapping("/api/user")}
 * {@code @CloudEventEnabled} // 标记整个 Controller
 * public class UserController {
 *     // 所有接口都会输出 CloudEvents 格式
 * }
 *
 * {@code @RestController}
 * {@code @RequestMapping("/api/user")}
 * public class UserController {
 *     {@code @CloudEventEnabled} // 仅标记该方法
 *     public Result<User> getUser() {
 *         // 该接口会输出 CloudEvents 格式
 *     }
 * }
 * </pre>
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CloudEventEnabled {

    /**
     * 事件源路径，默为空
     * <p>
     * 如果不指定，将自动从请求路径推导
     */
    String source() default "";

    /**
     * 事件类型，默认为空
     * <p>
     * 如果不指定，将根据响应结果自动推导（成功/失败）
     */
    String type() default "";

}
