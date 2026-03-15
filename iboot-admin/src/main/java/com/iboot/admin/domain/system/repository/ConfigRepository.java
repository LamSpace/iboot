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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.Config;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储接口
 * <p>
 * 负责系统参数配置的持久化操作，支持系统运行时的动态配置管理
 * </p>
 *
 * @author iBoot
 */
public interface ConfigRepository {

    /**
     * 保存配置
     *
     * @param config 配置实体
     * @return 保存后的配置
     */
    Config save(Config config);

    /**
     * 更新配置
     *
     * @param config 配置实体
     * @return 是否更新成功
     */
    boolean update(Config config);

    /**
     * 根据 ID 删除配置（逻辑删除）
     *
     * @param id 配置 ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询配置
     *
     * @param id 配置 ID
     * @return 配置实体，不存在则返回空
     */
    Optional<Config> findById(Long id);

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 配置实体，不存在则返回空
     */
    Optional<Config> findByConfigKey(String configKey);

    /**
     * 查询所有配置
     *
     * @return 配置列表
     */
    List<Config> findAll();

    /**
     * 分页查询配置
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 配置列表
     */
    List<Config> findPage(int pageNum, int pageSize);

    /**
     * 统计配置总数
     *
     * @return 配置总数
     */
    long count();

    /**
     * 按条件分页查询配置
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 配置列表
     */
    List<Config> findPageByCondition(String configName, String configKey, Integer configType, int pageNum, int pageSize);

    /**
     * 按条件统计配置总数
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 配置总数
     */
    long countByCondition(String configName, String configKey, Integer configType);

    /**
     * 按条件查询所有配置（导出用）
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 配置列表
     */
    List<Config> findAllByCondition(String configName, String configKey, Integer configType);

    /**
     * 检查配置键是否存在
     *
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 物理删除已逻辑删除的配置记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param configKey 配置键
     * @return 是否删除成功
     */
    boolean removeDeletedByConfigKey(String configKey);
}
