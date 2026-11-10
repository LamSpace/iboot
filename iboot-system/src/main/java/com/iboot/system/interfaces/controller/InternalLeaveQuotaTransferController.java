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
import com.iboot.system.application.service.tcc.LeaveQuotaTransferTccAction;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;
import com.iboot.system.interfaces.dto.request.LeaveQuotaTransferTryRequest;
import com.iboot.system.interfaces.dto.response.LeaveQuotaTransferResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 额度划转内部接口。
 *
 * <p>仅供审批服务在全局事务内调用，不经网关暴露。使用 {@code X-Internal-Token}
 * 共享密钥鉴权；必须携带 {@code TX_XID}，且本服务启用分布式事务，否则失败关闭。
 *
 * <p>Confirm/Cancel 由 Seata 协调器直接回调 {@link LeaveQuotaTransferTccAction}，
 * 不经过本控制器。该路径不在 {@code /api/**} 下，不套 CloudEvents 响应信封。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Hidden
@RestController
@RequestMapping("/internal/leave/quota/transfer")
public class InternalLeaveQuotaTransferController {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private static final String XID_HEADER = "TX_XID";

    private final LeaveQuotaTransferTccAction leaveQuotaTransferTccAction;

    private final String internalToken;

    private final boolean seataEnabled;

    /**
     * 创建内部接口控制器。
     *
     * @param leaveQuotaTransferTccAction 额度划转 TCC 动作
     * @param internalToken               内部共享密钥
     * @param seataEnabled                是否启用分布式事务
     */
    public InternalLeaveQuotaTransferController(
            final LeaveQuotaTransferTccAction leaveQuotaTransferTccAction,
            @Value("${iboot.leave-quota.internal-token:}") final String internalToken,
            @Value("${seata.enabled:true}") final boolean seataEnabled) {
        this.leaveQuotaTransferTccAction = leaveQuotaTransferTccAction;
        this.internalToken = internalToken;
        this.seataEnabled = seataEnabled;
    }

    /**
     * 执行额度划转的 Try。
     *
     * @param token   内部共享密钥
     * @param xid     全局事务标识，可为 {@code null}
     * @param request Try 请求
     * @return 划转标识与初始状态；密钥无效时为 401，缺少全局事务或未启用分布式事务时为 503
     */
    @PostMapping("/try")
    public ResponseEntity<Result<LeaveQuotaTransferResponse>> tryTransfer(
            @RequestHeader(value = TOKEN_HEADER, required = false) final String token,
            @RequestHeader(value = XID_HEADER, required = false) final String xid,
            @Valid @RequestBody final LeaveQuotaTransferTryRequest request) {
        if (!isAuthorized(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "内部令牌无效"));
        }
        if (!seataEnabled) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.error(503, "系统服务未启用分布式事务"));
        }
        if (xid == null || xid.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.error(503, "缺少全局事务标识，额度划转未执行"));
        }
        leaveQuotaTransferTccAction.tryTransfer(request.getTransferId(), request.getFromUserId(),
                request.getToUserId(), request.getYear(), request.getCount());
        return ResponseEntity.ok(Result.success(new LeaveQuotaTransferResponse(
                request.getTransferId(), LeaveQuotaTransferStatus.TRYING.name())));
    }

    private boolean isAuthorized(final String token) {
        if (internalToken == null || internalToken.isBlank() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
