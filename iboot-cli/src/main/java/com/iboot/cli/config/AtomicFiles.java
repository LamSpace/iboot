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

package com.iboot.cli.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * 配置文件原子写入工具。
 *
 * <p>先写同目录临时文件并设置权限，再移动到目标路径；支持 POSIX 的平台使用
 * {@code ATOMIC_MOVE}，不支持时回退为普通替换。写入失败不会留下半截目标文件。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class AtomicFiles {

    private static final Set<PosixFilePermission> SECRET_PERMISSIONS =
            PosixFilePermissions.fromString("rw-------");

    private AtomicFiles() {
    }

    /**
     * 原子写入内容。
     *
     * @param target 目标文件
     * @param content 文本内容
     * @throws IOException 写入失败时抛出
     */
    public static void writeSecret(Path target, String content) throws IOException {
        Path directory = target.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        secureDirectory(directory);
        Path temporary = Files.createTempFile(directory, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            secureFile(temporary);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            secureFile(target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void secureDirectory(Path directory) throws IOException {
        try {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException ignored) {
            // 非 POSIX 平台没有该权限模型。
        }
    }

    private static void secureFile(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, SECRET_PERMISSIONS);
        } catch (UnsupportedOperationException ignored) {
            // 非 POSIX 平台没有该权限模型。
        }
    }
}
