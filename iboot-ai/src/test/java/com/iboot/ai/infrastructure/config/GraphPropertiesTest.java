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

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GraphProperties} 的绑定测试。
 *
 * <p>不启动 Spring 上下文，直接通过 {@link Binder} 校验默认值与配置覆盖。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class GraphPropertiesTest {

    @Test
    void bindsDefaultsWithGraphDisabled() {
        GraphProperties properties = bind(new MockEnvironment());

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getUri()).isEqualTo("bolt://localhost:7687");
        assertThat(properties.getUsername()).isEqualTo("neo4j");
        assertThat(properties.getDatabase()).isEqualTo("neo4j");
        assertThat(properties.getSync().getPageSize()).isEqualTo(200);
        assertThat(properties.getSync().getMaxPages()).isEqualTo(50);
        assertThat(properties.getQuery().getMaxRows()).isEqualTo(50);
        assertThat(properties.getQuery().getTimeoutSeconds()).isEqualTo(10L);
    }

    @Test
    void bindsOverrides() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("iboot.ai.graph.enabled", "true")
                .withProperty("iboot.ai.graph.uri", "bolt://neo4j:7687")
                .withProperty("iboot.ai.graph.sync.page-size", "50")
                .withProperty("iboot.ai.graph.query.max-rows", "20");

        GraphProperties properties = bind(environment);

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getUri()).isEqualTo("bolt://neo4j:7687");
        assertThat(properties.getSync().getPageSize()).isEqualTo(50);
        assertThat(properties.getQuery().getMaxRows()).isEqualTo(20);
    }

    private GraphProperties bind(MockEnvironment environment) {
        return Binder.get(environment).bindOrCreate("iboot.ai.graph", GraphProperties.class);
    }
}
