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
 * 系统公告实体
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {

    /**
     * 公告ID
     */
    private Long id;

    /**
     * 公告标题
     */
    private String noticeTitle;

    /**
     * 公告类型（字典 sys_notice_type）
     */
    private String noticeType;

    /**
     * 公告内容
     */
    private String noticeContent;

    /**
     * 状态（字典 sys_notice_status）：0-草稿，1-已发布，2-已撤回
     */
    private String status;

    /**
     * 是否置顶：0-否，1-是
     */
    private Integer isTop;

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

    /**
     * 发布公告
     */
    public void publish() {
        this.status = "1";
    }

    /**
     * 撤回公告
     */
    public void revoke() {
        this.status = "2";
    }

    /**
     * 是否为草稿状态
     */
    public boolean isDraft() {
        return "0".equals(this.status);
    }

    /**
     * 是否为已发布状态
     */
    public boolean isPublished() {
        return "1".equals(this.status);
    }

    /**
     * 是否为已撤回状态
     */
    public boolean isRevoked() {
        return "2".equals(this.status);
    }
}
