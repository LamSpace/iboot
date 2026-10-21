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

import com.iboot.admin.infrastructure.persistence.po.LeaveApplicationPO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 请假申请 MyBatis Mapper。
 *
 * <p>只负责单表读写，不包含业务规则；查询默认过滤逻辑删除记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveApplicationMapper {

    /**
     * 新增请假申请。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int insert(LeaveApplicationPO po);

    /**
     * 更新申请状态与审批意见。
     *
     * @param po 持久化对象
     * @return 受影响行数
     */
    int update(LeaveApplicationPO po);

    /**
     * 按 ID 查询。
     *
     * @param id 申请 ID
     * @return 持久化对象，不存在时为 {@code null}
     */
    LeaveApplicationPO selectById(@Param("id") Long id);

    /**
     * 按流程实例 ID 查询。
     *
     * @param processInstanceId 流程实例 ID
     * @return 持久化对象，不存在时为 {@code null}
     */
    LeaveApplicationPO selectByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    /**
     * 按流程实例 ID 集合查询。
     *
     * @param processInstanceIds 流程实例 ID 集合
     * @return 持久化对象列表
     */
    List<LeaveApplicationPO> selectByProcessInstanceIds(@Param("processInstanceIds") Collection<String> processInstanceIds);

    /**
     * 分页查询指定申请人的申请。
     *
     * @param applicantId 申请人用户 ID
     * @param status      状态，可为 {@code null}
     * @param offset      起始偏移
     * @param limit       每页数量
     * @return 持久化对象列表
     */
    List<LeaveApplicationPO> selectByApplicant(@Param("applicantId") Long applicantId,
                                               @Param("status") String status,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    /**
     * 统计指定申请人的申请数量。
     *
     * @param applicantId 申请人用户 ID
     * @param status      状态，可为 {@code null}
     * @return 记录数
     */
    long countByApplicant(@Param("applicantId") Long applicantId, @Param("status") String status);
}
