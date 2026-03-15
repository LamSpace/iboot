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

import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.interfaces.dto.export.UserExportVO;
import com.iboot.admin.interfaces.dto.request.CreateUserRequest;
import com.iboot.admin.interfaces.dto.request.UpdateUserRequest;
import com.iboot.admin.interfaces.dto.response.UserResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 用户 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface UserMapper {

    // ==================== 查询映射 ====================

    /**
     * 将用户实体转换为响应 DTO
     *
     * @param user 用户实体
     * @return 用户响应 DTO
     */
    @Mapping(source = "avatar", target = "avatar", defaultValue = "")
    UserResponse toResponse(User user);

    /**
     * 将用户实体列表转换为响应 DTO 列表
     *
     * @param users 用户实体列表
     * @return 用户响应 DTO 列表
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * 将用户实体转换为导出 VO
     *
     * @param user 用户实体
     * @return 用户导出 VO
     */
    UserExportVO toExportVO(User user);

    /**
     * 将用户实体列表转换为导出 VO 列表
     *
     * @param users 用户实体列表
     * @return 用户导出 VO 列表
     */
    List<UserExportVO> toExportVOList(List<User> users);

    // ==================== 创建映射 ====================

    /**
     * 将创建用户请求转换为实体
     *
     * @param request 创建用户请求
     * @return 用户实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loginIp", ignore = true)
    @Mapping(target = "loginTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    User toEntity(CreateUserRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将更新用户请求映射到现有实体
     *
     * @param request 更新用户请求
     * @param user    现有用户实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "loginIp", ignore = true)
    @Mapping(target = "loginTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
