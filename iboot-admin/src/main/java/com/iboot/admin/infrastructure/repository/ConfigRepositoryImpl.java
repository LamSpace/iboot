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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.system.model.Config;
import com.iboot.admin.domain.system.repository.ConfigRepository;
import com.iboot.admin.infrastructure.persistence.mapper.ConfigMapper;
import com.iboot.admin.infrastructure.persistence.po.ConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 系统配置仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现系统配置数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigMapper configMapper;

    /**
     * 保存系统配置
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param config 系统配置实体
     * @return 保存后的系统配置
     */
    @Override
    public Config save(Config config) {
        ConfigPO po = convertToPO(config);
        if (config.getId() == null) {
            configMapper.insert(po);
        } else {
            configMapper.update(po);
        }
        return convertToDomain(po);
    }

    /**
     * 更新系统配置
     *
     * @param config 系统配置实体
     * @return 是否更新成功
     */
    @Override
    public boolean update(Config config) {
        ConfigPO po = convertToPO(config);
        return configMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除系统配置（逻辑删除）
     *
     * @param id 系统配置 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return configMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询系统配置
     *
     * @param id 系统配置 ID
     * @return 系统配置实体，不存在则返回空
     */
    @Override
    public Optional<Config> findById(Long id) {
        ConfigPO po = configMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 根据配置键查询系统配置
     *
     * @param configKey 配置键
     * @return 系统配置实体，不存在则返回空
     */
    @Override
    public Optional<Config> findByConfigKey(String configKey) {
        ConfigPO po = configMapper.selectByConfigKey(configKey);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 查询所有系统配置
     *
     * @return 系统配置列表
     */
    @Override
    public List<Config> findAll() {
        return configMapper.selectAll().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询系统配置
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 系统配置列表
     */
    @Override
    public List<Config> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return configMapper.selectPage(offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计系统配置总数
     *
     * @return 系统配置总数
     */
    @Override
    public long count() {
        return configMapper.count();
    }

    /**
     * 按条件分页查询系统配置
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 系统配置列表
     */
    @Override
    public List<Config> findPageByCondition(String configName, String configKey, Integer configType, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return configMapper.selectPageByCondition(configName, configKey, configType, offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计系统配置总数
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 系统配置总数
     */
    @Override
    public long countByCondition(String configName, String configKey, Integer configType) {
        return configMapper.countByCondition(configName, configKey, configType);
    }

    /**
     * 按条件查询所有系统配置（不分页，用于导出）
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 系统配置列表
     */
    @Override
    public List<Config> findAllByCondition(String configName, String configKey, Integer configType) {
        return configMapper.selectAllByCondition(configName, configKey, configType).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 检查配置键是否存在
     *
     * @param configKey 配置键
     * @return 是否存在
     */
    @Override
    public boolean existsByConfigKey(String configKey) {
        return configMapper.countByConfigKey(configKey) > 0;
    }

    /**
     * 物理删除已逻辑删除的系统配置记录（根据配置键）
     *
     * @param configKey 配置键
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByConfigKey(String configKey) {
        return configMapper.removeDeletedByConfigKey(configKey) > 0;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param config 系统配置领域对象
     * @return 系统配置持久化对象
     */
    private ConfigPO convertToPO(Config config) {
        ConfigPO po = new ConfigPO();
        po.setId(config.getId());
        po.setConfigName(config.getConfigName());
        po.setConfigKey(config.getConfigKey());
        po.setConfigValue(config.getConfigValue());
        po.setConfigType(config.getConfigType());
        po.setCreateBy(config.getCreateBy());
        po.setCreateTime(config.getCreateTime());
        po.setUpdateBy(config.getUpdateBy());
        po.setUpdateTime(config.getUpdateTime());
        po.setDeleted(config.getDeleted() != null ? config.getDeleted() : 0);
        po.setRemark(config.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 系统配置持久化对象
     * @return 系统配置领域对象
     */
    private Config convertToDomain(ConfigPO po) {
        return Config.builder()
                .id(po.getId())
                .configName(po.getConfigName())
                .configKey(po.getConfigKey())
                .configValue(po.getConfigValue())
                .configType(po.getConfigType())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .deleted(po.getDeleted())
                .remark(po.getRemark())
                .build();
    }
}
