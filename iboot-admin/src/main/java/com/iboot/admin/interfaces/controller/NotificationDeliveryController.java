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

import com.iboot.admin.application.service.notification.NotificationApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.domain.notification.model.NotificationDelivery;
import com.iboot.admin.interfaces.dto.response.NotificationDeliveryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知投递管理控制器。
 *
 * <p>提供投递记录分页查询与人工重试，供管理端排障使用。响应中的接收人和错误摘要
 * 已脱敏，不包含任何渠道密钥。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see NotificationApplicationService
 */
@Tag(name = "通知投递", description = "通知投递记录查询与人工重试")
@RestController
@RequestMapping("/api/notification/delivery")
public class NotificationDeliveryController {

    private final NotificationApplicationService notificationApplicationService;

    @SuppressWarnings("all")
    public NotificationDeliveryController(final NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    /**
     * 分页查询投递记录。
     *
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页数量
     * @param status    状态筛选，可为 {@code null}
     * @param channel   渠道筛选，可为 {@code null}
     * @param eventType 事件类型筛选，可为 {@code null}
     * @param recipient 接收人关键字，可为 {@code null}
     * @param startTime 创建时间下界，可为 {@code null}
     * @param endTime   创建时间上界，可为 {@code null}
     * @return 投递记录分页结果
     */
    @Operation(summary = "查询通知投递记录")
    @GetMapping(version = "1")
    @PreAuthorize("hasAuthority('notification:delivery:list')")
    public Result<PageResult<NotificationDeliveryResponse>> list(
            @RequestParam(defaultValue = "1") final Integer pageNum,
            @RequestParam(defaultValue = "10") final Integer pageSize,
            @RequestParam(required = false) final String status,
            @RequestParam(required = false) final String channel,
            @RequestParam(required = false) final String eventType,
            @RequestParam(required = false) final String recipient,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime) {
        List<NotificationDelivery> deliveries = notificationApplicationService.findPage(status, channel, eventType,
                recipient, startTime, endTime, pageNum, pageSize);
        long total = notificationApplicationService.count(status, channel, eventType, recipient, startTime, endTime);
        List<NotificationDeliveryResponse> rows = deliveries.stream()
                .map(NotificationDeliveryResponse::from)
                .toList();
        return Result.success(new PageResult<>(rows, total, pageNum, pageSize));
    }

    /**
     * 人工重试一条失败或终止的投递。
     *
     * @param id 投递记录 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "人工重试通知投递")
    @PostMapping(version = "1", value = "/{id}/retry")
    @PreAuthorize("hasAuthority('notification:delivery:retry')")
    @Log(title = "通知投递", businessType = BusinessTypeEnum.OTHER)
    public Result<Void> retry(@PathVariable final Long id) {
        notificationApplicationService.retryManually(id);
        return Result.success();
    }
}
