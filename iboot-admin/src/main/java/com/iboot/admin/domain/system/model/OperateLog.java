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

package com.iboot.admin.domain.system.model;

import java.time.LocalDateTime;

/**
 * 操作日志实体（值对象）
 *
 * @author iBoot
 */
public class OperateLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 模块标题
     */
    private String title;

    /**
     * 业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
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
     * 操作类别：0-其它，1-后台用户，2-手机端用户
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
     * 请求URL
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
     * 耗时（毫秒）
     */
    private Long costTime;

    @SuppressWarnings("all")
    public OperateLog() {
    }

    /**
     * Creates a new {@code OperateLog} instance.
     *
     * @param id            日志ID
     * @param title         模块标题
     * @param businessType  业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
     * @param method        方法名称
     * @param requestMethod 请求方式
     * @param operatorType  操作类别：0-其它，1-后台用户，2-手机端用户
     * @param operatorName  操作人员
     * @param deptName      部门名称
     * @param url           请求URL
     * @param ip            主机地址
     * @param location      操作地点
     * @param param         请求参数
     * @param result        返回参数
     * @param status        操作状态：0-失败，1-成功
     * @param errorMsg      错误消息
     * @param operateTime   操作时间
     * @param costTime      耗时（毫秒）
     */
    @SuppressWarnings("all")
    public OperateLog(final Long id, final String title, final Integer businessType, final String method,
                      final String requestMethod, final Integer operatorType, final String operatorName, final String deptName,
                      final String url, final String ip, final String location, final String param, final String result,
                      final Integer status, final String errorMsg, final LocalDateTime operateTime, final Long costTime) {
        this.id = id;
        this.title = title;
        this.businessType = businessType;
        this.method = method;
        this.requestMethod = requestMethod;
        this.operatorType = operatorType;
        this.operatorName = operatorName;
        this.deptName = deptName;
        this.url = url;
        this.ip = ip;
        this.location = location;
        this.param = param;
        this.result = result;
        this.status = status;
        this.errorMsg = errorMsg;
        this.operateTime = operateTime;
        this.costTime = costTime;
    }

    @SuppressWarnings("all")
    public static OperateLog.OperateLogBuilder builder() {
        return new OperateLog.OperateLogBuilder();
    }

    /**
     * 检查是否操作成功
     */
    public boolean isSuccess() {
        return this.status != null && this.status == 1;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 日志ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 模块标题
     */
    @SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    /**
     * 模块标题
     */
    @SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
     */
    @SuppressWarnings("all")
    public Integer getBusinessType() {
        return this.businessType;
    }

    /**
     * 业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
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
     * 操作类别：0-其它，1-后台用户，2-手机端用户
     */
    @SuppressWarnings("all")
    public Integer getOperatorType() {
        return this.operatorType;
    }

    /**
     * 操作类别：0-其它，1-后台用户，2-手机端用户
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
     * 请求URL
     */
    @SuppressWarnings("all")
    public String getUrl() {
        return this.url;
    }

    /**
     * 请求URL
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
     * 耗时（毫秒）
     */
    @SuppressWarnings("all")
    public Long getCostTime() {
        return this.costTime;
    }

    /**
     * 耗时（毫秒）
     */
    @SuppressWarnings("all")
    public void setCostTime(final Long costTime) {
        this.costTime = costTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof OperateLog))
            return false;
        final OperateLog other = (OperateLog) o;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$costTime = this.getCostTime();
        final java.lang.Object other$costTime = other.getCostTime();
        if (this$costTime == null ? other$costTime != null : !this$costTime.equals(other$costTime))
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
        return other instanceof OperateLog;
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
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $costTime = this.getCostTime();
        result = result * PRIME + ($costTime == null ? 43 : $costTime.hashCode());
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
        return "OperateLog(id=" + this.getId() + ", title=" + this.getTitle() + ", businessType="
                + this.getBusinessType() + ", method=" + this.getMethod() + ", requestMethod=" + this.getRequestMethod()
                + ", operatorType=" + this.getOperatorType() + ", operatorName=" + this.getOperatorName()
                + ", deptName=" + this.getDeptName() + ", url=" + this.getUrl() + ", ip=" + this.getIp() + ", location="
                + this.getLocation() + ", param=" + this.getParam() + ", result=" + this.getResult() + ", status="
                + this.getStatus() + ", errorMsg=" + this.getErrorMsg() + ", operateTime=" + this.getOperateTime()
                + ", costTime=" + this.getCostTime() + ")";
    }

    @SuppressWarnings("all")
    public static class OperateLogBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String title;

        @SuppressWarnings("all")
        private Integer businessType;

        @SuppressWarnings("all")
        private String method;

        @SuppressWarnings("all")
        private String requestMethod;

        @SuppressWarnings("all")
        private Integer operatorType;

        @SuppressWarnings("all")
        private String operatorName;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private String url;

        @SuppressWarnings("all")
        private String ip;

        @SuppressWarnings("all")
        private String location;

        @SuppressWarnings("all")
        private String param;

        @SuppressWarnings("all")
        private String result;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String errorMsg;

        @SuppressWarnings("all")
        private LocalDateTime operateTime;

        @SuppressWarnings("all")
        private Long costTime;

        @SuppressWarnings("all")
        OperateLogBuilder() {
        }

        /**
         * 日志ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 模块标题
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * 业务类型：0-其它，1-新增，2-修改，3-删除，4-查询，5-导出，6-导入
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder businessType(final Integer businessType) {
            this.businessType = businessType;
            return this;
        }

        /**
         * 方法名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * 请求方式
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder requestMethod(final String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        /**
         * 操作类别：0-其它，1-后台用户，2-手机端用户
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder operatorType(final Integer operatorType) {
            this.operatorType = operatorType;
            return this;
        }

        /**
         * 操作人员
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder operatorName(final String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        /**
         * 部门名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * 请求URL
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder url(final String url) {
            this.url = url;
            return this;
        }

        /**
         * 主机地址
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder ip(final String ip) {
            this.ip = ip;
            return this;
        }

        /**
         * 操作地点
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder location(final String location) {
            this.location = location;
            return this;
        }

        /**
         * 请求参数
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder param(final String param) {
            this.param = param;
            return this;
        }

        /**
         * 返回参数
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder result(final String result) {
            this.result = result;
            return this;
        }

        /**
         * 操作状态：0-失败，1-成功
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 错误消息
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder errorMsg(final String errorMsg) {
            this.errorMsg = errorMsg;
            return this;
        }

        /**
         * 操作时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder operateTime(final LocalDateTime operateTime) {
            this.operateTime = operateTime;
            return this;
        }

        /**
         * 耗时（毫秒）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLog.OperateLogBuilder costTime(final Long costTime) {
            this.costTime = costTime;
            return this;
        }

        @SuppressWarnings("all")
        public OperateLog build() {
            return new OperateLog(this.id, this.title, this.businessType, this.method, this.requestMethod,
                    this.operatorType, this.operatorName, this.deptName, this.url, this.ip, this.location, this.param,
                    this.result, this.status, this.errorMsg, this.operateTime, this.costTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "OperateLog.OperateLogBuilder(id=" + this.id + ", title=" + this.title + ", businessType="
                    + this.businessType + ", method=" + this.method + ", requestMethod=" + this.requestMethod
                    + ", operatorType=" + this.operatorType + ", operatorName=" + this.operatorName + ", deptName="
                    + this.deptName + ", url=" + this.url + ", ip=" + this.ip + ", location=" + this.location
                    + ", param=" + this.param + ", result=" + this.result + ", status=" + this.status + ", errorMsg="
                    + this.errorMsg + ", operateTime=" + this.operateTime + ", costTime=" + this.costTime + ")";
        }

    }

}
