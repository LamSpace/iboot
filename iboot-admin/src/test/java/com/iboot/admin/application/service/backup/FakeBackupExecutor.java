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

import com.iboot.admin.domain.backup.service.BackupExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存备份执行器，供应用服务单元测试使用。
 *
 * <p>按库名返回不同的行数快照，模拟“备份时源库”和“演练后目标库”两个阶段。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class FakeBackupExecutor implements BackupExecutor {

    private final String sourceSchema;

    private final String dumpContent;

    private final Map<String, Long> sourceStats;

    private Map<String, Long> restoredStats;

    private final List<String> recreatedSchemas = new ArrayList<>();

    private byte[] restoredContent;

    private boolean failDump;

    FakeBackupExecutor(final String sourceSchema, final String dumpContent, final Map<String, Long> sourceStats) {
        this.sourceSchema = sourceSchema;
        this.dumpContent = dumpContent;
        this.sourceStats = new LinkedHashMap<>(sourceStats);
        this.restoredStats = new LinkedHashMap<>(sourceStats);
    }

    @Override
    public void dump(final OutputStream target) throws IOException {
        if (failDump) {
            throw new IOException("模拟导出失败 password=s3cret");
        }
        target.write(dumpContent.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void restore(final InputStream source, final String targetSchema) throws IOException {
        restoredContent = source.readAllBytes();
    }

    @Override
    public void recreateSchema(final String schema) {
        recreatedSchemas.add(schema);
    }

    @Override
    public Map<String, Long> tableRowCounts(final String schema) {
        return sourceSchema.equals(schema) ? new LinkedHashMap<>(sourceStats)
                : new LinkedHashMap<>(restoredStats);
    }

    void failDump() {
        this.failDump = true;
    }

    void restoredStats(final Map<String, Long> stats) {
        this.restoredStats = new LinkedHashMap<>(stats);
    }

    List<String> recreatedSchemas() {
        return recreatedSchemas;
    }

    byte[] restoredContent() {
        return restoredContent;
    }
}
