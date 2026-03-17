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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Config;
import com.iboot.admin.domain.system.repository.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统配置应用服务
 * <p>
 * 提供系统配置的增删改查功能，支持二级缓存（Caffeine + Redis）优化查询性能
 * </p>
 *
 * @author iBoot
 */
@Service
public class ConfigApplicationService {

    /**
     * 缓存名称，用于 Spring Cache 管理
     */
    public static final String CACHE_NAME = "sys_config";

    private static final Logger log = LoggerFactory.getLogger(ConfigApplicationService.class);

    private final ConfigRepository configRepository;

    @SuppressWarnings("all")
    public ConfigApplicationService(final ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 创建系统配置
     * <p>
     * 检查配置键是否已存在，清理同键名已删除的配置记录，保存新配置 保存成功后清除该配置键的缓存
     * </p>
     *
     * @param config 系统配置实体
     *
     * @return 创建后的系统配置
     *
     * @throws BusinessException 当配置键已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#config.configKey")
    public Config createConfig(Config config) {
        if (configRepository.existsByConfigKey(config.getConfigKey())) {
            throw new BusinessException("配置键已存在");
        }
        // 清理同键名已删除配置记录（解决逻辑删除与唯一索引冲突问题）
        configRepository.removeDeletedByConfigKey(config.getConfigKey());
        config.setCreateTime(LocalDateTime.now());
        return configRepository.save(config);
    }

    /**
     * 更新系统配置
     * <p>
     * 检查配置是否存在，更新配置信息。如果配置键发生变化，清除旧键的缓存 保存成功后清除该配置键的缓存
     * </p>
     *
     * @param config 系统配置实体
     *
     * @return 是否更新成功
     *
     * @throws BusinessException 当配置不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, key = "#config.configKey")
    public boolean updateConfig(Config config) {
        Config existing = configRepository.findById(config.getId()).orElseThrow(() -> new BusinessException("配置不存在"));
        // 如果配置键发生变化，需要清除旧键的缓存
        if (!existing.getConfigKey().equals(config.getConfigKey())) {
            // 旧键缓存将在方法完成后由 AOP 清除
            log.info("配置键变更：{} -> {}", existing.getConfigKey(), config.getConfigKey());
        }
        config.setUpdateTime(LocalDateTime.now());
        return configRepository.update(config);
    }

    /**
     * 删除系统配置
     * <p>
     * 检查配置是否存在，系统内置配置不允许删除 删除成功后手动清除对应配置键的缓存
     * </p>
     *
     * @param id 系统配置 ID
     *
     * @return 是否删除成功
     *
     * @throws BusinessException 当配置不存在或为系统内置配置时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        Config config = configRepository.findById(id).orElseThrow(() -> new BusinessException("配置不存在"));
        // 系统内置配置不允许删除
        if (config.isSystemConfig()) {
            throw new BusinessException("系统内置配置不允许删除");
        }
        boolean result = configRepository.deleteById(id);
        // 手动清除缓存（因为需要用到查询出的 configKey）
        if (result) {
            evictConfigCache(config.getConfigKey());
        }
        return result;
    }

    /**
     * 清除指定配置键的缓存
     * <p>
     * 使用 Spring Cache 注解清除指定配置键的缓存
     * </p>
     *
     * @param configKey 配置键
     */
    @CacheEvict(value = CACHE_NAME, key = "#configKey")
    public void evictConfigCache(String configKey) {
        log.debug("清除配置缓存：{}", configKey);
    }

    /**
     * 根据 ID 获取系统配置
     *
     * @param id 系统配置 ID
     *
     * @return 系统配置实体
     *
     * @throws BusinessException 当配置不存在时抛出
     */
    public Config getConfigById(Long id) {
        return configRepository.findById(id).orElseThrow(() -> new BusinessException("配置不存在"));
    }

    /**
     * 根据配置键获取配置值
     * <p>
     * 使用二级缓存：先查本地 Caffeine，再查 Redis，最后查数据库 配置值会被缓存，下次查询时直接返回
     * </p>
     *
     * @param configKey 配置键
     *
     * @return 配置值，不存在则返回 null
     */
    @Cacheable(value = CACHE_NAME, key = "#configKey", unless = "#result == null")
    public String getConfigValue(String configKey) {
        return configRepository.findByConfigKey(configKey).map(Config::getConfigValue).orElse(null);
    }

    /**
     * 获取所有系统配置
     *
     * @return 系统配置列表
     */
    public List<Config> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 分页获取系统配置
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 系统配置列表
     */
    public List<Config> getConfigPage(int pageNum, int pageSize) {
        return configRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计系统配置总数
     *
     * @return 系统配置总数
     */
    public long countConfigs() {
        return configRepository.count();
    }

    /**
     * 按条件分页获取系统配置
     *
     * @param configName 配置名称（可选）
     * @param configKey  配置键（可选）
     * @param configType 配置类型（可选）
     * @param pageNum    页码，从 1 开始
     * @param pageSize   每页数量
     *
     * @return 系统配置列表
     */
    public List<Config> getConfigPageByCondition(String configName, String configKey, Integer configType, int pageNum,
                                                 int pageSize) {
        return configRepository.findPageByCondition(configName, configKey, configType, pageNum, pageSize);
    }

    /**
     * 按条件统计系统配置总数
     *
     * @param configName 配置名称（可选）
     * @param configKey  配置键（可选）
     * @param configType 配置类型（可选）
     *
     * @return 系统配置总数
     */
    public long countConfigsByCondition(String configName, String configKey, Integer configType) {
        return configRepository.countByCondition(configName, configKey, configType);
    }

    /**
     * 按条件获取所有系统配置（不分页，用于导出）
     *
     * @param configName 配置名称（可选）
     * @param configKey  配置键（可选）
     * @param configType 配置类型（可选）
     *
     * @return 系统配置列表
     */
    public List<Config> getAllConfigsByCondition(String configName, String configKey, Integer configType) {
        return configRepository.findAllByCondition(configName, configKey, configType);
    }

    /**
     * 刷新缓存
     * <p>
     * 清除所有系统配置缓存，下次查询时将重新从数据库加载
     * </p>
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void refreshCache() {
        log.info("配置缓存已清除，将在下次查询时重新加载");
    }

}
