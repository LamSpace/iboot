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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库 RAG 配置。
 *
 * <p>绑定 {@code iboot.ai.rag.*}，覆盖检索参数、入库 worker、Embedding、OCR 与对象存储。
 * OCR 默认关闭，只有 {@code iboot.ai.rag.ocr.enabled=true} 时才校验 Key 与模型。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.ai.rag")
public class RagProperties {

    /**
     * 是否启用知识库能力。
     */
    private boolean enabled = true;

    /**
     * 分块索引名称。
     */
    private String indexName = "iboot-kb-chunk-v1";

    /**
     * 返回的 Top-K 分块数。
     */
    private int topK = 5;

    /**
     * 单路检索的候选数。
     */
    private int candidateK = 20;

    /**
     * kNN 的候选向量数。
     */
    private int numCandidates = 100;

    /**
     * RRF 排名常数。
     */
    private int rankConstant = 60;

    /**
     * RRF 排名窗口。
     */
    private int rankWindowSize = 50;

    /**
     * 同一文档最多保留的分块数。
     */
    private int maxChunksPerDoc = 2;

    /**
     * 分块最大字符数。
     */
    private int chunkMaxChars = 800;

    /**
     * 相邻分块重叠字符数。
     */
    private int chunkOverlapChars = 100;

    /**
     * 判定页面无文本的最小字符数。
     */
    private int minPageChars = 20;

    /**
     * 入库配置。
     */
    private Ingest ingest = new Ingest();

    /**
     * Embedding 配置。
     */
    private Embedding embedding = new Embedding();

    /**
     * OCR 配置。
     */
    private Ocr ocr = new Ocr();

    /**
     * 对象存储配置。
     */
    private Storage storage = new Storage();

    /**
     * 入库 worker 配置。
     */
    public static class Ingest {

        /**
         * 轮询间隔毫秒数。
         */
        private long pollIntervalMs = 5000L;

        /**
         * 处理中任务的失效分钟数。
         */
        private long staleMinutes = 15L;

        /**
         * 最大重试次数。
         */
        private int maxAttempts = 3;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public long getStaleMinutes() {
            return staleMinutes;
        }

        public void setStaleMinutes(long staleMinutes) {
            this.staleMinutes = staleMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    /**
     * Embedding 配置。
     */
    public static class Embedding {

        /**
         * 单批文本数。
         */
        private int batchSize = 10;

        /**
         * 向量维度。
         */
        private int dimensions = 1024;

        /**
         * 每日 Embedding token 上限。
         */
        private long dailyTokenLimit = 5_000_000L;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }

        public long getDailyTokenLimit() {
            return dailyTokenLimit;
        }

        public void setDailyTokenLimit(long dailyTokenLimit) {
            this.dailyTokenLimit = dailyTokenLimit;
        }
    }

    /**
     * OCR 配置。
     */
    public static class Ocr {

        /**
         * 是否启用 OCR。
         */
        private boolean enabled = false;

        /**
         * OCR 模型标识。
         */
        private String model = "qwen3.5-ocr";

        /**
         * OCR 接入点；为空时复用对话接入点。
         */
        private String baseUrl = "";

        /**
         * OCR API Key。
         */
        private String apiKey = "";

        /**
         * 页面渲染 DPI。
         */
        private int dpi = 150;

        /**
         * 单文档最多送云页数。
         */
        private int maxPages = 50;

        /**
         * 单日最多送云页数。
         */
        private int dailyPageLimit = 200;

        /**
         * 单页请求超时秒数。
         */
        private int timeoutSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getDpi() {
            return dpi;
        }

        public void setDpi(int dpi) {
            this.dpi = dpi;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }

        public int getDailyPageLimit() {
            return dailyPageLimit;
        }

        public void setDailyPageLimit(int dailyPageLimit) {
            this.dailyPageLimit = dailyPageLimit;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * 对象存储配置。
     */
    public static class Storage {

        /**
         * MinIO 端点。
         */
        private String endpoint = "http://localhost:9000";

        /**
         * 访问 Key。
         */
        private String accessKey = "minioadmin";

        /**
         * 访问 Secret。
         */
        private String secretKey = "minioadmin";

        /**
         * bucket 名称。
         */
        private String bucket = "iboot-ai";

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getCandidateK() {
        return candidateK;
    }

    public void setCandidateK(int candidateK) {
        this.candidateK = candidateK;
    }

    public int getNumCandidates() {
        return numCandidates;
    }

    public void setNumCandidates(int numCandidates) {
        this.numCandidates = numCandidates;
    }

    public int getRankConstant() {
        return rankConstant;
    }

    public void setRankConstant(int rankConstant) {
        this.rankConstant = rankConstant;
    }

    public int getRankWindowSize() {
        return rankWindowSize;
    }

    public void setRankWindowSize(int rankWindowSize) {
        this.rankWindowSize = rankWindowSize;
    }

    public int getMaxChunksPerDoc() {
        return maxChunksPerDoc;
    }

    public void setMaxChunksPerDoc(int maxChunksPerDoc) {
        this.maxChunksPerDoc = maxChunksPerDoc;
    }

    public int getChunkMaxChars() {
        return chunkMaxChars;
    }

    public void setChunkMaxChars(int chunkMaxChars) {
        this.chunkMaxChars = chunkMaxChars;
    }

    public int getChunkOverlapChars() {
        return chunkOverlapChars;
    }

    public void setChunkOverlapChars(int chunkOverlapChars) {
        this.chunkOverlapChars = chunkOverlapChars;
    }

    public int getMinPageChars() {
        return minPageChars;
    }

    public void setMinPageChars(int minPageChars) {
        this.minPageChars = minPageChars;
    }

    public Ingest getIngest() {
        return ingest;
    }

    public void setIngest(Ingest ingest) {
        this.ingest = ingest;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public void setOcr(Ocr ocr) {
        this.ocr = ocr;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}
