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

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 备份产物的 AES-256-GCM 加解密组件。
 *
 * <p>密文文件自带头部：magic、版本、IV 长度与 IV，其后是密文和 16 字节认证标签。
 * 加密时先写头部再流式写入密文；解密时先校验头部，再显式调用 {@code update/doFinal}
 * 读到 EOF，确保认证标签失败时能够抛出 {@link BackupIntegrityException}，而不是静默截断。
 *
 * <p>主密钥只从环境变量读取，Base64 解码后必须为 32 字节。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class BackupCipher {

    /**
     * 密文文件头 magic。
     */
    private static final byte[] MAGIC = "IBOOTBAK".getBytes(StandardCharsets.US_ASCII);

    /**
     * 当前文件格式版本。
     */
    private static final byte VERSION = 1;

    /**
     * GCM IV 长度（字节）。
     */
    private static final int IV_LENGTH = 12;

    /**
     * GCM 认证标签长度（位）。
     */
    private static final int TAG_BITS = 128;

    /**
     * 流式处理缓冲区大小。
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * AES-256 密钥长度（字节）。
     */
    private static final int KEY_LENGTH = 32;

    /**
     * 加密主密钥。
     */
    private final SecretKeySpec key;

    /**
     * IV 随机源。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    private BackupCipher(final byte[] keyBytes) {
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 从 Base64 字符串创建加解密组件。
     *
     * @param value Base64 编码的 32 字节密钥
     * @return 加解密组件
     * @throws IllegalStateException 密钥缺失、无法解码或长度不为 32 字节时抛出
     */
    public static BackupCipher fromBase64(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("备份已启用但未配置 IBOOT_BACKUP_ENCRYPTION_KEY");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("IBOOT_BACKUP_ENCRYPTION_KEY 不是合法的 Base64 字符串", e);
        }
        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalStateException("IBOOT_BACKUP_ENCRYPTION_KEY 解码后必须为 32 字节，实际为 "
                    + keyBytes.length + " 字节");
        }
        return new BackupCipher(keyBytes);
    }

    /**
     * 创建仅用于功能关闭状态的占位组件。
     *
     * <p>功能关闭时不会执行任何加解密操作；该实例只用于满足依赖注入。
     *
     * @return 占位加解密组件
     */
    public static BackupCipher disabled() {
        return new BackupCipher(new byte[KEY_LENGTH]);
    }

    /**
     * 创建加密输出流。
     *
     * <p>返回的流在构造时写入文件头，写入的数据经 GCM 加密后传给目标流，
     * 关闭时完成认证标签写入。调用方必须关闭该流，否则密文不完整。
     *
     * @param target 密文目标流
     * @return 接收明文的输出流
     * @throws IOException 初始化加密器或写入文件头失败时抛出
     */
    public OutputStream encrypting(final OutputStream target) throws IOException {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            target.write(MAGIC);
            target.write(VERSION);
            target.write(IV_LENGTH);
            target.write(iv);
            return new EncryptingOutputStream(cipher, target);
        } catch (GeneralSecurityException e) {
            throw new IOException("初始化备份加密器失败", e);
        }
    }

    /**
     * 解密备份对象并把明文写入目标流。
     *
     * @param source 密文输入流
     * @param target 明文目标流
     * @throws IOException              读取、头部校验或标签校验失败时抛出
     * @throws BackupIntegrityException 文件头非法、数据截断或认证标签不匹配时抛出
     */
    public void decrypt(final InputStream source, final OutputStream target) throws IOException {
        byte[] magic = new byte[MAGIC.length];
        readFully(source, magic);
        if (!Arrays.equals(MAGIC, magic)) {
            throw new BackupIntegrityException("备份文件头不合法");
        }
        int version = source.read();
        if (version != VERSION) {
            throw new BackupIntegrityException("不支持的备份格式版本: " + version);
        }
        int ivLength = source.read();
        if (ivLength != IV_LENGTH) {
            throw new BackupIntegrityException("备份文件 IV 长度不合法: " + ivLength);
        }
        byte[] iv = new byte[IV_LENGTH];
        readFully(source, iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = source.read(buffer)) != -1) {
                byte[] plain = cipher.update(buffer, 0, read);
                if (plain != null && plain.length > 0) {
                    target.write(plain);
                }
            }
            byte[] finalPlain = cipher.doFinal();
            if (finalPlain != null && finalPlain.length > 0) {
                target.write(finalPlain);
            }
            target.flush();
        } catch (AEADBadTagException e) {
            throw new BackupIntegrityException("备份对象认证失败，文件可能被篡改或密钥不匹配", e);
        } catch (GeneralSecurityException e) {
            throw new BackupIntegrityException("解密备份对象失败", e);
        }
    }

    private static void readFully(final InputStream source, final byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = source.read(buffer, offset, buffer.length - offset);
            if (read == -1) {
                throw new BackupIntegrityException("备份文件头被截断");
            }
            offset += read;
        }
    }

    /**
     * 加密输出流。
     *
     * <p>写入的明文字节立即经 {@link Cipher#update} 加密并转发到目标流，关闭时调用
     * {@link Cipher#doFinal} 写入认证标签。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    private static final class EncryptingOutputStream extends OutputStream {

        /**
         * 加密器。
         */
        private final Cipher cipher;

        /**
         * 密文目标流。
         */
        private final OutputStream target;

        /**
         * 是否已关闭。
         */
        private boolean closed;

        private EncryptingOutputStream(final Cipher cipher, final OutputStream target) {
            this.cipher = cipher;
            this.target = target;
        }

        @Override
        public void write(final int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (closed) {
                throw new IOException("输出流已关闭");
            }
            byte[] cipherBytes = cipher.update(b, off, len);
            if (cipherBytes != null && cipherBytes.length > 0) {
                target.write(cipherBytes);
            }
        }

        @Override
        public void flush() throws IOException {
            target.flush();
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                byte[] finalBytes = cipher.doFinal();
                if (finalBytes != null && finalBytes.length > 0) {
                    target.write(finalBytes);
                }
            } catch (GeneralSecurityException e) {
                throw new IOException("完成备份加密失败", e);
            } finally {
                target.close();
            }
        }
    }
}
