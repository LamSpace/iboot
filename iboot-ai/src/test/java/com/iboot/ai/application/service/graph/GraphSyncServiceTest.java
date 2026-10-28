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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GraphSyncService} 的单元测试。
 *
 * <p>覆盖成功同步、下游失败与并发请求被单飞锁拒绝三种情况。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphSyncServiceTest {

    private final AdminOrgSnapshotClient client = mock(AdminOrgSnapshotClient.class);

    private final Neo4jGraphRepository repository = mock(Neo4jGraphRepository.class);

    private final GraphSyncService service = new GraphSyncService(client, repository,
            new AiMetrics(new SimpleMeterRegistry(), "test-model"));

    @Test
    void writesSnapshotFetchedWithCallerToken() {
        GraphSnapshot snapshot = new GraphSnapshot("sync-1", null, null, null, null);
        GraphSyncSummary summary = new GraphSyncSummary("sync-1", Instant.now(), 0, 0, 0, 0, 0, 0);
        when(client.fetch("token")).thenReturn(snapshot);
        when(repository.replace(snapshot)).thenReturn(summary);

        assertThat(service.sync("token")).isEqualTo(summary);
    }

    @Test
    void doesNotTouchGraphWhenDownstreamFails() {
        when(client.fetch("token")).thenThrow(new GraphSyncException("权限不足"));

        assertThatThrownBy(() -> service.sync("token"))
                .isInstanceOf(GraphSyncException.class)
                .hasMessageContaining("权限");

        verify(repository, never()).replace(any());
    }

    @Test
    void rejectsConcurrentSync() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(client.fetch("token")).thenAnswer(invocation -> {
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return new GraphSnapshot("sync-1", null, null, null, null);
        });
        when(repository.replace(any())).thenReturn(
                new GraphSyncSummary("sync-1", Instant.now(), 0, 0, 0, 0, 0, 0));

        Thread worker = Thread.ofVirtual().start(() -> service.sync("token"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> service.sync("token"))
                .isInstanceOf(GraphSyncException.class)
                .hasMessageContaining("进行中");

        release.countDown();
        worker.join(5000L);
    }

    @Test
    void wrapsWriteFailure() {
        when(client.fetch("token")).thenReturn(new GraphSnapshot("sync-1", List.of(), null, null, null));
        when(repository.replace(any())).thenThrow(new IllegalStateException("connection refused"));

        assertThatThrownBy(() -> service.sync("token"))
                .isInstanceOf(GraphSyncException.class)
                .hasMessageContaining("写入");
    }
}
