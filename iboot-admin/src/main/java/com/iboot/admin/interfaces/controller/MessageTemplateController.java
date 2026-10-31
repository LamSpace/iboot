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

import com.iboot.admin.application.service.MessageTemplateApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.domain.system.model.MessageTemplate;
import com.iboot.admin.interfaces.dto.request.MessageTemplateRequest;
import com.iboot.admin.interfaces.dto.response.MessageTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息模板控制器。
 *
 * <p>提供消息模板的分页查询、详情、启用列表、新增、修改和删除接口，
 * 对应 {@code message:template:*} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MessageTemplateApplicationService
 */
@Tag(name = "消息模板", description = "消息模板相关接口")
@RestController
@RequestMapping("/api/message/template")
public class MessageTemplateController {

    private final MessageTemplateApplicationService messageTemplateApplicationService;

    @SuppressWarnings("all")
    public MessageTemplateController(final MessageTemplateApplicationService messageTemplateApplicationService) {
        this.messageTemplateApplicationService = messageTemplateApplicationService;
    }

    /**
     * 分页查询消息模板。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param templateName 模板名称关键字，可为 {@code null}
     * @param messageType 消息类型，可为 {@code null}
     * @param status 状态：0-停用，1-正常；可为 {@code null}
     * @return 模板分页结果
     */
    @Operation(summary = "查询消息模板列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('message:template:list')")
    public Result<PageResult<MessageTemplateResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String templateName,
                                                            @RequestParam(required = false) String messageType,
                                                            @RequestParam(required = false) Integer status) {
        List<MessageTemplate> templates = messageTemplateApplicationService.getTemplatePage(templateName, messageType,
                status, pageNum, pageSize);
        long total = messageTemplateApplicationService.countTemplates(templateName, messageType, status);
        List<MessageTemplateResponse> responses = templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        PageResult<MessageTemplateResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询消息模板详情。
     *
     * @param id 模板 ID
     * @return 模板详情
     * @throws com.iboot.kernel.web.BusinessException 模板不存在时抛出
     */
    @Operation(summary = "查询消息模板详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('message:template:query')")
    public Result<MessageTemplateResponse> getById(@PathVariable Long id) {
        MessageTemplate template = messageTemplateApplicationService.getTemplateById(id);
        return Result.success(convertToResponse(template));
    }

    /**
     * 查询所有启用的消息模板。
     *
     * @return 已启用的模板列表；没有数据时返回空列表
     */
    @Operation(summary = "获取所有启用的消息模板")
    @GetMapping(version = "1", value = "/all")
    @PreAuthorize("hasAuthority('message:template:list')")
    public Result<List<MessageTemplateResponse>> all() {
        List<MessageTemplate> templates = messageTemplateApplicationService.getAllEnabledTemplates();
        List<MessageTemplateResponse> responses = templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 新增消息模板。
     *
     * @param request 模板创建请求
     * @return 创建后的模板详情
     */
    @Operation(summary = "新增消息模板")
    @PostMapping
    @PreAuthorize("hasAuthority('message:template:add')")
    @Log(title = "消息模板", businessType = BusinessTypeEnum.INSERT)
    public Result<MessageTemplateResponse> add(@Valid @RequestBody MessageTemplateRequest request) {
        MessageTemplate template = convertToEntity(request);
        MessageTemplate created = messageTemplateApplicationService.createTemplate(template);
        return Result.success(convertToResponse(created));
    }

    /**
     * 修改消息模板。
     *
     * @param request 模板更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改消息模板")
    @PutMapping
    @PreAuthorize("hasAuthority('message:template:edit')")
    @Log(title = "消息模板", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody MessageTemplateRequest request) {
        MessageTemplate template = convertToEntity(request);
        template.setId(request.getId());
        messageTemplateApplicationService.updateTemplate(template);
        return Result.success();
    }

    /**
     * 删除消息模板。
     *
     * @param id 模板 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除消息模板")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('message:template:remove')")
    @Log(title = "消息模板", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        messageTemplateApplicationService.deleteTemplate(id);
        return Result.success();
    }

    // ==================== 转换方法 ====================
    private MessageTemplate convertToEntity(MessageTemplateRequest request) {
        return MessageTemplate.builder()
                .templateCode(request.getTemplateCode())
                .templateName(request.getTemplateName())
                .templateContent(request.getTemplateContent())
                .messageType(request.getMessageType())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }

    private MessageTemplateResponse convertToResponse(MessageTemplate template) {
        return MessageTemplateResponse.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .templateContent(template.getTemplateContent())
                .messageType(template.getMessageType())
                .status(template.getStatus())
                .createBy(template.getCreateBy())
                .createTime(template.getCreateTime())
                .remark(template.getRemark())
                .build();
    }

}
