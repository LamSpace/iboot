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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 操作日志持久化对象
 * <p>
 * 对应数据库表：sys_operate_log
 * </p>
 *
 * @author iBoot
 */
public class OperateLogPO {

    /**
     * 日志 ID
     */
    private Long id;

    /**
     * 操作模块
     */
    private String title;

    /**
     * 操作类型：1-新增，2-修改，3-删除，4-导出，5-导入，6-详情，7-其他
     */
    private Integer businessType;

    /**
     * 方法名称
     */
    private String method;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 操作人类别：1-后台用户，2-手机端用户
     */
    private Integer operatorType;

    /**
     * 操作人员
     */
    private String operatorName;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 请求 URL
     */
    private String url;

    /**
     * 主机地址
     */
    private String ip;

    /**
     * 操作地点
     */
    private String location;

    /**
     * 请求参数
     */
    private String param;

    /**
     * 返回参数
     */
    private String result;

    /**
     * 操作状态：0-失败，1-成功
     */
    private Integer status;

    /**
     * 错误消息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 消耗时间（毫秒）
     */
    private Long costTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    @SuppressWarnings("all")
    public OperateLogPO() {
    }

    /**
     * 日志 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 日志 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 操作模块
     */
    @SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    /**
     * 操作模块
     */
    @SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 操作类型：1-新增，2-修改，3-删除，4-导出，5-导入，6-详情，7-其他
     */
    @SuppressWarnings("all")
    public Integer getBusinessType() {
        return this.businessType;
    }

    /**
     * 操作类型：1-新增，2-修改，3-删除，4-导出，5-导入，6-详情，7-其他
     */
    @SuppressWarnings("all")
    public void setBusinessType(final Integer businessType) {
        this.businessType = businessType;
    }

    /**
     * 方法名称
     */
    @SuppressWarnings("all")
    public String getMethod() {
        return this.method;
    }

    /**
     * 方法名称
     */
    @SuppressWarnings("all")
    public void setMethod(final String method) {
        this.method = method;
    }

    /**
     * 请求方式
     */
    @SuppressWarnings("all")
    public String getRequestMethod() {
        return this.requestMethod;
    }

    /**
     * 请求方式
     */
    @SuppressWarnings("all")
    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * 操作人类别：1-后台用户，2-手机端用户
     */
    @SuppressWarnings("all")
    public Integer getOperatorType() {
        return this.operatorType;
    }

    /**
     * 操作人类别：1-后台用户，2-手机端用户
     */
    @SuppressWarnings("all")
    public void setOperatorType(final Integer operatorType) {
        this.operatorType = operatorType;
    }

    /**
     * 操作人员
     */
    @SuppressWarnings("all")
    public String getOperatorName() {
        return this.operatorName;
    }

    /**
     * 操作人员
     */
    @SuppressWarnings("all")
    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    /**
     * 部门名称
     */
    @SuppressWarnings("all")
    public String getDeptName() {
        return this.deptName;
    }

    /**
     * 部门名称
     */
    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public Long getDeptId() {
        return this.deptId;
    }

    /**
     * 部门 ID
     */
    @SuppressWarnings("all")
    public void setDeptId(final Long deptId) {
        this.deptId = deptId;
    }

    /**
     * 请求 URL
     */
    @SuppressWarnings("all")
    public String getUrl() {
        return this.url;
    }

    /**
     * 请求 URL
     */
    @SuppressWarnings("all")
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * 主机地址
     */
    @SuppressWarnings("all")
    public String getIp() {
        return this.ip;
    }

    /**
     * 主机地址
     */
    @SuppressWarnings("all")
    public void setIp(final String ip) {
        this.ip = ip;
    }

    /**
     * 操作地点
     */
    @SuppressWarnings("all")
    public String getLocation() {
        return this.location;
    }

    /**
     * 操作地点
     */
    @SuppressWarnings("all")
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * 请求参数
     */
    @SuppressWarnings("all")
    public String getParam() {
        return this.param;
    }

    /**
     * 请求参数
     */
    @SuppressWarnings("all")
    public void setParam(final String param) {
        this.param = param;
    }

    /**
     * 返回参数
     */
    @SuppressWarnings("all")
    public String getResult() {
        return this.result;
    }

    /**
     * 返回参数
     */
    @SuppressWarnings("all")
    public void setResult(final String result) {
        this.result = result;
    }

    /**
     * 操作状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 操作状态：0-失败，1-成功
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    /**
     * 错误消息
     */
    @SuppressWarnings("all")
    public String getErrorMsg() {
        return this.errorMsg;
    }

    /**
     * 错误消息
     */
    @SuppressWarnings("all")
    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * 操作时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getOperateTime() {
        return this.operateTime;
    }

