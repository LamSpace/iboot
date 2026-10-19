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

package com.iboot.admin.infrastructure.job;

import com.iboot.admin.domain.job.model.Job;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 禁止并发的 Quartz 任务。
 *
 * <p>同一任务在上一轮尚未结束时不会再次触发，适用于有共享状态或外部副作用的场景。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob {

    @Override
    /**
     * 执行具体任务。
     *
     * @param context Quartz 执行上下文
     * @param job 任务配置
     * @throws Exception 任务执行失败时抛出
     */
    protected void doExecute(JobExecutionContext context, Job job) throws Exception {
        JobInvokeUtil.invokeMethod(job);
    }

}
