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

package com.iboot.admin.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时的内容索引初始化。
 *
 * <p>幂等创建索引与 mapping，并异步触发一次全量回填。Elasticsearch 不可用时只记录日志，
 * 不阻塞应用启动；后续由定时对账重试创建与回填。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ContentIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentIndexInitializer.class);

    private final ElasticsearchContentIndex contentIndex;

    private final ContentIndexReconcileTask reconcileTask;

    @SuppressWarnings("all")
    public ContentIndexInitializer(final ElasticsearchContentIndex contentIndex,
                                   final ContentIndexReconcileTask reconcileTask) {
        this.contentIndex = contentIndex;
        this.reconcileTask = reconcileTask;
    }

    /**
     * 初始化索引并触发异步回填。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(final ApplicationArguments args) {
        try {
            contentIndex.ensureIndex();
        } catch (Exception e) {
            log.warn("内容索引初始化失败，将由定时对账重试", e);
        }
        reconcileTask.reconcileAsync();
    }

}
