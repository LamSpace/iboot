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

package com.iboot.auth.interfaces.controller;

import com.iboot.auth.application.service.PersonalAccessTokenService;
import com.iboot.auth.domain.model.PersonalAccessTokenCreated;
import com.iboot.auth.domain.model.PersonalAccessTokenSummary;
import com.iboot.auth.interfaces.dto.request.CreatePersonalAccessTokenRequest;
import com.iboot.auth.interfaces.dto.response.PersonalAccessTokenCreatedResponse;
import com.iboot.auth.interfaces.dto.response.PersonalAccessTokenResponse;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.security.SecurityUtils;
import com.iboot.kernel.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 个人访问令牌管理控制器。
 *
 * <p>提供当前用户自己令牌的签发、列表与撤销接口。接口要求交互式登录会话；
 * 使用 PAT 调用会被拒绝，避免泄漏的令牌自行续期或扩散。签发响应包含只出现一次的
 * 令牌明文，因此操作日志不记录响应体。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see PersonalAccessTokenService
 */
@Tag(name = "个人访问令牌", description = "签发、查看与撤销用于 CI 与本地自动化的访问令牌")
@RestController
@RequestMapping("/api/auth/tokens")
public class PersonalAccessTokenController {

    private final PersonalAccessTokenService tokenService;

    /**
     * 创建控制器。
     *
     * @param tokenService 令牌应用服务
     */
    public PersonalAccessTokenController(PersonalAccessTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 签发个人访问令牌。
     *
     * @param request 创建请求
     * @return 新令牌，包含只出现一次的明文
     * @throws AccessDeniedException 使用 PAT 调用时抛出
     */
    @Operation(summary = "签发个人访问令牌", description = "令牌明文只在本次响应中返回一次")
    @PostMapping(version = "1")
    @Log(title = "个人访问令牌", businessType = BusinessTypeEnum.INSERT, isSaveResponseData = false)
    public Result<PersonalAccessTokenCreatedResponse> create(
            @Valid @RequestBody CreatePersonalAccessTokenRequest request) {
        rejectPat();
        PersonalAccessTokenCreated created = tokenService.create(
                SecurityUtils.getCurrentUserId(), request.getName(), request.getExpiresDays(),
                request.getScopes());
        return Result.success(new PersonalAccessTokenCreatedResponse(created.id(), created.name(),
                created.scopes(), created.token(), created.createdAt(), created.expiresAt()));
    }

    /**
     * 列出当前用户的个人访问令牌。
     *
     * @return 令牌元数据列表，不包含明文
     * @throws AccessDeniedException 使用 PAT 调用时抛出
     */
    @Operation(summary = "列出个人访问令牌", description = "只返回元数据，不返回令牌明文")
    @GetMapping(version = "1")
    public Result<List<PersonalAccessTokenResponse>> list() {
        rejectPat();
        List<PersonalAccessTokenResponse> tokens = tokenService.list(SecurityUtils.getCurrentUserId())
                .stream()
                .map(this::toResponse)
                .toList();
        return Result.success(tokens);
    }

    /**
     * 撤销当前用户的一条个人访问令牌。
     *
     * @param id 记录 ID
     * @return 无数据的成功响应
     * @throws AccessDeniedException 使用 PAT 调用时抛出
     */
    @Operation(summary = "撤销个人访问令牌")
    @DeleteMapping(version = "1", value = "/{id}")
    @Log(title = "个人访问令牌", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> revoke(@PathVariable Long id) {
        rejectPat();
        tokenService.revoke(SecurityUtils.getCurrentUserId(), id);
        return Result.success("撤销成功", null);
    }

    /**
     * 把服务层结果转换为响应 DTO。
     *
     * @param summary 令牌摘要
     * @return 响应 DTO
     */
    private PersonalAccessTokenResponse toResponse(PersonalAccessTokenSummary summary) {
        return new PersonalAccessTokenResponse(summary.id(), summary.name(), summary.scopes(),
                summary.status(), summary.createdAt(), summary.expiresAt(), summary.lastUsedAt());
    }

    /**
     * 拒绝来自个人访问令牌的管理请求。
     *
     * @throws AccessDeniedException 当前请求使用 PAT 时抛出
     */
    private void rejectPat() {
        if (SecurityUtils.isPersonalAccessToken()) {
            throw new AccessDeniedException("个人访问令牌不能管理个人访问令牌");
        }
    }
}
