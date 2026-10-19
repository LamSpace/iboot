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
 * Excel 导出列标记。
 *
 * <p>标记在导出 VO 的字段上，{@code ExcelExportUtil} 会收集带该注解的字段，
 * 按 {@link #order()} 升序生成表头和单元格；未标记的字段不会出现在导出文件中。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.admin.common.util.ExcelExportUtil
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * 表头名称。
     */
    String name();

    /**
     * 列顺序，数值越小越靠前；相同值按字段声明顺序排列。
     */
    int order() default 0;

    /**
     * 日期格式化模板，仅对日期类型字段生效。
     */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss";

    /**
     * 字典类型。配置后导出时会将字典值翻译为字典标签。
     */
    String dictType() default "";

    /**
     * 列宽，单位为字符数；{@code 0} 表示使用默认宽度。
     */
    int width() default 0;

}
