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

package com.iboot.cli.http;

/**
 * 当前请求使用的访问令牌来源。
 *
 * <p>命令层通过该接口把令牌解析与 HTTP 发送解耦；测试可直接返回固定令牌。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface TokenProvider {

    /**
     * 获取当前访问令牌。
     *
     * @return 访问令牌；未登录时返回 {@code null}
     */
    String accessToken();
}
