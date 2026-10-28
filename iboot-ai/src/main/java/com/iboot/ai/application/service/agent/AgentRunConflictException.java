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

package com.iboot.ai.application.service.agent;

/**
 * 运行状态不允许当前操作。
 *
 * <p>典型场景：重复确认、确认已完成或已拒绝的运行。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class AgentRunConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 创建异常。
     *
     * @param message 说明
     */
    public AgentRunConflictException(String message) {
        super(message);
    }
}
