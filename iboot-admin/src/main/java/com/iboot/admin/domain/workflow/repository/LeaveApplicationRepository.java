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

package com.iboot.admin.domain.workflow.repository;

import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 请假申请仓储接口。
 *
 * <p>查询约定：列表方法无数据时返回空列表；分页参数 {@code offset} 从 0 开始；
 * 按 ID 查询返回 {@link Optional}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveApplicationRepository {

    /**
     * 新增请假申请。
     *
     * @param application 申请实体
     * @return 保存后的实体，包含生成的 ID
     */
    LeaveApplication save(LeaveApplication application);

    /**
     * 更新请假申请。
     *
     * @param application 申请实体
     * @return 是否更新成功
     */
    boolean update(LeaveApplication application);

    /**
     * 按 ID 查询申请。
     *
     * @param id 申请 ID
     * @return 申请实体，不存在时为空
     */
    Optional<LeaveApplication> findById(Long id);

    /**
     * 按流程实例 ID 查询申请。
     *
     * @param processInstanceId 流程实例 ID
     * @return 申请实体，不存在时为空
     */
    Optional<LeaveApplication> findByProcessInstanceId(String processInstanceId);

    /**
     * 按流程实例 ID 批量查询申请。
     *
     * @param processInstanceIds 流程实例 ID 集合
     * @return 申请列表，无匹配时为空列表
     */
    List<LeaveApplication> findByProcessInstanceIds(Collection<String> processInstanceIds);

    /**
     * 分页查询指定申请人的申请。
     *
     * @param applicantId 申请人用户 ID
     * @param status      状态筛选，可为 {@code null}
     * @param offset      起始偏移，从 0 开始
     * @param limit       每页数量
     * @return 申请列表
     */
    List<LeaveApplication> findByApplicant(Long applicantId, LeaveStatus status, int offset, int limit);

    /**
     * 统计指定申请人的申请数量。
     *
     * @param applicantId 申请人用户 ID
     * @param status      状态筛选，可为 {@code null}
     * @return 记录数
     */
    long countByApplicant(Long applicantId, LeaveStatus status);
}
