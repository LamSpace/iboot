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

import com.iboot.admin.domain.system.model.Post;
import com.iboot.admin.interfaces.dto.export.PostExportVO;
import com.iboot.admin.interfaces.dto.request.PostRequest;
import com.iboot.admin.interfaces.dto.response.PostResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 岗位 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PostMapper {

    // ==================== 查询映射 ====================

    /**
     * 将岗位实体转换为响应 DTO
     *
     * @param post 岗位实体
     *
     * @return 岗位响应 DTO
     */
    @Mapping(target = "orderNum", source = "postSort")
    PostResponse toResponse(Post post);

    /**
     * 将岗位实体列表转换为响应 DTO 列表
     *
     * @param posts 岗位实体列表
     *
     * @return 岗位响应 DTO 列表
     */
    List<PostResponse> toResponseList(List<Post> posts);

    /**
     * 将岗位实体转换为导出 VO
     *
     * @param post 岗位实体
     *
     * @return 岗位导出 VO
     */
    @Mapping(target = "orderNum", source = "postSort")
    PostExportVO toExportVO(Post post);

    /**
     * 将岗位实体列表转换为导出 VO 列表
     *
     * @param posts 岗位实体列表
     *
     * @return 岗位导出 VO 列表
     */
    List<PostExportVO> toExportVOList(List<Post> posts);

    // ==================== 创建映射 ====================

    /**
     * 将岗位请求转换为实体
     *
     * @param request 岗位请求
     *
     * @return 岗位实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "postSort", source = "orderNum")
    Post toEntity(PostRequest request);

    // ==================== 更新映射 ====================

    /**
     * 将岗位请求映射到现有实体
     *
     * @param request 岗位请求
     * @param post    现有岗位实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "postSort", source = "orderNum")
    void updateEntityFromRequest(PostRequest request, @MappingTarget Post post);

}
