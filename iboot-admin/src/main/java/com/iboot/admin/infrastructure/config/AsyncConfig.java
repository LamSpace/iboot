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

package com.iboot.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;

/**
 * 异步任务配置。
 *
 * <p>为 {@code @Async} 方法注册基于虚拟线程的执行器，避免平台线程池
 * 在高并发 I/O 场景下的线程饥饿。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 创建基于虚拟线程的异步任务执行器。
     *
     * <p>以 {@link AsyncTaskExecutor} 暴露，使依赖 {@code @ConditionalOnMissingBean} 按类型判断
     * 的第三方自动配置（如 Flowable）能够识别并退让，避免同名 Bean 冲突。
     *
     * @return 虚拟线程执行器
     */
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

}
