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

package com.iboot.admin.domain.backup.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 数据库备份与还原执行器。
 *
 * <p>抽象数据库导出、还原、演练库重建和逐表行数统计。本地实现通过容器内的
 * {@code mysqldump}/{@code mysql} 执行，云端可以替换为镜像内客户端或云数据库 API，
 * 上层备份编排与校验逻辑不感知执行环境差异。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface BackupExecutor {

    /**
     * 将源数据库导出为逻辑 SQL 明文并写入目标流。
     *
     * @param target 明文目标流
     * @throws IOException 命令执行失败、数据库不可达或写入失败时抛出
     */
    void dump(OutputStream target) throws IOException;

    /**
     * 将逻辑 SQL 明文还原到指定库。
     *
     * @param source       明文输入流
     * @param targetSchema 目标库名
     * @throws IOException 命令执行失败、目标库不可写或读取中断时抛出
     */
    void restore(InputStream source, String targetSchema) throws IOException;

    /**
     * 删除并重建指定库。
     *
     * @param schema 目标库名
     * @throws IOException 数据库连接或 DDL 执行失败时抛出
     */
    void recreateSchema(String schema) throws IOException;

    /**
     * 统计指定库中所有基础表的行数。
     *
     * @param schema 库名
     * @return 表名到行数的有序映射
     * @throws IOException 数据库连接或查询失败时抛出
     */
    Map<String, Long> tableRowCounts(String schema) throws IOException;
}
