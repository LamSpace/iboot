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

import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.domain.system.repository.NoticeRepository;
import com.iboot.admin.infrastructure.persistence.mapper.NoticeMapper;
import com.iboot.admin.infrastructure.persistence.po.NoticePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 公告仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现公告数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class NoticeRepositoryImpl implements NoticeRepository {

    private final NoticeMapper noticeMapper;

    /**
     * 保存公告
     * <p>
     * 插入新公告记录并设置生成的 ID
     * </p>
     *
     * @param notice 公告实体
     * @return 保存后的公告
     */
    @Override
    public Notice save(Notice notice) {
        NoticePO po = convertToPO(notice);
        noticeMapper.insert(po);
        notice.setId(po.getId());
        return notice;
    }

    /**
     * 更新公告
     *
     * @param notice 公告实体
     * @return 是否更新成功
     */
    @Override
    public boolean update(Notice notice) {
        NoticePO po = convertToPO(notice);
        return noticeMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除公告（逻辑删除）
     *
     * @param id 公告 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return noticeMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询公告
     *
     * @param id 公告 ID
     * @return 公告实体，不存在则返回空
     */
    @Override
    public Optional<Notice> findById(Long id) {
        NoticePO po = noticeMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

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
    @Override
    public List<Notice> findPageByCondition(String noticeTitle, String noticeType, String status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return noticeMapper.selectPageByCondition(noticeTitle, noticeType, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计公告总数
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告总数
     */
    @Override
    public long countByCondition(String noticeTitle, String noticeType, String status) {
        return noticeMapper.countByCondition(noticeTitle, noticeType, status);
    }

    /**
     * 按条件查询所有公告（不分页，用于导出）
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告列表
     */
    @Override
    public List<Notice> findAllByCondition(String noticeTitle, String noticeType, String status) {
        return noticeMapper.selectAllByCondition(noticeTitle, noticeType, status)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 查询已发布的公告（按发布时间倒序，限制数量）
     *
     * @param limit 最大返回数量
     * @return 已发布公告列表
     */
    @Override
    public List<Notice> findPublishedNotices(int limit) {
        return noticeMapper.selectPublishedNotices(limit)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计最新发布的前 N 条已发布公告数量
     *
     * @return 公告数量
     */
    @Override
    public long countTopPublishedNotices() {
        return noticeMapper.countTopPublishedNotices();
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 公告持久化对象
     * @return 公告领域对象
     */
    private Notice convertToDomain(NoticePO po) {
        return Notice.builder()
                .id(po.getId())
                .noticeTitle(po.getNoticeTitle())
                .noticeType(po.getNoticeType())
                .noticeContent(po.getNoticeContent())
                .status(po.getStatus())
                .isTop(po.getIsTop())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .deleted(po.getDeleted())
                .remark(po.getRemark())
                .build();
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param domain 公告领域对象
     * @return 公告持久化对象
     */
    private NoticePO convertToPO(Notice domain) {
        NoticePO po = new NoticePO();
        po.setId(domain.getId());
        po.setNoticeTitle(domain.getNoticeTitle());
        po.setNoticeType(domain.getNoticeType());
        po.setNoticeContent(domain.getNoticeContent());
        po.setStatus(domain.getStatus());
        po.setIsTop(domain.getIsTop());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        po.setDeleted(domain.getDeleted());
        po.setRemark(domain.getRemark());
        return po;
    }
}
