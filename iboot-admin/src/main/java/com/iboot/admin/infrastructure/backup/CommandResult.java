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

/**
 * 外部命令执行结果。
 *
 * @param exitCode 进程退出码，{@code 0} 表示成功
 * @param stderr   标准错误输出，已限制长度，调用方负责脱敏
 * @author iBoot Team
 * @since 1.0.0
 */
public record CommandResult(int exitCode, String stderr) {
}
