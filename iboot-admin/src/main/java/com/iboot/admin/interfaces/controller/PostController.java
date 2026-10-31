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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.PostApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Post;
import com.iboot.admin.interfaces.dto.export.PostExportVO;
import com.iboot.admin.interfaces.dto.request.PostRequest;
import com.iboot.admin.interfaces.dto.response.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位管理控制器。
 *
 * <p>提供岗位的分页查询、详情、新增、修改、删除、状态切换和导出接口，
 * 对应 {@code post:*} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see PostApplicationService
 */
@Tag(name = "岗位管理", description = "岗位管理相关接口")
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostApplicationService postApplicationService;

    @SuppressWarnings("all")
    public PostController(final PostApplicationService postApplicationService) {
        this.postApplicationService = postApplicationService;
    }

    /**
     * 分页查询岗位列表。
     *
     * <p>任一带条件参数非空时走条件查询，否则查询全部岗位。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param postName 岗位名称关键字，可为 {@code null}
     * @param postCode 岗位编码关键字，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @return 岗位分页结果
     */
    @Operation(summary = "查询岗位列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('post:list')")
    public Result<PageResult<PostResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String postName,
                                                 @RequestParam(required = false) String postCode,
                                                 @RequestParam(required = false) Integer status) {
        List<Post> posts;
        long total;
        if (postName != null || postCode != null || status != null) {
            posts = postApplicationService.getPostPageByCondition(postName, postCode, status, pageNum, pageSize);
            total = postApplicationService.countPostsByCondition(postName, postCode, status);
        } else {
            posts = postApplicationService.getPostPage(pageNum, pageSize);
            total = postApplicationService.countPosts();
        }
        List<PostResponse> responses = posts.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<PostResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询所有岗位。
     *
     * @return 岗位列表；没有数据时返回空列表
     */
    @Operation(summary = "查询所有岗位")
    @GetMapping(version = "1", value = "/all")
    @PreAuthorize("hasAuthority('post:list')")
    public Result<List<PostResponse>> all() {
        List<Post> posts = postApplicationService.getAllPosts();
        List<PostResponse> responses = posts.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 查询岗位详情。
     *
     * @param id 岗位 ID
     * @return 岗位详情
     * @throws com.iboot.kernel.web.BusinessException 岗位不存在时抛出
     */
    @Operation(summary = "查询岗位详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('post:query')")
    public Result<PostResponse> getById(@PathVariable Long id) {
        Post post = postApplicationService.getPostById(id);
        return Result.success(convertToResponse(post));
    }

    /**
     * 新增岗位。
     *
     * @param request 岗位创建请求
     * @return 创建后的岗位详情
     */
    @Operation(summary = "新增岗位")
    @PostMapping
    @PreAuthorize("hasAuthority('post:add')")
    @Log(title = "岗位管理", businessType = BusinessTypeEnum.INSERT)
    public Result<PostResponse> add(@Valid @RequestBody PostRequest request) {
        Post post = convertToEntity(request);
        Post createdPost = postApplicationService.createPost(post);
        return Result.success(convertToResponse(createdPost));
    }

    /**
     * 修改岗位。
     *
     * @param request 岗位更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改岗位")
    @PutMapping
    @PreAuthorize("hasAuthority('post:edit')")
    @Log(title = "岗位管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody PostRequest request) {
        Post post = convertToEntity(request);
        post.setId(request.getId());
        postApplicationService.updatePost(post);
        return Result.success();
    }

    /**
     * 删除岗位。
     *
     * @param id 岗位 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除岗位")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('post:remove')")
    @Log(title = "岗位管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        postApplicationService.deletePost(id);
        return Result.success();
    }

    /**
     * 修改岗位状态。
     *
     * @param id 岗位 ID
     * @param status 目标状态：1-启用，0-停用
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改岗位状态")
    @PutMapping(version = "1", value = "/changeStatus")
    @PreAuthorize("hasAuthority('post:edit')")
    @Log(title = "岗位管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        postApplicationService.changeStatus(id, status);
        return Result.success();
    }

    /**
     * 导出岗位列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param postName 岗位名称关键字，可为 {@code null}
     * @param postCode 岗位编码关键字，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出岗位列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('post:export')")
    @Log(title = "岗位管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String postName,
                       @RequestParam(required = false) String postCode, @RequestParam(required = false) Integer status)
            throws IOException {
        List<Post> posts;
        if (postName != null || postCode != null || status != null) {
            posts = postApplicationService.getAllPostsByCondition(postName, postCode, status);
        } else {
            posts = postApplicationService.getAllPosts();
        }
        List<PostExportVO> exportList = posts.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, PostExportVO.class, "岗位列表", "岗位数据");
    }

    private PostExportVO convertToExportVO(Post post) {
        return PostExportVO.builder()
                .id(post.getId())
                .postCode(post.getPostCode())
                .postName(post.getPostName())
                .orderNum(post.getPostSort())
                .status(post.getStatus())
                .remark(post.getRemark())
                .createTime(post.getCreateTime())
                .build();
    }

    private Post convertToEntity(PostRequest request) {
        return Post.builder()
                .postCode(request.getPostCode())
                .postName(request.getPostName())
                .postSort(request.getOrderNum())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }

    private PostResponse convertToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .postCode(post.getPostCode())
                .postName(post.getPostName())
                .orderNum(post.getPostSort())
                .status(post.getStatus())
                .remark(post.getRemark())
                .createTime(post.getCreateTime())
                .build();
    }

}
