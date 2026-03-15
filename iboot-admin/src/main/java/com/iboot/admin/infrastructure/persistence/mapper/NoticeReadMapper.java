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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.infrastructure.persistence.po.NoticeReadPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告已读记录 Mapper 接口
 * <p>
 * 负责公告已读记录的数据库操作，跟踪用户阅读公告的状态
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface NoticeReadMapper {

    /**
     * 插入公告已读记录
     *
     * @param noticeReadPO 公告已读记录持久化对象
     * @return 影响的记录数
     */
    int insert(NoticeReadPO noticeReadPO);

    /**
     * 根据公告 ID 和用户 ID 统计数量
     *
     * @param noticeId 公告 ID
     * @param userId 用户 ID
     * @return 数量
     */
    int countByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    /**
     * 统计用户未读公告数量
     *
     * @param userId 用户 ID
     * @return 未读公告数量
     */
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 查询用户未读的已发布公告 ID 列表
     *
     * @param userId 用户 ID
     * @param limit 返回数量限制
     * @return 未读公告 ID 列表
     */
    List<Long> selectUnreadPublishedNoticeIds(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 根据公告 ID 删除已读记录
     *
     * @param noticeId 公告 ID
     * @return 影响的记录数
     */
    int deleteByNoticeId(@Param("noticeId") Long noticeId);
}
