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

package com.iboot.admin.application.service;

import com.iboot.admin.infrastructure.storage.MinioProperties;
import io.minio.ListBucketsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MinIO 管理服务
 * <p>
 * 提供 MinIO 集群监控、存储桶管理等功能的接口。
 * 支持检查 MinIO 服务在线状态、获取存储桶统计信息（对象数量、总大小）等监控功能。
 * MinIO 配置通过 MinioProperties 注入，支持动态启用/禁用监控。
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioAdminService {

    private final MinioProperties minioProperties;

    /**
     * 获取 MinIO 监控数据
     * <p>
     * 检查 MinIO 服务状态，获取存储桶列表及统计信息。
     * 返回数据包括：服务状态、存储桶数量、对象总数、总大小等。
     * </p>
     *
     * @return MinIO 监控数据 Map，包含状态、存储桶统计等信息
     */
    public Map<String, Object> getMinioStatus() {
        Map<String, Object> result = new HashMap<>();

        // 检查是否启用 MinIO 监控
        result.put("monitorEnabled", minioProperties.isMonitorEnabled());

        // 检查存储类型
        result.put("storageType", minioProperties.getStorageType());
        result.put("minioEnabled", minioProperties.isMinioEnabled());

        // 获取控制台 URL
        result.put("consoleUrl", minioProperties.getConsoleUrl());

        // 获取 MinIO 连接信息
        result.put("endpoint", minioProperties.getEndpoint());
        result.put("bucketName", minioProperties.getBucketName());

        if (!minioProperties.isMinioEnabled()) {
            result.put("status", "disabled");
            result.put("message", "当前未启用 MinIO 存储");
            return result;
        }

        try {
            // 检查服务是否在线
            boolean isOnline = checkMinioOnline();
            if (!isOnline) {
                result.put("status", "offline");
                result.put("message", "MinIO 服务无响应");
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

            log.info("MinIO 监控数据获取成功：{} 个存储桶，{} 个对象，{} 字节",
                    buckets.size(), totalObjects, totalSize);

        } catch (Exception e) {
            log.error("获取 MinIO 监控数据失败", e);
            result.put("status", "error");
            result.put("message", "连接 MinIO 服务器失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 检查 MinIO 服务是否在线
     * <p>
     * 仅当 MinIO 存储启用时才执行检查。
     * </p>
     *
     * @return MinIO 是否在线
     */
    public boolean pingMinio() {
        if (!minioProperties.isMinioEnabled()) {
            return false;
        }
        return checkMinioOnline();
    }

    /**
     * 检查 MinIO 服务是否在线
     * <p>
     * 通过访问 MinIO 健康检查接口判断服务状态。
     * </p>
     *
     * @return MinIO 是否在线
     */
    private boolean checkMinioOnline() {
        try {
            String apiUrl = minioProperties.getEndpoint() + "/minio/health/live";
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("MinIO 服务在线检查失败：{}", e.getMessage());
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
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();

            // 获取存储桶列表
            List<Bucket> buckets = minioClient.listBuckets(ListBucketsArgs.builder().build());

            for (Bucket bucket : buckets) {
                Map<String, Long> stats = new HashMap<>();
                stats.put("objectCount", 0L);
                stats.put("totalSize", 0L);

                // 遍历存储桶中的对象获取统计信息
                try {
                    Iterable<Result<Item>> objects = minioClient.listObjects(
                            ListObjectsArgs.builder()
                                    .bucket(bucket.name())
                                    .recursive(true)
                                    .build());

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
