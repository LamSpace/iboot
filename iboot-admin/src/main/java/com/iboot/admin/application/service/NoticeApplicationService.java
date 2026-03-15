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

package com.iboot.admin.application.service;

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Notice;
import com.iboot.admin.domain.system.model.NoticeRead;
import com.iboot.admin.domain.system.repository.NoticeReadRepository;
import com.iboot.admin.domain.system.repository.NoticeRepository;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统公告应用服务
 * <p>
 * 负责公告的创建、编辑、发布、撤回、删除和查询等业务逻辑处理。
 * 支持公告状态管理（草稿/已发布）、置顶控制、已读记录管理等功能。
 * 公告最大显示数量和置顶数量上限通过 sys_config 参数配置动态读取。
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeApplicationService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final ConfigApplicationService configApplicationService;

    private static final String CONFIG_KEY_MAX_DISPLAY = "sys.notice.maxDisplay";
    private static final String CONFIG_KEY_TOP_COUNT = "sys.notice.topCount";
    private static final int DEFAULT_MAX_DISPLAY = 5;
    private static final int DEFAULT_TOP_COUNT = 3;

    // ==================== 后台管理方法 ====================

    /**
     * 创建公告（默认草稿状态）
     * <p>
     * 创建公告时默认状态为草稿（status="0"），设置创建人和创建时间。
     * isTop 未指定时默认为 0（不置顶）。
     * </p>
     *
     * @param notice 公告实体
     * @return 创建后的公告实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Notice createNotice(Notice notice) {
        notice.setStatus("0");
        notice.setCreateBy(SecurityUtils.getCurrentUsername());
        notice.setCreateTime(LocalDateTime.now());
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        return noticeRepository.save(notice);
    }

    /**
     * 更新公告
     * <p>
     * 仅草稿状态的公告可以修改，已发布的公告需先撤回才能修改。
     * </p>
     *
     * @param notice 公告实体
     * @return 是否更新成功
     * @throws BusinessException 当公告不存在或已发布时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNotice(Notice notice) {
        Notice existing = noticeRepository.findById(notice.getId())
                .orElseThrow(() -> new BusinessException("公告不存在"));

        if (existing.isPublished()) {
            throw new BusinessException("已发布的公告不能修改，请先撤回");
        }

        notice.setUpdateBy(SecurityUtils.getCurrentUsername());
        notice.setUpdateTime(LocalDateTime.now());
        return noticeRepository.update(notice);
    }

    /**
     * 删除公告
     * <p>
     * 仅草稿状态的公告可以删除，已发布的公告需先撤回。
     * 删除时会同时删除关联的已读记录。
     * </p>
     *
     * @param id 公告 ID
     * @return 是否删除成功
     * @throws BusinessException 当公告不存在或已发布时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));

        if (notice.isPublished()) {
            throw new BusinessException("已发布的公告不能删除，请先撤回");
        }

        // 删除关联的已读记录
        noticeReadRepository.deleteByNoticeId(id);
        return noticeRepository.deleteById(id);
    }

    /**
     * 发布公告
     * <p>
     * 将草稿状态的公告发布，发布后用户可见。
     * 检查置顶数量限制，超过上限则不允许置顶发布。
     * </p>
     *
     * @param id 公告 ID
     * @return 是否发布成功
     * @throws BusinessException 当公告不存在、已发布或置顶数量超限时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean publishNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));

        if (notice.isPublished()) {
            throw new BusinessException("公告已经是发布状态");
        }

        // 检查置顶数量限制
        if (notice.getIsTop() != null && notice.getIsTop() == 1) {
            int maxTopCount = getConfigIntValue(CONFIG_KEY_TOP_COUNT, DEFAULT_TOP_COUNT);
            long currentTopCount = noticeRepository.countTopPublishedNotices();
            if (currentTopCount >= maxTopCount) {
                throw new BusinessException("置顶公告数量已达上限（最多" + maxTopCount + "条）");
            }
        }

        notice.publish();
        notice.setUpdateBy(SecurityUtils.getCurrentUsername());
        notice.setUpdateTime(LocalDateTime.now());
        return noticeRepository.update(notice);
    }

    /**
     * 撤回公告
     * <p>
     * 将已发布的公告撤回为草稿状态，撤回后用户不可见。
     * </p>
     *
     * @param id 公告 ID
     * @return 是否撤回成功
     * @throws BusinessException 当公告不存在或未发布时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));

        if (!notice.isPublished()) {
            throw new BusinessException("只有已发布的公告才能撤回");
        }

        notice.revoke();
        notice.setUpdateBy(SecurityUtils.getCurrentUsername());
        notice.setUpdateTime(LocalDateTime.now());
        return noticeRepository.update(notice);
    }

    /**
     * 根据 ID 获取公告
     *
     * @param id 公告 ID
     * @return 公告实体
     * @throws BusinessException 当公告不存在时抛出
     */
    public Notice getNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));
    }

    /**
     * 分页查询公告（条件）
     * <p>
     * 支持按公告标题、类型和状态进行条件查询。
     * </p>
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 公告列表
     */
    public List<Notice> getNoticePage(String noticeTitle, String noticeType, String status, int pageNum, int pageSize) {
        return noticeRepository.findPageByCondition(noticeTitle, noticeType, status, pageNum, pageSize);
    }

    /**
     * 统计公告数量（条件）
     * <p>
     * 支持按公告标题、类型和状态进行条件统计。
     * </p>
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告总数
     */
    public long countNotices(String noticeTitle, String noticeType, String status) {
        return noticeRepository.countByCondition(noticeTitle, noticeType, status);
    }

    /**
     * 按条件获取所有公告（导出用）
     * <p>
     * 不分页获取所有符合条件的公告，用于数据导出。
     * </p>
     *
     * @param noticeTitle 公告标题（可选）
     * @param noticeType 公告类型（可选）
     * @param status 状态（可选）
     * @return 公告列表
     */
    public List<Notice> getAllNoticesByCondition(String noticeTitle, String noticeType, String status) {
        return noticeRepository.findAllByCondition(noticeTitle, noticeType, status);
    }

    // ==================== 用户端方法 ====================

    /**
     * 获取已发布公告列表
     * <p>
     * 获取已发布的公告列表，按置顶和发布时间排序。
     * 最大显示数量通过 sys.notice.maxDisplay 配置控制。
     * </p>
     *
     * @return 已发布公告列表
     */
    public List<Notice> getPublishedNotices() {
        int maxDisplay = getConfigIntValue(CONFIG_KEY_MAX_DISPLAY, DEFAULT_MAX_DISPLAY);
        return noticeRepository.findPublishedNotices(maxDisplay);
    }

    /**
     * 获取用户未读公告数量
     *
     * @param userId 用户 ID
     * @return 未读公告数量
     */
    public long getUnreadCount(Long userId) {
        return noticeReadRepository.countUnreadByUserId(userId);
    }

    /**
     * 标记公告已读
     * <p>
     * 用户阅读公告后标记为已读，记录已读时间。
     * 仅已发布的公告可以标记已读，重复标记会跳过。
     * </p>
     *
     * @param noticeId 公告 ID
     * @param userId 用户 ID
     * @throws BusinessException 当公告不存在或未发布时抛出
     */
    public void markAsRead(Long noticeId, Long userId) {
        // 检查公告是否存在且已发布
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException("公告不存在"));

        if (!notice.isPublished()) {
            throw new BusinessException("公告未发布");
        }

        // 已读则跳过（INSERT IGNORE 也会处理，这里做前置校验）
        if (noticeReadRepository.existsByNoticeIdAndUserId(noticeId, userId)) {
            return;
        }

        NoticeRead noticeRead = NoticeRead.builder()
                .noticeId(noticeId)
                .userId(userId)
                .readTime(LocalDateTime.now())
                .build();
        noticeReadRepository.save(noticeRead);
    }

    /**
     * 检查用户是否已读某公告
     *
     * @param noticeId 公告 ID
     * @param userId 用户 ID
     * @return 是否已读
     */
    public boolean isRead(Long noticeId, Long userId) {
        return noticeReadRepository.existsByNoticeIdAndUserId(noticeId, userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 从系统配置获取整数值，若配置不存在或格式错误则使用默认值
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private int getConfigIntValue(String configKey, int defaultValue) {
        String value = configApplicationService.getConfigValue(configKey);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置项 {} 的值 {} 不是有效的整数，使用默认值 {}", configKey, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
