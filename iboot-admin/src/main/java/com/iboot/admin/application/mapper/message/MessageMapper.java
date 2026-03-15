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

import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.interfaces.dto.export.MessageExportVO;
import com.iboot.admin.interfaces.dto.request.MessageRequest;
import com.iboot.admin.interfaces.dto.response.MessageResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 消息 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(
    componentModel = "spring",
    
    
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface MessageMapper {

    // ==================== 查询映射 ====================

    /**
     * 将消息实体转换为响应 DTO
     *
     * @param message 消息实体
     * @return 消息响应 DTO
     */
    MessageResponse toResponse(Message message);

    /**
     * 将消息实体列表转换为响应 DTO 列表
     *
     * @param messages 消息实体列表
     * @return 消息响应 DTO 列表
     */
    List<MessageResponse> toResponseList(List<Message> messages);

    /**
     * 将消息实体转换为导出 VO
     *
     * @param message 消息实体
     * @return 消息导出 VO
     */
    MessageExportVO toExportVO(Message message);

    /**
     * 将消息实体列表转换为导出 VO 列表
     *
     * @param messages 消息实体列表
     * @return 消息导出 VO 列表
     */
    List<MessageExportVO> toExportVOList(List<Message> messages);

    // ==================== 创建映射 ====================

    /**
     * 将消息请求转换为实体
     *
     * @param request 消息请求
     * @return 消息实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senderType", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    Message toEntity(MessageRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将消息请求映射到现有实体
     *
     * @param request 消息请求
     * @param message 现有消息实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senderType", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromRequest(MessageRequest request, @MappingTarget Message message);
}
