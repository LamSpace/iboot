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

package com.iboot.admin.application.mapper.system;

import com.iboot.admin.domain.system.model.Config;
import com.iboot.admin.interfaces.dto.export.ConfigExportVO;
import com.iboot.admin.interfaces.dto.request.ConfigRequest;
import com.iboot.admin.interfaces.dto.response.ConfigResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 配置 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(
    componentModel = "spring",
    
    
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ConfigMapper {

    // ==================== 查询映射 ====================

    /**
     * 将配置实体转换为响应 DTO
     *
     * @param config 配置实体
     * @return 配置响应 DTO
     */
    ConfigResponse toResponse(Config config);

    /**
     * 将配置实体列表转换为响应 DTO 列表
     *
     * @param configs 配置实体列表
     * @return 配置响应 DTO 列表
     */
    List<ConfigResponse> toResponseList(List<Config> configs);

    /**
     * 将配置实体转换为导出 VO
     *
     * @param config 配置实体
     * @return 配置导出 VO
     */
    ConfigExportVO toExportVO(Config config);

    /**
     * 将配置实体列表转换为导出 VO 列表
     *
     * @param configs 配置实体列表
     * @return 配置导出 VO 列表
     */
    List<ConfigExportVO> toExportVOList(List<Config> configs);

    // ==================== 创建映射 ====================

    /**
     * 将配置请求转换为实体
     *
     * @param request 配置请求
     * @return 配置实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Config toEntity(ConfigRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将配置请求映射到现有实体
     *
     * @param request 配置请求
     * @param config  现有配置实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(ConfigRequest request, @MappingTarget Config config);
}
