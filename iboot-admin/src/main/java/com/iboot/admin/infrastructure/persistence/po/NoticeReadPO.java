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
 * 公告已读记录持久化对象
 * <p>
 * 对应数据库表：sys_notice_read
 * </p>
 *
 * @author iBoot
 */
@Data
public class NoticeReadPO {

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 公告 ID
     */
    private Long noticeId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
}
