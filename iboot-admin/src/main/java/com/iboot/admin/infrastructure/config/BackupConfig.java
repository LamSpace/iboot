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

package com.iboot.admin.infrastructure.config;

import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据备份基础设施配置。
 *
 * <p>根据功能开关创建加解密组件：启用时必须提供合法的 32 字节主密钥，否则应用启动失败；
 * 关闭时使用占位组件，保证未配置密钥的环境可以正常启动。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see BackupCipher
 */
@Configuration
public class BackupConfig {

    /**
     * 创建备份加解密组件。
     *
     * @param properties 备份配置
     * @return 加解密组件
     * @throws IllegalStateException 功能启用但密钥缺失、无法解码或长度不为 32 字节时抛出
     */
    @Bean
    public BackupCipher backupCipher(final BackupProperties properties) {
        if (!properties.isEnabled()) {
            return BackupCipher.disabled();
        }
        return BackupCipher.fromBase64(properties.getEncryptionKey());
    }
}
