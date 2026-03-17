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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.NoticeRead;

import java.util.List;

/**
 * 公告已读记录仓储接口
 * <p>
 * 负责公告已读记录的持久化操作，用于跟踪用户阅读公告的状态
 * </p>
 *
 * @author iBoot
 */
public interface NoticeReadRepository {

    /**
     * 保存公告已读记录
     *
     * @param noticeRead 公告已读记录实体
     *
     * @return 是否保存成功
     */
    boolean save(NoticeRead noticeRead);

    /**
     * 检查用户是否已阅读指定公告
     *
     * @param noticeId 公告 ID
     * @param userId   用户 ID
     *
     * @return 是否已读
     */
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    /**
     * 统计用户未读公告数量
     *
     * @param userId 用户 ID
     *
     * @return 未读公告数量
     */
    long countUnreadByUserId(Long userId);

    /**
     * 查询用户未读的已发布公告 ID 列表
     *
     * @param userId 用户 ID
     * @param limit  返回数量限制
     *
     * @return 未读公告 ID 列表
     */
    List<Long> findUnreadPublishedNoticeIds(Long userId, int limit);

    /**
     * 根据公告 ID 删除已读记录
     * <p>
     * 用于公告删除时级联清理已读记录
     * </p>
     *
     * @param noticeId 公告 ID
     *
     * @return 是否删除成功
     */
    boolean deleteByNoticeId(Long noticeId);

}
