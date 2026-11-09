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

package com.iboot.workflow.infrastructure.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seata 演练故障 profile 测试。
 *
 * <p>验证默认 profile 不注册故障切面与过滤器，{@code seata-demo} profile 下才注册；
 * 并验证切面会抛出异常且清理线程标记。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SeataDemoFaultProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DemoConfig.class);

    /**
     * 默认 profile 不应注册故障 Bean。
     */
    @Test
    void defaultProfileDoesNotRegisterFaultBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(SeataDemoFaultAspect.class);
            assertThat(context).doesNotHaveBean(SeataDemoFaultFilter.class);
        });
    }

    /**
     * seata-demo profile 才注册故障 Bean。
     */
    @Test
    void demoProfileRegistersFaultBeans() {
        runner.withPropertyValues("spring.profiles.active=seata-demo").run(context -> {
            assertThat(context).hasSingleBean(SeataDemoFaultAspect.class);
            assertThat(context).hasSingleBean(SeataDemoFaultFilter.class);
        });
    }

    /**
     * 切面在标记存在时抛出异常并清理标记。
     */
    @Test
    void aspectThrowsAndClearsMarker() {
        SeataDemoFaultContext.request();

        assertThatThrownBy(() -> new SeataDemoFaultAspect().failAfterRemoteDeduct())
                .isInstanceOf(IllegalStateException.class);
        assertThat(SeataDemoFaultContext.isRequested()).isFalse();
    }

    /**
     * 测试配置：导入两个带 profile 条件的组件。
     */
    @Configuration
    @Import({SeataDemoFaultAspect.class, SeataDemoFaultFilter.class})
    static class DemoConfig {
    }
}
