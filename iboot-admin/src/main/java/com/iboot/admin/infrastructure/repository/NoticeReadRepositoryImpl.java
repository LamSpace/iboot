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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.system.model.NoticeRead;
import com.iboot.admin.domain.system.repository.NoticeReadRepository;
import com.iboot.admin.infrastructure.persistence.mapper.NoticeReadMapper;
import com.iboot.admin.infrastructure.persistence.po.NoticeReadPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 公告已读记录仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现公告已读记录的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class NoticeReadRepositoryImpl implements NoticeReadRepository {

    private final NoticeReadMapper noticeReadMapper;

    /**
     * 保存公告已读记录
     *
     * @param noticeRead 公告已读记录实体
     * @return 是否保存成功
     */
    @Override
    public boolean save(NoticeRead noticeRead) {
        NoticeReadPO po = new NoticeReadPO();
        po.setNoticeId(noticeRead.getNoticeId());
        po.setUserId(noticeRead.getUserId());
        po.setReadTime(noticeRead.getReadTime());
        return noticeReadMapper.insert(po) > 0;
    }

    /**
     * 检查用户是否已阅读指定公告
     *
     * @param noticeId 公告 ID
     * @param userId 用户 ID
     * @return 是否已读
     */
    @Override
    public boolean existsByNoticeIdAndUserId(Long noticeId, Long userId) {
        return noticeReadMapper.countByNoticeIdAndUserId(noticeId, userId) > 0;
    }

    /**
     * 统计用户未读公告数量
     *
     * @param userId 用户 ID
     * @return 未读公告数量
     */
    @Override
    public long countUnreadByUserId(Long userId) {
        return noticeReadMapper.countUnreadByUserId(userId);
    }

    /**
     * 查询用户未读的已发布公告 ID 列表
     *
     * @param userId 用户 ID
     * @param limit 返回数量限制
     * @return 未读公告 ID 列表
     */
    @Override
    public List<Long> findUnreadPublishedNoticeIds(Long userId, int limit) {
        return noticeReadMapper.selectUnreadPublishedNoticeIds(userId, limit);
    }

    /**
     * 根据公告 ID 删除已读记录
     *
     * @param noticeId 公告 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByNoticeId(Long noticeId) {
        return noticeReadMapper.deleteByNoticeId(noticeId) >= 0;
    }
}
