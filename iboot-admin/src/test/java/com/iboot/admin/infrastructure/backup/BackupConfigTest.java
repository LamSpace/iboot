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

package com.iboot.admin.infrastructure.backup;

import com.iboot.admin.infrastructure.config.BackupConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BackupConfig} 的单元测试。
 *
 * <p>验证功能启用时密钥非法会阻止上下文启动，关闭时缺少密钥仍可正常启动。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupConfigTest {

    /**
     * 验证启用但密钥非法时上下文启动失败，并提示环境变量名。
     */
    @Test
    void failsWhenEnabledWithInvalidKey() {
        runner(properties(true, "not-base64")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasMessageContaining("IBOOT_BACKUP_ENCRYPTION_KEY");
        });
    }

    /**
     * 验证启用且密钥合法时创建加解密组件。
     */
    @Test
    void createsCipherWhenEnabledWithValidKey() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);

        runner(properties(true, key)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(BackupCipher.class)).isNotNull();
        });
    }

    /**
     * 验证功能关闭时未配置密钥也能正常启动。
     */
    @Test
    void startsWhenDisabledWithoutKey() {
        runner(properties(false, null)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(BackupCipher.class)).isNotNull();
        });
    }

    private ApplicationContextRunner runner(final BackupProperties properties) {
        return new ApplicationContextRunner()
                .withUserConfiguration(BackupConfig.class)
                .withBean(BackupProperties.class, () -> properties);
    }

    private BackupProperties properties(final boolean enabled, final String key) {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(enabled);
        properties.setEncryptionKey(key);
        return properties;
    }
}
