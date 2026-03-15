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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.infrastructure.persistence.po.ConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper 接口
 * <p>
 * 负责系统参数配置的数据库操作，支持配置的增删改查
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface ConfigMapper {

    /**
     * 插入配置记录
     *
     * @param configPO 配置持久化对象
     * @return 影响的记录数
     */
    int insert(ConfigPO configPO);

    /**
     * 更新配置记录
     *
     * @param configPO 配置持久化对象
     * @return 影响的记录数
     */
    int update(ConfigPO configPO);

    /**
     * 根据 ID 删除配置
     *
     * @param id 配置 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询配置
     *
     * @param id 配置 ID
     * @return 配置持久化对象
     */
    ConfigPO selectById(@Param("id") Long id);

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 配置持久化对象
     */
    ConfigPO selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 查询所有配置
     *
     * @return 配置列表
     */
    List<ConfigPO> selectAll();

    /**
     * 分页查询配置
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 配置列表
     */
    List<ConfigPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计配置总数
     *
     * @return 配置总数
     */
    long count();

    /**
     * 按条件分页查询配置记录
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 配置列表
     */
    List<ConfigPO> selectPageByCondition(@Param("configName") String configName,
                                         @Param("configKey") String configKey,
                                         @Param("configType") Integer configType,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 按条件统计配置总数
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 配置总数
     */
    long countByCondition(@Param("configName") String configName,
                          @Param("configKey") String configKey,
                          @Param("configType") Integer configType);

    /**
     * 按条件查询所有配置（导出用）
     *
     * @param configName 配置名称（可选）
     * @param configKey 配置键（可选）
     * @param configType 配置类型（可选）
     * @return 配置列表
     */
    List<ConfigPO> selectAllByCondition(@Param("configName") String configName,
                                        @Param("configKey") String configKey,
                                        @Param("configType") Integer configType);

    /**
     * 根据配置键统计数量
     *
     * @param configKey 配置键
     * @return 数量
     */
    int countByConfigKey(@Param("configKey") String configKey);

    /**
     * 物理删除已逻辑删除的配置记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param configKey 配置键
     * @return 影响的记录数
     */
    int removeDeletedByConfigKey(@Param("configKey") String configKey);
}
