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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务调度初始化
 * 在应用启动时，将数据库中的任务加载到 Quartz 调度器
 *
 * @author iBoot
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleJobInit {

    private final Scheduler scheduler;
    private final JobRepository jobRepository;

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
