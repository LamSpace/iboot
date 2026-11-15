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

package com.iboot.monitor.application.service;

import com.iboot.monitor.infrastructure.storage.SiloProperties;
import io.minio.ListBucketsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Silo 管理服务
 * <p>
 * 提供 Silo 集群监控、存储桶管理等功能的接口。 支持检查 Silo 服务在线状态、获取存储桶统计信息（对象数量、总大小）等监控功能。 Silo 配置通过 SiloProperties 注入，支持动态启用/禁用监控。
 * </p>
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class SiloAdminService {

    private static final Logger log = LoggerFactory.getLogger(SiloAdminService.class);

    private final SiloProperties siloProperties;

    @SuppressWarnings("all")
    public SiloAdminService(final SiloProperties siloProperties) {
        this.siloProperties = siloProperties;
    }

    /**
     * 获取 Silo 监控数据
     * <p>
     * 检查 Silo 服务状态，获取存储桶列表及统计信息。 返回数据包括：服务状态、存储桶数量、对象总数、总大小等。
     * </p>
     *
     * @return Silo 监控数据 Map，包含状态、存储桶统计等信息
     */
    public Map<String, Object> getSiloStatus() {
        Map<String, Object> result = new HashMap<>();
        // 检查是否启用 Silo 监控
        result.put("monitorEnabled", siloProperties.isMonitorEnabled());
        // 检查存储类型
        result.put("storageType", siloProperties.getStorageType());
        result.put("siloEnabled", siloProperties.isSiloEnabled());
        // 获取控制台 URL
        result.put("consoleUrl", siloProperties.getConsoleUrl());
        // 获取 Silo 连接信息
        result.put("endpoint", siloProperties.getEndpoint());
        result.put("bucketName", siloProperties.getBucketName());
        if (!siloProperties.isSiloEnabled()) {
            result.put("status", "disabled");
            result.put("message", "当前未启用 Silo 存储");
            return result;
        }
        try {
            // 检查服务是否在线
            boolean isOnline = checkSiloOnline();
            if (!isOnline) {
                result.put("status", "offline");
                result.put("message", "Silo 服务无响应");
                return result;
            }
            result.put("status", "online");
            result.put("uptime", "-");
            result.put("version", "-");
            // 获取存储桶列表及统计信息
            // 返回 Map: { "bucketName": { "objectCount": xxx, "totalSize": xxx } }
            Map<String, Map<String, Long>> buckets = fetchBucketStats();
            result.put("buckets", buckets);
            result.put("totalBuckets", buckets.size());
            // 计算总对象数和总大小
            long totalObjects = 0;
            long totalSize = 0;
            for (Map<String, Long> stats : buckets.values()) {
                totalObjects += stats.get("objectCount");
                totalSize += stats.get("totalSize");
            }
            result.put("totalObjects", totalObjects);
            result.put("totalSize", totalSize);
            log.info("Silo 监控数据获取成功：{} 个存储桶，{} 个对象，{} 字节", buckets.size(), totalObjects, totalSize);
        } catch (Exception e) {
            log.error("获取 Silo 监控数据失败", e);
            result.put("status", "error");
            result.put("message", "连接 Silo 服务器失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 检查 Silo 服务是否在线
     * <p>
     * 仅当 Silo 存储启用时才执行检查。
     * </p>
     *
     * @return Silo 是否在线
     */
    public boolean pingSilo() {
        if (!siloProperties.isSiloEnabled()) {
            return false;
        }
        return checkSiloOnline();
    }

    /**
     * 检查 Silo 服务是否在线
     * <p>
     * 通过访问 Silo 健康检查接口判断服务状态。
     * </p>
     *
     * @return Silo 是否在线
     */
    private boolean checkSiloOnline() {
        try {
            // Silo 保留 MinIO 的 /minio/health/live 兼容路由，因此路径保持不变
            String apiUrl = siloProperties.getEndpoint() + "/minio/health/live";
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Silo 服务在线检查失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有存储桶的统计信息
     * <p>
     * 遍历所有存储桶，统计每个桶的对象数量和总大小。
     * </p>
     *
     * @return Map: { "bucketName": { "objectCount": xxx, "totalSize": xxx } }
     */
    private Map<String, Map<String, Long>> fetchBucketStats() {
        Map<String, Map<String, Long>> bucketStatsMap = new HashMap<>();
        try {
            MinioClient siloClient = MinioClient.builder()
                    .endpoint(siloProperties.getEndpoint())
                    .credentials(siloProperties.getAccessKey(), siloProperties.getSecretKey())
                    .build();
            // 获取存储桶列表
            List<Bucket> buckets = siloClient.listBuckets(ListBucketsArgs.builder().build());
            for (Bucket bucket : buckets) {
                Map<String, Long> stats = new HashMap<>();
                stats.put("objectCount", 0L);
                stats.put("totalSize", 0L);
                // 遍历存储桶中的对象获取统计信息
                try {
                    Iterable<Result<Item>> objects = siloClient
                            .listObjects(ListObjectsArgs.builder().bucket(bucket.name()).recursive(true).build());
                    for (Result<Item> result : objects) {
                        try {
                            Item item = result.get();
                            if (!item.isDir()) {
                                stats.put("objectCount", stats.get("objectCount") + 1);
                                stats.put("totalSize", stats.get("totalSize") + item.size());
                            }
                        } catch (Exception e) {
                            log.debug("获取对象信息失败：{}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.debug("遍历存储桶 {} 失败：{}", bucket.name(), e.getMessage());
                }
                bucketStatsMap.put(bucket.name(), stats);
                log.debug("存储桶 {}: {} 个对象，{} 字节", bucket.name(), stats.get("objectCount"), stats.get("totalSize"));
            }
        } catch (Exception e) {
            log.error("获取存储桶统计信息失败：{}", e.getMessage());
        }
        return bucketStatsMap;
    }

}
