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

package com.iboot.ai.application.service;

/**
 * 配额超限异常。
 *
 * <p>由控制器映射为 HTTP 429；{@code scope} 用于区分全局与单用户配额。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class QuotaExceededException extends RuntimeException {

    private final String scope;

    /**
     * 创建异常。
     *
     * @param scope   拒绝范围
     * @param message 提示
     */
    public QuotaExceededException(String scope, String message) {
        super(message);
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }
}
