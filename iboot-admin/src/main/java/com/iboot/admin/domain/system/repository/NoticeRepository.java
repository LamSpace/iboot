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

import com.iboot.admin.domain.system.model.Notice;

import java.util.List;
import java.util.Optional;

/**
 * 公告仓储接口
 * <p>
 * 负责系统公告的持久化操作，支持公告的发布、管理和查询
 * </p>
 *
 * @author iBoot
 */
public interface NoticeRepository {

    /**
     * 保存公告
     *
     * @param notice 公告实体
     * @return 保存后的公告
     */
    Notice save(Notice notice);

    /**
     * 更新公告
     *
     * @param notice 公告实体
     * @return 是否更新成功
     */
    boolean update(Notice notice);

    /**
     * 根据 ID 删除公告（逻辑删除）
     *
     * @param id 公告 ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询公告
     *
     * @param id 公告 ID
     * @return 公告实体，不存在则返回空
     */
    Optional<Notice> findById(Long id);

    /**
     * 按条件分页查询公告
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 公告列表
     */
    List<Notice> findPageByCondition(String noticeTitle, String noticeType, String status, int pageNum, int pageSize);

    /**
     * 按条件统计公告数量
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告数量
     */
    long countByCondition(String noticeTitle, String noticeType, String status);

    /**
     * 按条件查询所有公告（导出用）
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告列表
     */
    List<Notice> findAllByCondition(String noticeTitle, String noticeType, String status);

    /**
     * 查询已发布的公告列表
     * <p>
     * 用于首页展示最新的通知公告
     * </p>
     *
     * @param limit 返回数量限制
     * @return 已发布公告列表
     */
    List<Notice> findPublishedNotices(int limit);

    /**
     * 统计已发布公告数量
     *
     * @return 已发布公告数量
     */
    long countTopPublishedNotices();
}
