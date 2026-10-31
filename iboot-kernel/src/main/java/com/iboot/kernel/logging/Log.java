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

package com.iboot.kernel.logging;


import java.lang.annotation.*;

/**
 * 操作日志标记。
 *
 * <p>标记在 Controller 方法上，由 {@code LogAspect} 记录模块名称、业务类型、
 * 请求参数、响应数据、执行耗时和操作人等信息。敏感字段应在请求 DTO
 * 或切面配置中排除。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.kernel.logging.LogAspect
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 日志中记录的模块名称。
     */
    String title() default "";

    /**
     * 业务操作类型，默认 {@link BusinessTypeEnum#OTHER}。
     */
    BusinessTypeEnum businessType() default BusinessTypeEnum.OTHER;

    /**
     * 是否记录请求参数。
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否记录响应数据。
     */
    boolean isSaveResponseData() default true;

}
