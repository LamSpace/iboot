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

import com.iboot.admin.application.service.MessageApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Message;
import com.iboot.admin.domain.system.model.MessageReceiver;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.export.MessageExportVO;
import com.iboot.admin.interfaces.dto.request.MessageRequest;
import com.iboot.admin.interfaces.dto.request.MessageSendRequest;
import com.iboot.admin.interfaces.dto.response.MessageResponse;
import com.iboot.admin.interfaces.dto.response.UserMessageResponse;
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
 * 消息中心控制器。
 *
 * <p>提供消息的分页查询、详情、增删改、发送、撤回、导出，以及用户端的收件箱、
 * 未读数量、标记已读和删除接口。管理接口对应 {@code message:*} 权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see MessageApplicationService
 */
@Tag(name = "消息中心", description = "消息中心相关接口")
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageApplicationService messageApplicationService;

    @SuppressWarnings("all")
    public MessageController(final MessageApplicationService messageApplicationService) {
        this.messageApplicationService = messageApplicationService;
    }

    // ==================== 后台管理接口 ====================
    /**
     * 分页查询消息列表。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param title 标题关键字，可为 {@code null}
     * @param messageType 消息类型，可为 {@code null}
     * @param status 状态：0-草稿，1-已发送，2-已撤回；可为 {@code null}
     * @return 消息分页结果
     */
    @Operation(summary = "查询消息列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('message:list')")
    public Result<PageResult<MessageResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String messageType,
                                                    @RequestParam(required = false) String status) {
        List<Message> messages = messageApplicationService.getMessagePage(title, messageType, status, pageNum,
                pageSize);
        long total = messageApplicationService.countMessages(title, messageType, status);
        List<MessageResponse> responses = messages.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<MessageResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询消息详情。
     *
     * @param id 消息 ID
     * @return 消息详情
     * @throws com.iboot.admin.common.exception.BusinessException 消息不存在时抛出
     */
    @Operation(summary = "查询消息详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('message:query')")
    public Result<MessageResponse> getById(@PathVariable Long id) {
        Message message = messageApplicationService.getMessageById(id);
        return Result.success(convertToResponse(message));
    }

    /**
     * 新增消息。
     *
     * @param request 消息创建请求
     * @return 创建后的消息详情
     */
    @Operation(summary = "新增消息")
    @PostMapping
    @PreAuthorize("hasAuthority('message:add')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.INSERT)
    public Result<MessageResponse> add(@Valid @RequestBody MessageRequest request) {
        Message message = convertToEntity(request);
        Message created = messageApplicationService.createMessage(message);
        return Result.success(convertToResponse(created));
    }

    /**
     * 修改消息。
     *
     * @param request 消息更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改消息")
    @PutMapping
    @PreAuthorize("hasAuthority('message:edit')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody MessageRequest request) {
        Message message = convertToEntity(request);
        message.setId(request.getId());
        messageApplicationService.updateMessage(message);
        return Result.success();
    }

    /**
     * 删除消息。
     *
     * @param id 消息 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除消息")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('message:remove')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        messageApplicationService.deleteMessage(id);
        return Result.success();
    }

    /**
     * 发送消息。
     *
     * @param request 发送请求，包含消息 ID 和接收用户 ID 列表
     * @return 无数据的成功响应
     */
    @Operation(summary = "发送消息")
    @PostMapping(version = "1", value = "/send")
    @PreAuthorize("hasAuthority('message:send')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.OTHER)
    public Result<Void> send(@Valid @RequestBody MessageSendRequest request) {
        messageApplicationService.sendMessage(request.getMessageId(), request.getUserIds());
        return Result.success();
    }

    /**
     * 撤回消息。
     *
     * @param id 消息 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "撤回消息")
    @PutMapping(version = "1", value = "/{id}/revoke")
    @PreAuthorize("hasAuthority('message:send')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> revoke(@PathVariable Long id) {
        messageApplicationService.revokeMessage(id);
        return Result.success();
    }

    /**
     * 导出消息列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param title 标题关键字，可为 {@code null}
     * @param messageType 消息类型，可为 {@code null}
     * @param status 状态：0-草稿，1-已发送，2-已撤回；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出消息列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('message:export')")
    @Log(title = "消息管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String title,
                       @RequestParam(required = false) String messageType,
                       @RequestParam(required = false) String status)
            throws IOException {
        List<Message> messages = messageApplicationService.getAllMessagesByCondition(title, messageType, status);
        List<MessageExportVO> exportList = messages.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, MessageExportVO.class, "消息列表", "消息数据");
    }

    // ==================== 用户端接口 ====================
    /**
     * 分页查询当前用户的收件箱。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param messageType 消息类型，可为 {@code null}
     * @param isRead 是否已读：0-未读，1-已读；可为 {@code null}
     * @return 收件箱分页结果
     */
    @Operation(summary = "获取用户收件箱")
    @GetMapping(version = "1", value = "/inbox")
    public Result<PageResult<UserMessageResponse>> inbox(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String messageType,
                                                         @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<MessageReceiver> receivers = messageApplicationService.getUserMessages(userId, messageType, isRead,
                pageNum, pageSize);
        long total = messageApplicationService.countUserMessages(userId, messageType, isRead);
        List<UserMessageResponse> responses = receivers.stream()
                .map(this::convertToUserMessageResponse)
                .collect(Collectors.toList());
        PageResult<UserMessageResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 获取当前用户的未读消息数量。
     *
     * @return 未读消息数量
     */
    @Operation(summary = "获取未读消息数量")
    @GetMapping(version = "1", value = "/unread/count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = messageApplicationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记单条消息已读。
     *
     * @param id 消息 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "标记消息已读")
    @PutMapping(version = "1", value = "/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageApplicationService.markAsRead(id, userId);
        return Result.success();
    }

    /**
     * 将当前用户的全部消息标记为已读。
     *
     * @return 无数据的成功响应
     */
    @Operation(summary = "全部标记已读")
    @PutMapping(version = "1", value = "/read/all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        messageApplicationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 在当前用户侧删除消息。
     *
     * <p>只设置用户侧删除标记，不影响其他接收者。
     *
     * @param id 消息 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "用户删除消息")
    @DeleteMapping(version = "1", value = "/inbox/{id}")
    public Result<Void> deleteUserMessage(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageApplicationService.deleteUserMessage(id, userId);
        return Result.success();
    }

    // ==================== 转换方法 ====================
    private Message convertToEntity(MessageRequest request) {
        return Message.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .messageType(request.getMessageType())
                .priority(request.getPriority())
                .receiverType(request.getReceiverType())
                .remark(request.getRemark())
                .build();
    }

    private MessageResponse convertToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .title(message.getTitle())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .priority(message.getPriority())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .receiverType(message.getReceiverType())
                .status(message.getStatus())
                .createBy(message.getCreateBy())
                .createTime(message.getCreateTime())
                .remark(message.getRemark())
                .build();
    }

    private UserMessageResponse convertToUserMessageResponse(MessageReceiver receiver) {
        UserMessageResponse response = UserMessageResponse.builder()
                .id(receiver.getMessageId())
                .title(receiver.getTitle())
                .content(receiver.getContent())
                .messageType(receiver.getMessageType())
                .priority(receiver.getPriority())
                .senderType(receiver.getSenderType())
                .senderId(receiver.getSenderId())
                .createBy(receiver.getCreateBy())
                .createTime(receiver.getCreateTime())
                .isRead(receiver.getIsRead())
                .readTime(receiver.getReadTime())
                .build();
        response.setBusinessType(receiver.getBusinessType());
        return response;
    }

    private MessageExportVO convertToExportVO(Message message) {
        return MessageExportVO.builder()
                .id(message.getId())
                .title(message.getTitle())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .priority(message.getPriority())
                .receiverType(message.getReceiverType())
                .status(message.getStatus())
                .createBy(message.getCreateBy())
                .remark(message.getRemark())
                .createTime(message.getCreateTime())
                .build();
    }

}
