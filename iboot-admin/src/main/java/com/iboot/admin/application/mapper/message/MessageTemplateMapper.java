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

package com.iboot.admin.application.mapper.message;

import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.interfaces.dto.export.MessageTemplateExportVO;
import com.iboot.admin.interfaces.dto.request.MessageTemplateRequest;
import com.iboot.admin.interfaces.dto.response.MessageTemplateResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 消息模板 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface MessageTemplateMapper {

    // ==================== 查询映射 ====================

    /**
     * 将消息模板实体转换为响应 DTO
     *
     * @param template 消息模板实体
     *
     * @return 消息模板响应 DTO
     */
    MessageTemplateResponse toResponse(MessageTemplate template);

    /**
     * 将消息模板实体列表转换为响应 DTO 列表
     *
     * @param templates 消息模板实体列表
     *
     * @return 消息模板响应 DTO 列表
     */
    List<MessageTemplateResponse> toResponseList(List<MessageTemplate> templates);

    /**
     * 将消息模板实体转换为导出 VO
     *
     * @param template 消息模板实体
     *
     * @return 消息模板导出 VO
     */
    MessageTemplateExportVO toExportVO(MessageTemplate template);

    /**
     * 将消息模板实体列表转换为导出 VO 列表
     *
     * @param templates 消息模板实体列表
     *
     * @return 消息模板导出 VO 列表
     */
    List<MessageTemplateExportVO> toExportVOList(List<MessageTemplate> templates);

    // ==================== 创建映射 ====================

    /**
     * 将消息模板请求转换为实体
     *
     * @param request 消息模板请求
     *
     * @return 消息模板实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    MessageTemplate toEntity(MessageTemplateRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将消息模板请求映射到现有实体
     *
     * @param request  消息模板请求
     * @param template 现有消息模板实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(MessageTemplateRequest request, @MappingTarget MessageTemplate template);

}
