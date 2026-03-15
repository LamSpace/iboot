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

import com.iboot.admin.domain.system.model.LoginLog;
import com.iboot.admin.domain.system.repository.LoginLogRepository;
import com.iboot.admin.infrastructure.persistence.mapper.LoginLogMapper;
import com.iboot.admin.infrastructure.persistence.po.LoginLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 登录日志仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现用户登录日志的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper loginLogMapper;

    /**
     * 保存登录日志
     *
     * @param loginLog 登录日志实体
     * @return 保存后的登录日志
     */
    @Override
    public LoginLog save(LoginLog loginLog) {
        LoginLogPO po = convertToPO(loginLog);
        loginLogMapper.insert(po);
        return convertToDomain(po);
    }

    /**
     * 根据 ID 查询登录日志
     *
     * @param id 登录日志 ID
     * @return 登录日志实体，不存在则返回空
     */
    @Override
    public Optional<LoginLog> findById(Long id) {
        LoginLogPO po = loginLogMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 分页查询登录日志
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 登录日志列表
     */
    @Override
    public List<LoginLog> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return loginLogMapper.selectPage(offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据条件分页查询登录日志
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 登录日志列表
     */
    @Override
    public List<LoginLog> findPageByCondition(String username, String ipAddress, Integer status,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return loginLogMapper.selectPageByCondition(username, ipAddress, status, startTime, endTime, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据条件查询所有登录日志（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志列表
     */
    @Override
    public List<LoginLog> findAllByCondition(String username, String ipAddress, Integer status,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.selectAllByCondition(username, ipAddress, status, startTime, endTime)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计登录日志总数
     *
     * @return 登录日志总数
     */
    @Override
    public long count() {
        return loginLogMapper.count();
    }

    /**
     * 根据条件统计登录日志总数
     *
     * @param username 用户名（可选）
     * @param ipAddress IP 地址（可选）
     * @param status 登录状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志总数
     */
    @Override
    public long countByCondition(String username, String ipAddress, Integer status,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        return loginLogMapper.countByCondition(username, ipAddress, status, startTime, endTime);
    }

    /**
     * 清理指定日期之前的日志
     *
     * @param beforeDate 清理该日期之前的日志
     * @return 清理的日志数量
     */
    @Override
    public int cleanLogBeforeDate(LocalDateTime beforeDate) {
        return loginLogMapper.deleteBeforeDate(beforeDate);
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param loginLog 登录日志领域对象
     * @return 登录日志持久化对象
     */
    private LoginLogPO convertToPO(LoginLog loginLog) {
        LoginLogPO po = new LoginLogPO();
        po.setId(loginLog.getId());
        po.setUsername(loginLog.getUsername());
        po.setIpAddress(loginLog.getIpAddress());
        po.setLoginLocation(loginLog.getLoginLocation());
        po.setBrowser(loginLog.getBrowser());
        po.setOs(loginLog.getOs());
        po.setStatus(loginLog.getStatus());
        po.setMessage(loginLog.getMessage());
        po.setLoginTime(loginLog.getLoginTime());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 登录日志持久化对象
     * @return 登录日志领域对象
     */
    private LoginLog convertToDomain(LoginLogPO po) {
        return LoginLog.builder()
                .id(po.getId())
                .username(po.getUsername())
                .ipAddress(po.getIpAddress())
                .loginLocation(po.getLoginLocation())
                .browser(po.getBrowser())
                .os(po.getOs())
                .status(po.getStatus())
                .message(po.getMessage())
                .loginTime(po.getLoginTime())
                .build();
    }
}
