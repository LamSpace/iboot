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

package com.iboot.kernel.security;

import io.jsonwebtoken.JwtException;

/**
 * 令牌版本已停用异常。
 *
 * <p>双栈窗口关闭后，客户端仍携带旧的对称签名令牌时抛出。之所以与普通认证失败区分，
 * 是为了让前端可以静默重新登录，而不是提示「用户名或密码错误」。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class TokenVersionRetiredException extends JwtException {

    private static final long serialVersionUID = 1L;

    /**
     * 创建异常。
     *
     * @param message 异常说明
     */
    public TokenVersionRetiredException(String message) {
        super(message);
    }
}
