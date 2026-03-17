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
import com.iboot.admin.domain.job.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务调度初始化 在应用启动时，将数据库中的任务加载到 Quartz 调度器
 *
 * @author iBoot
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
     * 应用启动时初始化定时任务
     */
    @PostConstruct
    public void init() {
        try {
            // 清除已有的调度任务
            scheduler.clear();
            // 从数据库加载所有任务
            List<Job> jobList = jobRepository.findAll();
            for (Job job : jobList) {
                ScheduleUtils.createScheduleJob(scheduler, job);
            }
            log.info("定时任务初始化完成，共加载 {} 个任务", jobList.size());
        } catch (SchedulerException e) {
            log.error("定时任务初始化失败", e);
            throw new RuntimeException("定时任务初始化失败", e);
        }
    }

}
