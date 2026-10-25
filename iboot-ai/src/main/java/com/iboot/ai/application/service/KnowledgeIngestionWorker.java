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

package com.iboot.ai.application.service;

import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import com.iboot.ai.infrastructure.config.RagProperties;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库入库 worker。
 *
 * <p>定时在事务中认领一条文档并标记处理中，然后在事务外执行解析、向量化与索引；
 * 处理失败由应用服务写入失败状态与下次重试时间。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeIngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionWorker.class);

    private final KnowledgeRepository repository;

    private final KnowledgeIngestionService ingestionService;

    private final RagProperties properties;

    private final TransactionTemplate transactionTemplate;

    /**
     * 创建 worker。
     *
     * @param repository          元数据仓储
     * @param ingestionService    入库服务
     * @param properties          知识库配置
     * @param transactionTemplate 事务模板
     */
    public KnowledgeIngestionWorker(KnowledgeRepository repository, KnowledgeIngestionService ingestionService,
                                    RagProperties properties, TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.ingestionService = ingestionService;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 轮询并处理一条待入库文档。
     */
    @Scheduled(fixedDelayString = "${iboot.ai.rag.ingest.poll-interval-ms:5000}")
    public void poll() {
        KnowledgeDocument document;
        try {
            document = transactionTemplate.execute(status -> claim());
        } catch (RuntimeException e) {
            log.warn("认领入库任务失败：{}", e.getMessage());
            return;
        }
        if (document != null) {
            try {
                ingestionService.process(document.id());
            } catch (RuntimeException e) {
                log.error("处理入库任务失败，docId: {}", document.id(), e);
            }
        }
    }

    private KnowledgeDocument claim() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(properties.getIngest().getStaleMinutes());
        List<KnowledgeDocument> documents = repository.findClaimable(now, staleBefore,
                properties.getIngest().getMaxAttempts(), 1);
        if (documents.isEmpty()) {
            return null;
        }
        KnowledgeDocument document = documents.getFirst();
        if (!repository.markProcessing(document.id(), now)) {
            return null;
        }
        return repository.findDocument(document.id()).orElse(document);
    }
}
