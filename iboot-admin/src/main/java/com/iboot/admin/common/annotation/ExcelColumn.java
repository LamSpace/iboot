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
 * Excel列注解，用于标记导出字段
 * 
 * @author iBoot
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {
    
    /**
     * 列名称
     */
    String name();
    
    /**
     * 列排序（从小到大）
     */
    int order() default 0;
    
    /**
     * 日期格式化（仅对日期类型有效）
     */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 字典类型（用于字典值翻译）
     */
    String dictType() default "";
    
    /**
     * 列宽度（字符数）
     */
    int width() default 0;
}
