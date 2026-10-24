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

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BackupCipher} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class BackupCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(keyBytes((byte) 7));

    /**
     * 验证加密后可还原出完全相同的明文。
     */
    @Test
    void roundTripPreservesContent() throws Exception {
        byte[] plaintext = "CREATE TABLE demo (id BIGINT);\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
        try (var output = cipher().encrypting(ciphertext)) {
            output.write(plaintext);
        }

        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        cipher().decrypt(new ByteArrayInputStream(ciphertext.toByteArray()), decrypted);

        assertThat(decrypted.toByteArray()).isEqualTo(plaintext);
        assertThat(ciphertext.toByteArray()).isNotEqualTo(plaintext);
        assertThat(new String(ciphertext.toByteArray(), StandardCharsets.ISO_8859_1))
                .doesNotContain("CREATE TABLE");
    }

    /**
     * 验证空内容也能正常加密并通过认证。
     */
    @Test
    void emptyContentRoundTrip() throws Exception {
        ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
        try (var output = cipher().encrypting(ciphertext)) {
            // 不写入任何内容
        }

        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        cipher().decrypt(new ByteArrayInputStream(ciphertext.toByteArray()), decrypted);

        assertThat(decrypted.size()).isZero();
    }

    /**
     * 验证篡改密文后认证失败。
     */
    @Test
    void tamperedCiphertextFails() throws Exception {
        byte[] ciphertext = encrypt("sensitive");
        ciphertext[ciphertext.length - 1] ^= 0x01;

        assertThatThrownBy(() -> cipher().decrypt(new ByteArrayInputStream(ciphertext),
                new ByteArrayOutputStream()))
                .isInstanceOf(BackupIntegrityException.class);
    }

    /**
     * 验证使用错误密钥时认证失败。
     */
    @Test
    void wrongKeyFails() throws Exception {
        byte[] ciphertext = encrypt("sensitive");
        BackupCipher other = BackupCipher.fromBase64(
                Base64.getEncoder().encodeToString(keyBytes((byte) 9)));

        assertThatThrownBy(() -> other.decrypt(new ByteArrayInputStream(ciphertext),
                new ByteArrayOutputStream()))
                .isInstanceOf(BackupIntegrityException.class);
    }

    /**
     * 验证密钥长度不为 32 字节时拒绝创建。
     */
    @Test
    void invalidKeyLengthRejected() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> BackupCipher.fromBase64(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IBOOT_BACKUP_ENCRYPTION_KEY");
    }

    /**
     * 验证密钥缺失时拒绝创建。
     */
    @Test
    void missingKeyRejected() {
        assertThatThrownBy(() -> BackupCipher.fromBase64(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IBOOT_BACKUP_ENCRYPTION_KEY");
    }

    private byte[] encrypt(final String plaintext) throws Exception {
        ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
        try (var output = cipher().encrypting(ciphertext)) {
            output.write(plaintext.getBytes(StandardCharsets.UTF_8));
        }
        return ciphertext.toByteArray();
    }

    private BackupCipher cipher() {
        return BackupCipher.fromBase64(KEY);
    }

    private static byte[] keyBytes(final byte value) {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, value);
        return key;
    }
}
