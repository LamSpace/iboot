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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.RedisMonitorApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.interfaces.dto.response.RedisMonitorResponse;
import com.iboot.admin.interfaces.dto.response.RedisMonitorResponse.CacheKeyDetail;
import com.iboot.admin.interfaces.dto.response.RedisMonitorResponse.CacheKeyInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Redis 缓存监控控制器。
 *
 * <p>提供 Redis 服务器信息、缓存键查询与删除、清空缓存等接口，分别需要
 * {@code redis:list}、{@code redis:query}、{@code redis:delete}、{@code redis:clear} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see RedisMonitorApplicationService
 */
@Tag(name = "Redis缓存监控", description = "Redis缓存监控及缓存键管理接口")
@RestController
@RequestMapping("/api/monitor/redis")
public class RedisMonitorController {

    private final RedisMonitorApplicationService redisMonitorApplicationService;

    @SuppressWarnings("all")
    public RedisMonitorController(final RedisMonitorApplicationService redisMonitorApplicationService) {
        this.redisMonitorApplicationService = redisMonitorApplicationService;
    }

    /**
     * 查询 Redis 监控信息。
     *
     * @return 服务器信息、内存、统计、键空间和命令统计
     */
    @Operation(summary = "获取Redis监控信息", description = "获取Redis服务器信息、内存、统计、键空间、命令统计等监控数据")
    @GetMapping
    @PreAuthorize("hasAuthority('redis:list')")
    public Result<RedisMonitorResponse> getRedisInfo() {
        RedisMonitorResponse response = redisMonitorApplicationService.getRedisMonitorInfo();
        return Result.success(response);
    }

    /**
     * 分页查询缓存键。
     *
     * @param pattern 匹配模式，支持通配符，默认 {@code *}
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 缓存键分页结果
     */
    @Operation(summary = "查询缓存键列表", description = "使用SCAN命令分页查询缓存键，支持通配符匹配")
    @GetMapping(version = "1", value = "/keys")
    @PreAuthorize("hasAuthority('redis:list')")
    public Result<PageResult<CacheKeyInfo>> getCacheKeys(@RequestParam(defaultValue = "*") String pattern,
                                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        PageResult<CacheKeyInfo> pageResult = redisMonitorApplicationService.getCacheKeys(pattern, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 获取缓存键详情。
     *
     * @param key 缓存键
     * @return 键类型、TTL 和值详情；键不存在时返回错误响应
     */
    @Operation(summary = "获取缓存键值", description = "获取指定缓存键的类型、TTL和值详情")
    @GetMapping(version = "1", value = "/key/{key}")
    @PreAuthorize("hasAuthority('redis:query')")
    public Result<CacheKeyDetail> getCacheKeyValue(@PathVariable String key) {
        CacheKeyDetail detail = redisMonitorApplicationService.getCacheKeyValue(key);
        if (detail == null) {
            return Result.error("缓存键不存在");
        }
        return Result.success(detail);
    }

    /**
     * 删除缓存键。
     *
     * @param key 缓存键
     * @return 成功响应；键不存在或删除失败时返回错误响应
     */
    @Operation(summary = "删除缓存键", description = "删除指定的缓存键")
    @DeleteMapping(version = "1", value = "/key/{key}")
    @PreAuthorize("hasAuthority('redis:delete')")
    @Log(title = "Redis缓存监控", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> deleteCacheKey(@PathVariable String key) {
        boolean result = redisMonitorApplicationService.deleteCacheKey(key);
        return result ? Result.success() : Result.error("删除失败，缓存键可能不存在");
    }

    /**
     * 清空当前数据库的所有缓存键。
     *
     * <p>该操作不可恢复，属于危险操作，需要 {@code redis:clear} 权限。
     *
     * @return 无数据的成功响应
     */
    @Operation(summary = "清空所有缓存", description = "清空当前数据库所有缓存键（危险操作）")
    @DeleteMapping(version = "1", value = "/keys")
    @PreAuthorize("hasAuthority('redis:clear')")
    @Log(title = "Redis缓存监控", businessType = BusinessTypeEnum.CLEAN)
    public Result<Void> clearAllKeys() {
        redisMonitorApplicationService.clearAllKeys();
        return Result.success();
    }

}
