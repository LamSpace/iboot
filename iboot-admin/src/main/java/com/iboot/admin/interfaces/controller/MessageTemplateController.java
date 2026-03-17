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
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
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
 * 消息模板控制器
 *
 * @author iBoot
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

    @Operation(summary = "查询消息模板列表")
    @GetMapping("/list")
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

    @Operation(summary = "查询消息模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('message:template:query')")
    public Result<MessageTemplateResponse> getById(@PathVariable Long id) {
        MessageTemplate template = messageTemplateApplicationService.getTemplateById(id);
        return Result.success(convertToResponse(template));
    }

    @Operation(summary = "获取所有启用的消息模板")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('message:template:list')")
    public Result<List<MessageTemplateResponse>> all() {
        List<MessageTemplate> templates = messageTemplateApplicationService.getAllEnabledTemplates();
        List<MessageTemplateResponse> responses = templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "新增消息模板")
    @PostMapping
    @PreAuthorize("hasAuthority('message:template:add')")
    @Log(title = "消息模板", businessType = BusinessTypeEnum.INSERT)
    public Result<MessageTemplateResponse> add(@Valid @RequestBody MessageTemplateRequest request) {
        MessageTemplate template = convertToEntity(request);
        MessageTemplate created = messageTemplateApplicationService.createTemplate(template);
        return Result.success(convertToResponse(created));
    }

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

    @Operation(summary = "删除消息模板")
    @DeleteMapping("/{id}")
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
