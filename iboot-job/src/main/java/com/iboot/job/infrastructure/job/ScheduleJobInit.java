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

package com.iboot.job.infrastructure.job;

import com.iboot.job.domain.job.model.Job;
import com.iboot.job.domain.job.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定时任务初始化。
 *
 * <p>应用启动后将数据库中的任务同步到 Quartz 调度器，使任务配置在重启后继续生效。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ScheduleJobInit {

    private static final Logger log = LoggerFactory.getLogger(ScheduleJobInit.class);

    private final Scheduler scheduler;

    private final JobRepository jobRepository;

    @SuppressWarnings("all")
    public ScheduleJobInit(final Scheduler scheduler, final JobRepository jobRepository) {
        this.scheduler = scheduler;
        this.jobRepository = jobRepository;
    }

    /**
     * 应用启动时把数据库中的任务同步到调度器。
     *
     * <p>集群模式下多个实例共享同一份 Quartz 存储，因此<b>不能</b>清空整个调度器：
     * 那会抹掉其它在线实例正在执行的任务。这里只做增量对账——删除数据库中已不存在的
     * 任务，其余任务按 upsert 方式重新登记；并发启动时若另一个实例刚创建了同名任务，
     * 以对方为准即可，不影响最终一致。
     */
    @PostConstruct
    public void init() {
        try {
            List<Job> jobList = jobRepository.findAll();
            Set<JobKey> desired = jobList.stream()
                    .map(job -> ScheduleUtils.getJobKey(job.getId(), job.getJobGroup()))
                    .collect(Collectors.toSet());
            removeOrphanJobs(desired);
            for (Job job : jobList) {
                scheduleQuietly(job);
            }
            log.info("定时任务初始化完成，共加载 {} 个任务", jobList.size());
        } catch (SchedulerException e) {
            log.error("定时任务初始化失败", e);
            throw new RuntimeException("定时任务初始化失败", e);
        }
    }

    /**
     * 删除数据库中已不存在、但仍留在 Quartz 里的任务，避免删除后继续触发。
     *
     * @param desired 数据库中仍存在的任务键
     * @throws SchedulerException 查询调度器失败时抛出
     */
    private void removeOrphanJobs(final Set<JobKey> desired) throws SchedulerException {
        for (JobKey key : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
            if (key.getName().startsWith(ScheduleConstants.TASK_CLASS_NAME) && !desired.contains(key)) {
                scheduler.deleteJob(key);
                log.info("移除已不存在的定时任务：{}", key.getName());
            }
        }
    }

    /**
     * 登记单个任务；并发启动时另一个实例可能已抢先创建，此时以对方为准。
     *
     * @param job 任务定义
     */
    private void scheduleQuietly(final Job job) {
        try {
            ScheduleUtils.createScheduleJob(scheduler, job);
        } catch (ObjectAlreadyExistsException e) {
            log.debug("任务已由其它实例登记，跳过：{}", job.getId());
        } catch (SchedulerException e) {
            log.error("登记定时任务失败，jobId={}", job.getId(), e);
            throw new RuntimeException("登记定时任务失败", e);
        }
    }

}
