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

package com.iboot.kernel.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KernelLoggingAutoConfiguration} 的启动测试。
 *
 * <p>验证默认表名可以直接启动，而格式非法的写入目标会在启动阶段失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class KernelLoggingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KernelLoggingAutoConfiguration.class));

    /**
     * 未配置写入目标时使用默认表名并正常启动。
     */
    @Test
    void startsWithDefaultTableName() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OperateLogRecorder.class);
        });
    }

    /**
     * 非法的写入目标必须让启动失败，而不是运行期静默写错表。
     */
    @Test
    void failsWhenTableNameIsInvalid() {
        runner.withPropertyValues("iboot.logging.operate-log-table=sys_operate_log;drop")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("operate-log-table");
                });
    }
}
