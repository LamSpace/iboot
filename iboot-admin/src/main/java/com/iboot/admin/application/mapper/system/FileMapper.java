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

import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.interfaces.dto.export.FileExportVO;
import com.iboot.admin.interfaces.dto.request.FileUpdateRequest;
import com.iboot.admin.interfaces.dto.response.FileResponse;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * 文件 Mapper 接口
 *
 * @author iBoot
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface FileMapper {

    // ==================== 查询映射 ====================

    /**
     * 将文件实体转换为响应 DTO
     *
     * @param fileInfo 文件实体
     *
     * @return 文件响应 DTO
     */
    FileResponse toResponse(FileInfo fileInfo);

    /**
     * 将文件实体列表转换为响应 DTO 列表
     *
     * @param files 文件实体列表
     *
     * @return 文件响应 DTO 列表
     */
    List<FileResponse> toResponseList(List<FileInfo> files);

    /**
     * 将文件实体转换为导出 VO
     *
     * @param fileInfo 文件实体
     *
     * @return 文件导出 VO
     */
    FileExportVO toExportVO(FileInfo fileInfo);

    /**
     * 将文件实体列表转换为导出 VO 列表
     *
     * @param files 文件实体列表
     *
     * @return 文件导出 VO 列表
     */
    List<FileExportVO> toExportVOList(List<FileInfo> files);

    // ==================== 更新映射 ====================

    /**
     * 将文件更新请求映射到现有实体
     *
     * @param request  文件更新请求
     * @param fileInfo 现有文件实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "filePath", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "fileType", ignore = true)
    @Mapping(target = "fileExt", ignore = true)
    @Mapping(target = "uploadBy", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(FileUpdateRequest request, @MappingTarget FileInfo fileInfo);

}
