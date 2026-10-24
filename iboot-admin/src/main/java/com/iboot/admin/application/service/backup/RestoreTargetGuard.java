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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.application.service.backup;

import com.iboot.admin.common.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * 恢复目标库守卫。
 *
 * <p>恢复演练只允许写入带固定前缀的独立演练库，且不得与业务库同名。守卫在服务层
 * 强制校验，接口不接受客户端传入的目标库名，从设计上排除覆盖业务库的可能。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class RestoreTargetGuard {

    /**
     * 合法库名：仅字母、数字与下划线。
     */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");

    private RestoreTargetGuard() {
    }

    /**
     * 校验恢复目标库名。
     *
     * @param schema           目标库名
     * @param prefix           允许的库名前缀
     * @param businessDatabase 业务库名
     * @throws BusinessException 库名非法、前缀不匹配或与业务库同名时抛出
     */
    public static void requireValid(final String schema, final String prefix, final String businessDatabase) {
        if (schema == null || !IDENTIFIER.matcher(schema).matches()) {
            throw new BusinessException("恢复目标库名不合法: " + schema);
        }
        if (prefix == null || prefix.isBlank() || !schema.startsWith(prefix)) {
            throw new BusinessException("恢复目标必须位于 " + prefix + " 前缀下");
        }
        if (schema.equals(businessDatabase)) {
            throw new BusinessException("恢复目标不得为业务库: " + schema);
        }
    }
}
