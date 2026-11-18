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

package com.iboot.system.application.service;

import com.iboot.system.domain.system.model.User;
import com.iboot.system.domain.system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户生命周期与个人访问令牌联动的测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserApplicationServicePatTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersonalAccessTokenRevoker revoker;

    /**
     * 停用用户应撤销其全部 PAT。
     */
    @Test
    void disablingUserRevokesTokens() {
        UserApplicationService service = service();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(userRepository.update(any())).thenReturn(true);

        service.changeStatus(7L, 0);

        verify(revoker).revokeAll(7L);
    }

    /**
     * 启用用户不应恢复或再次撤销旧令牌。
     */
    @Test
    void enablingUserDoesNotRevokeTokens() {
        UserApplicationService service = service();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(userRepository.update(any())).thenReturn(true);

        service.changeStatus(7L, 1);

        verify(revoker, never()).revokeAll(7L);
    }

    /**
     * 删除用户应撤销其全部 PAT。
     */
    @Test
    void deletingUserRevokesTokens() {
        UserApplicationService service = service();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(userRepository.deleteById(7L)).thenReturn(true);

        service.deleteUser(7L);

        verify(revoker).revokeAll(7L);
    }

    /**
     * 撤销失败必须向上抛出，让用户状态变更事务回滚。
     */
    @Test
    void revocationFailureAbortsUserChange() {
        UserApplicationService service = service();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(userRepository.update(any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(revoker).revokeAll(7L);

        assertThatThrownBy(() -> service.changeStatus(7L, 0))
                .isInstanceOf(IllegalStateException.class);
    }

    private UserApplicationService service() {
        return new UserApplicationService(userRepository, passwordEncoder, revoker);
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("tester");
        user.setUserType(2);
        return user;
    }
}
