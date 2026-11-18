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
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人访问令牌控制器测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class PersonalAccessTokenControllerTest {

    private final PersonalAccessTokenService tokenService = mock(PersonalAccessTokenService.class);

    private final PersonalAccessTokenController controller =
            new PersonalAccessTokenController(tokenService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 交互式会话签发后返回一次明文。
     */
    @Test
    void createsTokenForInteractiveSession() {
        authenticate(false);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        when(tokenService.create(7L, "ci", 30L, List.of("user:list"))).thenReturn(
                new PersonalAccessTokenCreated(42L, "ci", List.of("user:list"), "jwt-token",
                        LocalDateTime.now(), expiresAt));
        CreatePersonalAccessTokenRequest request = new CreatePersonalAccessTokenRequest();
        request.setName("ci");
        request.setExpiresDays(30L);
        request.setScopes(List.of("user:list"));

        Result<PersonalAccessTokenCreatedResponse> result = controller.create(request);

        assertThat(result.getData().token()).isEqualTo("jwt-token");
        assertThat(result.getData().id()).isEqualTo(42L);
        assertThat(result.getData().scopes()).containsExactly("user:list");
    }

    /**
     * 列表只返回元数据。
     */
    @Test
    void listsMetadata() {
        authenticate(false);
        when(tokenService.list(7L)).thenReturn(List.of(
                new PersonalAccessTokenSummary(1L, "ci", List.of("user:list"),
                        PersonalAccessTokenService.STATUS_ACTIVE,
                        LocalDateTime.now(), LocalDateTime.now().plusDays(1), null)));

        Result<List<PersonalAccessTokenResponse>> result = controller.list();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).name()).isEqualTo("ci");
        assertThat(result.getData().get(0).scopes()).containsExactly("user:list");
        assertThat(result.getData().get(0).status()).isEqualTo("ACTIVE");
    }

    /**
     * 撤销委托给服务层。
     */
    @Test
    void revokesOwnToken() {
        authenticate(false);

        controller.revoke(42L);

        verify(tokenService).revoke(7L, 42L);
    }

    /**
     * PAT 不能签发、查看或撤销 PAT。
     */
    @Test
    void rejectsPatManagement() {
        authenticate(true);
        CreatePersonalAccessTokenRequest request = new CreatePersonalAccessTokenRequest();
        request.setName("ci");

        assertThatThrownBy(() -> controller.create(request))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(controller::list)
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.revoke(42L))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * 创建接口的操作日志不能记录包含明文的响应体。
     *
     * @throws NoSuchMethodException 方法签名变化时抛出
     */
    @Test
    void createLogDoesNotSaveResponseBody() throws NoSuchMethodException {
        Method method = PersonalAccessTokenController.class.getMethod(
                "create", CreatePersonalAccessTokenRequest.class);

        Log log = method.getAnnotation(Log.class);

        assertThat(log).isNotNull();
        assertThat(log.isSaveResponseData()).isFalse();
    }

    private void authenticate(boolean pat) {
        Map<String, Object> principal = new HashMap<>();
        principal.put(Constants.USER_ID, 7L);
        principal.put("username", "tester");
        principal.put(Constants.PAT_CLAIM, pat);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
