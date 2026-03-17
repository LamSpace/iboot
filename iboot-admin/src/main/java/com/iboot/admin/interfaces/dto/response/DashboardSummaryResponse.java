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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Dashboard 汇总数据响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "Dashboard 汇总数据响应")
public class DashboardSummaryResponse {

    @Schema(description = "用户总数")
    private long userCount;

    @Schema(description = "角色总数")
    private long roleCount;

    @Schema(description = "部门总数")
    private long deptCount;

    @Schema(description = "岗位总数")
    private long postCount;

    @Schema(description = "登录日志总数")
    private long loginLogCount;

    @Schema(description = "操作日志总数")
    private long operateLogCount;

    @Schema(description = "最近登录日志")
    private List<LoginLogResponse> recentLoginLogs;

    @Schema(description = "最近操作日志")
    private List<OperateLogResponse> recentOperateLogs;

    @SuppressWarnings("all")
    public DashboardSummaryResponse() {
    }

    @SuppressWarnings("all")
    public DashboardSummaryResponse(final long userCount, final long roleCount, final long deptCount,
                                    final long postCount, final long loginLogCount, final long operateLogCount,
                                    final List<LoginLogResponse> recentLoginLogs, final List<OperateLogResponse> recentOperateLogs) {
        this.userCount = userCount;
        this.roleCount = roleCount;
        this.deptCount = deptCount;
        this.postCount = postCount;
        this.loginLogCount = loginLogCount;
        this.operateLogCount = operateLogCount;
        this.recentLoginLogs = recentLoginLogs;
        this.recentOperateLogs = recentOperateLogs;
    }

    @SuppressWarnings("all")
    public static DashboardSummaryResponse.DashboardSummaryResponseBuilder builder() {
        return new DashboardSummaryResponse.DashboardSummaryResponseBuilder();
    }

    @SuppressWarnings("all")
    public long getUserCount() {
        return this.userCount;
    }

    @SuppressWarnings("all")
    public void setUserCount(final long userCount) {
        this.userCount = userCount;
    }

    @SuppressWarnings("all")
    public long getRoleCount() {
        return this.roleCount;
    }

    @SuppressWarnings("all")
    public void setRoleCount(final long roleCount) {
        this.roleCount = roleCount;
    }

    @SuppressWarnings("all")
    public long getDeptCount() {
        return this.deptCount;
    }

    @SuppressWarnings("all")
    public void setDeptCount(final long deptCount) {
        this.deptCount = deptCount;
    }

    @SuppressWarnings("all")
    public long getPostCount() {
        return this.postCount;
    }

    @SuppressWarnings("all")
    public void setPostCount(final long postCount) {
        this.postCount = postCount;
    }

    @SuppressWarnings("all")
    public long getLoginLogCount() {
        return this.loginLogCount;
    }

    @SuppressWarnings("all")
    public void setLoginLogCount(final long loginLogCount) {
        this.loginLogCount = loginLogCount;
    }

    @SuppressWarnings("all")
    public long getOperateLogCount() {
        return this.operateLogCount;
    }

    @SuppressWarnings("all")
    public void setOperateLogCount(final long operateLogCount) {
        this.operateLogCount = operateLogCount;
    }

    @SuppressWarnings("all")
    public List<LoginLogResponse> getRecentLoginLogs() {
        return this.recentLoginLogs;
    }

    @SuppressWarnings("all")
    public void setRecentLoginLogs(final List<LoginLogResponse> recentLoginLogs) {
        this.recentLoginLogs = recentLoginLogs;
    }

    @SuppressWarnings("all")
    public List<OperateLogResponse> getRecentOperateLogs() {
        return this.recentOperateLogs;
    }

