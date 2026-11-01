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

package com.iboot.dbinit;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;

/**
 * 数据库迁移入口，供初始化进程与集成测试共用。
 *
 * <p>迁移脚本随本模块分发，是项目唯一的建表来源；服务进程不再执行迁移。
 * 传入 schema 时会先创建该 schema，便于测试在空库上建立结构。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SchemaMigrator {

    private SchemaMigrator() {
    }

    /**
     * 在当前数据库内执行全部未应用的迁移。
     *
     * @param dataSource 数据源
     * @return 迁移结果，包含本次应用的迁移数量
     */
    public static MigrateResult migrate(DataSource dataSource) {
        return migrate(dataSource, null);
    }

    /**
     * 在指定 schema 内执行全部未应用的迁移，schema 不存在时创建。
     *
     * @param dataSource 数据源
     * @param schema     目标 schema；为 null 或空白时使用连接自身的数据库
     * @return 迁移结果，包含本次应用的迁移数量
     */
    public static MigrateResult migrate(DataSource dataSource, String schema) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .cleanDisabled(true);
        if (schema != null && !schema.isBlank()) {
            configuration.schemas(schema).defaultSchema(schema).createSchemas(true);
        }
        return configuration.load().migrate();
    }
}
