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

package com.iboot.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识图谱配置。
 *
 * <p>绑定 {@code iboot.ai.graph.*}，覆盖 Neo4j 连接、同步翻页与查询上限。
 * 默认关闭：关闭时不建立图数据库连接，也不注册图查询工具。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.ai.graph")
public class GraphProperties {

    /**
     * 是否启用知识图谱能力。
     */
    private boolean enabled = false;

    /**
     * Neo4j 连接地址。
     */
    private String uri = "bolt://localhost:7687";

    /**
     * Neo4j 用户名。
     */
    private String username = "neo4j";

    /**
     * Neo4j 密码。
     */
    private String password = "";

    /**
     * 使用的数据库名。
     */
    private String database = "neo4j";

    /**
     * 同步配置。
     */
    private Sync sync = new Sync();

    /**
     * 查询配置。
     */
    private Query query = new Query();

    /**
     * 同步配置。
     *
     * <p>控制从 admin 拉取分页数据时的页大小与最大页数，防止异常数据导致无限翻页。
     */
    public static class Sync {

        /**
         * 单页拉取条数。
         */
        private int pageSize = 200;

        /**
         * 最大翻页数。
         */
        private int maxPages = 50;

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }

    /**
     * 查询配置。
     *
     * <p>控制单次图查询返回行数与超时时间，避免模型诱导出超大结果集。
     */
    public static class Query {

        /**
         * 单次查询返回的最大行数。
         */
        private int maxRows = 50;

        /**
         * 查询超时秒数。
         */
        private long timeoutSeconds = 10L;

        public int getMaxRows() {
            return maxRows;
        }

        public void setMaxRows(int maxRows) {
            this.maxRows = maxRows;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }
}
