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

package com.iboot.ai.domain.agent;

/**
 * 一次性写操作授权。
 *
 * <p>由确认接口在校验运行归属、有效期与参数摘要后签发，只对同一工具与同一参数摘要有效。
 * 工具执行时据此判断是否允许真正调用下游，授权本身不来自模型输出。
 *
 * @param toolName   被授权的工具名
 * @param argsDigest 被授权参数的 SHA-256 摘要
 * @author iBoot Team
 * @since 1.0.0
 */
public record ApprovalGrant(String toolName, String argsDigest) {

    /**
     * 判断授权是否覆盖本次调用。
     *
     * @param name   工具名
     * @param digest 参数摘要
     * @return 完全匹配返回 {@code true}
     */
    public boolean matches(String name, String digest) {
        return toolName != null && toolName.equals(name)
                && argsDigest != null && argsDigest.equals(digest);
    }
}
