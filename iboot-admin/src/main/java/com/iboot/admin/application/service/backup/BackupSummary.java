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

import java.time.LocalDateTime;

/**
 * 备份概览。
 *
 * @param enabled      备份功能是否启用
 * @param totalCount   备份记录总数
 * @param totalSize    成功备份的密文总大小
 * @param lastSuccessAt 最近一次成功备份的结束时间
 * @author iBoot Team
 * @since 1.0.0
 */
public record BackupSummary(boolean enabled, long totalCount, long totalSize, LocalDateTime lastSuccessAt) {
}
