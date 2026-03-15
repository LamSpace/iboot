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

package com.iboot.admin.infrastructure.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推送事件定义
 * <p>
 * 与 CloudEvents 规范兼容，用于 SSE 推送
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件 ID，确保唯一性
     */
    private String id;

    /**
     * 事件类型（与 CloudEventTypes 对应）
     */
    private String type;

    /**
     * 事件源
     */
    private String source;

    /**
     * 事件发生时间
     */
    private String time;

    /**
     * 数据内容类型
     */
    @Builder.Default
    private String dataContentType = "application/json";

    /**
     * 实际的业务数据
     */
    private Object data;

    /**
     * 目标用户 ID（用于单播推送）
     */
    private Long targetUserId;

    /**
     * 生成事件 ID
     *
     * @return 事件 ID
     */
    private static String generateId() {
        return "push-" + java.util.UUID.randomUUID();
    }
}