    @SuppressWarnings("all")
    public void setRecentOperateLogs(final List<OperateLogResponse> recentOperateLogs) {
        this.recentOperateLogs = recentOperateLogs;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DashboardSummaryResponse))
            return false;
        final DashboardSummaryResponse other = (DashboardSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (this.getUserCount() != other.getUserCount())
            return false;
        if (this.getRoleCount() != other.getRoleCount())
            return false;
        if (this.getDeptCount() != other.getDeptCount())
            return false;
        if (this.getPostCount() != other.getPostCount())
            return false;
        if (this.getLoginLogCount() != other.getLoginLogCount())
            return false;
        if (this.getOperateLogCount() != other.getOperateLogCount())
            return false;
        final java.lang.Object this$recentLoginLogs = this.getRecentLoginLogs();
        final java.lang.Object other$recentLoginLogs = other.getRecentLoginLogs();
        if (this$recentLoginLogs == null ? other$recentLoginLogs != null
                : !this$recentLoginLogs.equals(other$recentLoginLogs))
            return false;
        final java.lang.Object this$recentOperateLogs = this.getRecentOperateLogs();
        final java.lang.Object other$recentOperateLogs = other.getRecentOperateLogs();
        if (this$recentOperateLogs == null ? other$recentOperateLogs != null
                : !this$recentOperateLogs.equals(other$recentOperateLogs))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DashboardSummaryResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $userCount = this.getUserCount();
        result = result * PRIME + (int) ($userCount >>> 32 ^ $userCount);
        final long $roleCount = this.getRoleCount();
        result = result * PRIME + (int) ($roleCount >>> 32 ^ $roleCount);
        final long $deptCount = this.getDeptCount();
        result = result * PRIME + (int) ($deptCount >>> 32 ^ $deptCount);
        final long $postCount = this.getPostCount();
        result = result * PRIME + (int) ($postCount >>> 32 ^ $postCount);
        final long $loginLogCount = this.getLoginLogCount();
        result = result * PRIME + (int) ($loginLogCount >>> 32 ^ $loginLogCount);
        final long $operateLogCount = this.getOperateLogCount();
        result = result * PRIME + (int) ($operateLogCount >>> 32 ^ $operateLogCount);
        final java.lang.Object $recentLoginLogs = this.getRecentLoginLogs();
        result = result * PRIME + ($recentLoginLogs == null ? 43 : $recentLoginLogs.hashCode());
        final java.lang.Object $recentOperateLogs = this.getRecentOperateLogs();
        result = result * PRIME + ($recentOperateLogs == null ? 43 : $recentOperateLogs.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "DashboardSummaryResponse(userCount=" + this.getUserCount() + ", roleCount=" + this.getRoleCount()
                + ", deptCount=" + this.getDeptCount() + ", postCount=" + this.getPostCount() + ", loginLogCount="
                + this.getLoginLogCount() + ", operateLogCount=" + this.getOperateLogCount() + ", recentLoginLogs="
                + this.getRecentLoginLogs() + ", recentOperateLogs=" + this.getRecentOperateLogs() + ")";
    }

    @SuppressWarnings("all")
    public static class DashboardSummaryResponseBuilder {

        @SuppressWarnings("all")
        private long userCount;

        @SuppressWarnings("all")
        private long roleCount;

        @SuppressWarnings("all")
        private long deptCount;

        @SuppressWarnings("all")
        private long postCount;

        @SuppressWarnings("all")
        private long loginLogCount;

        @SuppressWarnings("all")
        private long operateLogCount;

        @SuppressWarnings("all")
        private List<LoginLogResponse> recentLoginLogs;

        @SuppressWarnings("all")
        private List<OperateLogResponse> recentOperateLogs;

        @SuppressWarnings("all")
        DashboardSummaryResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder userCount(final long userCount) {
            this.userCount = userCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder roleCount(final long roleCount) {
            this.roleCount = roleCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder deptCount(final long deptCount) {
            this.deptCount = deptCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder postCount(final long postCount) {
            this.postCount = postCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder loginLogCount(final long loginLogCount) {
            this.loginLogCount = loginLogCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder operateLogCount(final long operateLogCount) {
            this.operateLogCount = operateLogCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder recentLoginLogs(
                final List<LoginLogResponse> recentLoginLogs) {
            this.recentLoginLogs = recentLoginLogs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public DashboardSummaryResponse.DashboardSummaryResponseBuilder recentOperateLogs(
                final List<OperateLogResponse> recentOperateLogs) {
            this.recentOperateLogs = recentOperateLogs;
            return this;
        }

        @SuppressWarnings("all")
        public DashboardSummaryResponse build() {
            return new DashboardSummaryResponse(this.userCount, this.roleCount, this.deptCount, this.postCount,
                    this.loginLogCount, this.operateLogCount, this.recentLoginLogs, this.recentOperateLogs);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DashboardSummaryResponse.DashboardSummaryResponseBuilder(userCount=" + this.userCount
                    + ", roleCount=" + this.roleCount + ", deptCount=" + this.deptCount + ", postCount="
                    + this.postCount + ", loginLogCount=" + this.loginLogCount + ", operateLogCount="
                    + this.operateLogCount + ", recentLoginLogs=" + this.recentLoginLogs + ", recentOperateLogs="
                    + this.recentOperateLogs + ")";
        }

    }

}
