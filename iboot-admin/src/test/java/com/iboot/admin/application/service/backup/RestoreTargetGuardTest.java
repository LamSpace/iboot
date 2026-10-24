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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RestoreTargetGuard} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RestoreTargetGuardTest {

    private static final String PREFIX = "iboot_restore_";

    private static final String BUSINESS = "iboot_admin";

    /**
     * 验证固定演练库通过校验。
     */
    @Test
    void acceptsDrillSchema() {
        assertThatCode(() -> RestoreTargetGuard.requireValid("iboot_restore_drill", PREFIX, BUSINESS))
                .doesNotThrowAnyException();
    }

    /**
     * 验证业务库被拒绝。
     */
    @Test
    void rejectsBusinessDatabase() {
        assertThatThrownBy(() -> RestoreTargetGuard.requireValid("iboot_admin", PREFIX, BUSINESS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前缀");
    }

    /**
     * 验证无前缀库名被拒绝。
     */
    @Test
    void rejectsSchemaWithoutPrefix() {
        assertThatThrownBy(() -> RestoreTargetGuard.requireValid("restore_drill", PREFIX, BUSINESS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("前缀");
    }

    /**
     * 验证注入字符被拒绝。
     */
    @Test
    void rejectsIllegalCharacters() {
        assertThatThrownBy(() -> RestoreTargetGuard.requireValid(
                "iboot_restore_drill; DROP DATABASE iboot_admin", PREFIX, BUSINESS))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不合法");
    }
}
