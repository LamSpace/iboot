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

import com.iboot.admin.infrastructure.persistence.po.NoticePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统公告 Mapper 接口
 * <p>
 * 负责系统公告的数据库操作，支持公告的发布和管理
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface NoticeMapper {

    /**
     * 插入公告记录
     *
     * @param noticePO 公告持久化对象
     * @return 影响的记录数
     */
    int insert(NoticePO noticePO);

    /**
     * 更新公告记录
     *
     * @param noticePO 公告持久化对象
     * @return 影响的记录数
     */
    int update(NoticePO noticePO);

    /**
     * 根据 ID 删除公告
     *
     * @param id 公告 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询公告
     *
     * @param id 公告 ID
     * @return 公告持久化对象
     */
    NoticePO selectById(@Param("id") Long id);

    /**
     * 按条件分页查询公告
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 公告列表
     */
    List<NoticePO> selectPageByCondition(@Param("noticeTitle") String noticeTitle,
                                          @Param("noticeType") String noticeType,
                                          @Param("status") String status,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    /**
     * 按条件统计公告数量
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告数量
     */
    long countByCondition(@Param("noticeTitle") String noticeTitle,
                          @Param("noticeType") String noticeType,
                          @Param("status") String status);

    /**
     * 按条件查询所有公告（导出用）
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告列表
     */
    List<NoticePO> selectAllByCondition(@Param("noticeTitle") String noticeTitle,
                                        @Param("noticeType") String noticeType,
                                        @Param("status") String status);

    /**
     * 查询已发布的公告列表
     *
     * @param limit 返回数量限制
     * @return 已发布公告列表
     */
    List<NoticePO> selectPublishedNotices(@Param("limit") int limit);

    /**
     * 统计已发布公告数量
     *
     * @return 已发布公告数量
     */
    long countTopPublishedNotices();
}
