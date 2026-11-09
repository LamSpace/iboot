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

package com.iboot.system.interfaces.controller;

import com.iboot.kernel.web.Result;
import com.iboot.system.application.service.LeaveQuotaApplicationService;
import com.iboot.system.domain.system.model.LeaveQuota;
import com.iboot.system.interfaces.dto.request.LeaveQuotaDeductRequest;
import com.iboot.system.interfaces.dto.response.LeaveQuotaResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 请假额度内部接口。
 *
 * <p>仅供审批服务调用，不经网关暴露。使用 {@code X-Internal-Token} 共享密钥鉴权，
 * 密钥缺失或不匹配时失败关闭。请求携带 {@code TX_XID} 但本服务未启用分布式事务时
 * 拒绝请求，避免在本地提交却不登记全局事务分支。
 *
 * <p>该路径不在 {@code /api/**} 下，因此不套 CloudEvents 响应信封。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Hidden
@RestController
@RequestMapping("/internal/leave/quota")
public class InternalLeaveQuotaController {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private static final String XID_HEADER = "TX_XID";

    private final LeaveQuotaApplicationService leaveQuotaApplicationService;

    private final String internalToken;

    private final boolean seataEnabled;

    /**
     * 创建内部接口控制器。
     *
     * @param leaveQuotaApplicationService 额度应用服务
     * @param internalToken                内部共享密钥
     * @param seataEnabled                 是否启用分布式事务
     */
    public InternalLeaveQuotaController(
            final LeaveQuotaApplicationService leaveQuotaApplicationService,
            @Value("${iboot.leave-quota.internal-token:}") final String internalToken,
            @Value("${seata.enabled:true}") final boolean seataEnabled) {
        this.leaveQuotaApplicationService = leaveQuotaApplicationService;
        this.internalToken = internalToken;
        this.seataEnabled = seataEnabled;
    }

    /**
     * 按申请 ID 幂等扣减一次额度。
     *
     * @param token   内部共享密钥
     * @param xid     全局事务标识，可为 {@code null}
     * @param request 扣减请求
     * @return 扣减后的额度；密钥无效时为 401，参与方未启用分布式事务时为 503
     */
    @PostMapping("/deduct")
    public ResponseEntity<Result<LeaveQuotaResponse>> deduct(
            @RequestHeader(value = TOKEN_HEADER, required = false) final String token,
            @RequestHeader(value = XID_HEADER, required = false) final String xid,
            @Valid @RequestBody final LeaveQuotaDeductRequest request) {
        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "内部令牌无效"));
        }
        if (xid != null && !seataEnabled) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.error(503, "系统服务未启用分布式事务"));
        }
        LeaveQuota quota = leaveQuotaApplicationService.deduct(
                request.getRequestId(), request.getUserId(), request.getYear());
        return ResponseEntity.ok(Result.success(LeaveQuotaResponse.from(quota)));
    }

    /**
     * 查询指定用户的年度额度。
     *
     * @param token  内部共享密钥
     * @param userId 用户 ID
     * @param year   年度
     * @return 额度视图；密钥无效时为 401
     */
    @GetMapping
    public ResponseEntity<Result<LeaveQuotaResponse>> get(
            @RequestHeader(value = TOKEN_HEADER, required = false) final String token,
            @RequestParam final Long userId,
            @RequestParam final int year) {
        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "内部令牌无效"));
        }
        LeaveQuota quota = leaveQuotaApplicationService.view(userId, year);
        return ResponseEntity.ok(Result.success(LeaveQuotaResponse.from(quota)));
    }

    private boolean isAuthorized(final String token) {
        if (internalToken == null || internalToken.isBlank() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
