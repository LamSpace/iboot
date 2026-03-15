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

package com.iboot.admin.domain.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息接收记录实体
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReceiver {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 接收者用户ID
     */
    private Long userId;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 用户侧删除：0-未删除，1-已删除
     */
    private Integer isDeleted;

    /**
     * 标记已读
     */
    public void markAsRead() {
        this.isRead = 1;
        this.readTime = LocalDateTime.now();
    }

    /**
     * 用户侧软删除
     */
    public void softDelete() {
        this.isDeleted = 1;
    }
}
