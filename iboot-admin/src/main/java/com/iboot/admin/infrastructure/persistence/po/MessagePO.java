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

package com.iboot.admin.infrastructure.persistence.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息持久化对象
 * <p>
 * 对应数据库表：sys_message
 * </p>
 *
 * @author iBoot
 */
@Data
public class MessagePO {

    /**
     * 消息 ID
     */
    private Long id;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：1-系统消息，2-用户消息
     */
    private String messageType;

    /**
     * 优先级：LOW-低，MEDIUM-中，HIGH-高
     */
    private String priority;

    /**
     * 发送者类型：1-用户，2-系统
     */
    private String senderType;

    /**
     * 发送者 ID
     */
    private Long senderId;

    /**
     * 接收者类型：1-指定用户，2-指定角色，3-全体成员
     */
    private String receiverType;

    /**
     * 消息状态：0-草稿，1-已发送，2-已撤回
     */
    private String status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;
}