    /**
     * 操作时间
     */
    @SuppressWarnings("all")
    public void setOperateTime(final LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    /**
     * 消耗时间（毫秒）
     */
    @SuppressWarnings("all")
    public Long getCostTime() {
        return this.costTime;
    }

    /**
     * 消耗时间（毫秒）
     */
    @SuppressWarnings("all")
    public void setCostTime(final Long costTime) {
        this.costTime = costTime;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof OperateLogPO))
            return false;
        final OperateLogPO other = (OperateLogPO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType))
            return false;
        final java.lang.Object this$operatorType = this.getOperatorType();
        final java.lang.Object other$operatorType = other.getOperatorType();
        if (this$operatorType == null ? other$operatorType != null : !this$operatorType.equals(other$operatorType))
            return false;
        final java.lang.Object this$deptId = this.getDeptId();
        final java.lang.Object other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$costTime = this.getCostTime();
        final java.lang.Object other$costTime = other.getCostTime();
        if (this$costTime == null ? other$costTime != null : !this$costTime.equals(other$costTime))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title))
            return false;
        final java.lang.Object this$method = this.getMethod();
        final java.lang.Object other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method))
            return false;
        final java.lang.Object this$requestMethod = this.getRequestMethod();
        final java.lang.Object other$requestMethod = other.getRequestMethod();
        if (this$requestMethod == null ? other$requestMethod != null : !this$requestMethod.equals(other$requestMethod))
            return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName))
            return false;
        final java.lang.Object this$deptName = this.getDeptName();
        final java.lang.Object other$deptName = other.getDeptName();
        if (this$deptName == null ? other$deptName != null : !this$deptName.equals(other$deptName))
            return false;
        final java.lang.Object this$url = this.getUrl();
        final java.lang.Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url))
            return false;
        final java.lang.Object this$ip = this.getIp();
        final java.lang.Object other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip))
            return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location))
            return false;
        final java.lang.Object this$param = this.getParam();
        final java.lang.Object other$param = other.getParam();
        if (this$param == null ? other$param != null : !this$param.equals(other$param))
            return false;
        final java.lang.Object this$result = this.getResult();
        final java.lang.Object other$result = other.getResult();
        if (this$result == null ? other$result != null : !this$result.equals(other$result))
            return false;
        final java.lang.Object this$errorMsg = this.getErrorMsg();
        final java.lang.Object other$errorMsg = other.getErrorMsg();
        if (this$errorMsg == null ? other$errorMsg != null : !this$errorMsg.equals(other$errorMsg))
            return false;
        final java.lang.Object this$operateTime = this.getOperateTime();
        final java.lang.Object other$operateTime = other.getOperateTime();
        if (this$operateTime == null ? other$operateTime != null : !this$operateTime.equals(other$operateTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OperateLogPO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $operatorType = this.getOperatorType();
        result = result * PRIME + ($operatorType == null ? 43 : $operatorType.hashCode());
        final java.lang.Object $deptId = this.getDeptId();
        result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $costTime = this.getCostTime();
        result = result * PRIME + ($costTime == null ? 43 : $costTime.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $method = this.getMethod();
        result = result * PRIME + ($method == null ? 43 : $method.hashCode());
        final java.lang.Object $requestMethod = this.getRequestMethod();
        result = result * PRIME + ($requestMethod == null ? 43 : $requestMethod.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $deptName = this.getDeptName();
        result = result * PRIME + ($deptName == null ? 43 : $deptName.hashCode());
        final java.lang.Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final java.lang.Object $ip = this.getIp();
        result = result * PRIME + ($ip == null ? 43 : $ip.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final java.lang.Object $param = this.getParam();
        result = result * PRIME + ($param == null ? 43 : $param.hashCode());
        final java.lang.Object $result = this.getResult();
        result = result * PRIME + ($result == null ? 43 : $result.hashCode());
        final java.lang.Object $errorMsg = this.getErrorMsg();
        result = result * PRIME + ($errorMsg == null ? 43 : $errorMsg.hashCode());
        final java.lang.Object $operateTime = this.getOperateTime();
        result = result * PRIME + ($operateTime == null ? 43 : $operateTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "OperateLogPO(id=" + this.getId() + ", title=" + this.getTitle() + ", businessType="
                + this.getBusinessType() + ", method=" + this.getMethod() + ", requestMethod=" + this.getRequestMethod()
                + ", operatorType=" + this.getOperatorType() + ", operatorName=" + this.getOperatorName()
                + ", deptName=" + this.getDeptName() + ", deptId=" + this.getDeptId() + ", url=" + this.getUrl()
                + ", ip=" + this.getIp() + ", location=" + this.getLocation() + ", param=" + this.getParam()
                + ", result=" + this.getResult() + ", status=" + this.getStatus() + ", errorMsg=" + this.getErrorMsg()
                + ", operateTime=" + this.getOperateTime() + ", costTime=" + this.getCostTime() + ", deleted="
                + this.getDeleted() + ")";
    }

}
