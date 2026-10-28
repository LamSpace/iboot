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

package com.iboot.ai.application.service.graph;

import com.iboot.ai.domain.graph.GraphSnapshot;
import com.iboot.ai.domain.graph.GraphSyncSummary;
import com.iboot.ai.infrastructure.graph.AdminOrgSnapshotClient;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 组织图谱同步服务。
 *
 * <p>先以调用者身份取得完整快照，再交给仓储在单个事务内整体替换图内容；
 * 因此任一步失败都不会留下半新半旧的数据。同步为单飞执行，并发请求直接拒绝。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class GraphSyncService {

    private final AdminOrgSnapshotClient client;

    private final Neo4jGraphRepository repository;

    private final AiMetrics metrics;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 创建同步服务。
     *
     * @param client     组织快照客户端
     * @param repository 图仓储
     * @param metrics    指标记录
     */
    public GraphSyncService(AdminOrgSnapshotClient client, Neo4jGraphRepository repository, AiMetrics metrics) {
        this.client = client;
        this.repository = repository;
        this.metrics = metrics;
    }

    /**
     * 以调用者身份执行一次同步。
     *
     * @param token 调用者登录令牌
     * @return 同步摘要
     * @throws GraphSyncException 已有同步进行中、下游失败或写入图失败时抛出
     */
    public GraphSyncSummary sync(String token) {
        if (!running.compareAndSet(false, true)) {
            throw new GraphSyncException("已有同步任务进行中，请稍后重试");
        }
        long start = System.nanoTime();
        try {
            GraphSnapshot snapshot = client.fetch(token);
            GraphSyncSummary summary = repository.replace(snapshot);
            metrics.graphSync("success", Duration.ofNanos(System.nanoTime() - start));
            return summary;
        } catch (GraphSyncException e) {
            metrics.graphSync("failure", Duration.ofNanos(System.nanoTime() - start));
            throw e;
        } catch (RuntimeException e) {
            metrics.graphSync("failure", Duration.ofNanos(System.nanoTime() - start));
            throw new GraphSyncException("写入图数据库失败", e);
        } finally {
            running.set(false);
        }
    }
}
