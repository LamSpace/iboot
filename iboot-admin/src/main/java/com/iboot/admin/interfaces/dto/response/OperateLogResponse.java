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

import java.time.LocalDateTime;

/**
 * 操作日志响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "操作日志响应")
public class OperateLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "业务类型：0-其它，1-新增，2-修改，3-删除，4-授权，5-导出，6-导入，7-强退，8-清空")
    private Integer businessType;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "操作人员")
    private String operatorName;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "请求URL")
    private String operUrl;

    @Schema(description = "操作IP")
    private String operIp;

    @Schema(description = "操作地点")
    private String operLocation;

    @Schema(description = "请求参数")
    private String operParam;

    @Schema(description = "返回结果")
    private String jsonResult;

    @Schema(description = "操作状态：0-失败，1-成功")
    private Integer status;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "操作耗时（毫秒）")
    private Long costTime;

    @Schema(description = "操作时间")
    private LocalDateTime operTime;

    @SuppressWarnings("all")
    public OperateLogResponse() {
    }

    @SuppressWarnings("all")
    public OperateLogResponse(final Long id, final String title, final Integer businessType, final String method,
                              final String requestMethod, final String operatorName, final String deptName, final String operUrl,
                              final String operIp, final String operLocation, final String operParam, final String jsonResult,
                              final Integer status, final String errorMsg, final Long costTime, final LocalDateTime operTime) {
        this.id = id;
        this.title = title;
        this.businessType = businessType;
        this.method = method;
        this.requestMethod = requestMethod;
        this.operatorName = operatorName;
        this.deptName = deptName;
        this.operUrl = operUrl;
        this.operIp = operIp;
        this.operLocation = operLocation;
        this.operParam = operParam;
        this.jsonResult = jsonResult;
        this.status = status;
        this.errorMsg = errorMsg;
        this.costTime = costTime;
        this.operTime = operTime;
    }

    @SuppressWarnings("all")
    public static OperateLogResponse.OperateLogResponseBuilder builder() {
        return new OperateLogResponse.OperateLogResponseBuilder();
    }

    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("all")
    public String getTitle() {
        return this.title;
    }

    @SuppressWarnings("all")
    public void setTitle(final String title) {
        this.title = title;
    }

    @SuppressWarnings("all")
    public Integer getBusinessType() {
        return this.businessType;
    }

    @SuppressWarnings("all")
    public void setBusinessType(final Integer businessType) {
        this.businessType = businessType;
    }

    @SuppressWarnings("all")
    public String getMethod() {
        return this.method;
    }

    @SuppressWarnings("all")
    public void setMethod(final String method) {
        this.method = method;
    }

    @SuppressWarnings("all")
    public String getRequestMethod() {
        return this.requestMethod;
    }

    @SuppressWarnings("all")
    public void setRequestMethod(final String requestMethod) {
        this.requestMethod = requestMethod;
    }

    @SuppressWarnings("all")
    public String getOperatorName() {
        return this.operatorName;
    }

    @SuppressWarnings("all")
    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    @SuppressWarnings("all")
    public String getDeptName() {
        return this.deptName;
    }

    @SuppressWarnings("all")
    public void setDeptName(final String deptName) {
        this.deptName = deptName;
    }

    @SuppressWarnings("all")
    public String getOperUrl() {
        return this.operUrl;
    }

    @SuppressWarnings("all")
    public void setOperUrl(final String operUrl) {
        this.operUrl = operUrl;
    }

    @SuppressWarnings("all")
    public String getOperIp() {
        return this.operIp;
    }

    @SuppressWarnings("all")
    public void setOperIp(final String operIp) {
        this.operIp = operIp;
    }

    @SuppressWarnings("all")
    public String getOperLocation() {
        return this.operLocation;
    }

    @SuppressWarnings("all")
    public void setOperLocation(final String operLocation) {
        this.operLocation = operLocation;
    }

    @SuppressWarnings("all")
    public String getOperParam() {
        return this.operParam;
    }

    @SuppressWarnings("all")
    public void setOperParam(final String operParam) {
        this.operParam = operParam;
    }

    @SuppressWarnings("all")
    public String getJsonResult() {
        return this.jsonResult;
    }

    @SuppressWarnings("all")
    public void setJsonResult(final String jsonResult) {
        this.jsonResult = jsonResult;
    }

    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @SuppressWarnings("all")
    public String getErrorMsg() {
        return this.errorMsg;
    }

    @SuppressWarnings("all")
    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @SuppressWarnings("all")
    public Long getCostTime() {
        return this.costTime;
    }

    @SuppressWarnings("all")
    public void setCostTime(final Long costTime) {
        this.costTime = costTime;
    }

    @SuppressWarnings("all")
    public LocalDateTime getOperTime() {
        return this.operTime;
    }

    @SuppressWarnings("all")
    public void setOperTime(final LocalDateTime operTime) {
        this.operTime = operTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof OperateLogResponse))
            return false;
        final OperateLogResponse other = (OperateLogResponse) o;
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
        final java.lang.Object this$operUrl = this.getOperUrl();
        final java.lang.Object other$operUrl = other.getOperUrl();
        if (this$operUrl == null ? other$operUrl != null : !this$operUrl.equals(other$operUrl))
            return false;
        final java.lang.Object this$operIp = this.getOperIp();
        final java.lang.Object other$operIp = other.getOperIp();
        if (this$operIp == null ? other$operIp != null : !this$operIp.equals(other$operIp))
            return false;
        final java.lang.Object this$operLocation = this.getOperLocation();
        final java.lang.Object other$operLocation = other.getOperLocation();
        if (this$operLocation == null ? other$operLocation != null : !this$operLocation.equals(other$operLocation))
            return false;
        final java.lang.Object this$operParam = this.getOperParam();
        final java.lang.Object other$operParam = other.getOperParam();
        if (this$operParam == null ? other$operParam != null : !this$operParam.equals(other$operParam))
            return false;
        final java.lang.Object this$jsonResult = this.getJsonResult();
        final java.lang.Object other$jsonResult = other.getJsonResult();
        if (this$jsonResult == null ? other$jsonResult != null : !this$jsonResult.equals(other$jsonResult))
            return false;
        final java.lang.Object this$errorMsg = this.getErrorMsg();
        final java.lang.Object other$errorMsg = other.getErrorMsg();
        if (this$errorMsg == null ? other$errorMsg != null : !this$errorMsg.equals(other$errorMsg))
            return false;
        final java.lang.Object this$operTime = this.getOperTime();
        final java.lang.Object other$operTime = other.getOperTime();
        if (this$operTime == null ? other$operTime != null : !this$operTime.equals(other$operTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OperateLogResponse;
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
        final java.lang.Object $operUrl = this.getOperUrl();
        result = result * PRIME + ($operUrl == null ? 43 : $operUrl.hashCode());
        final java.lang.Object $operIp = this.getOperIp();
        result = result * PRIME + ($operIp == null ? 43 : $operIp.hashCode());
        final java.lang.Object $operLocation = this.getOperLocation();
        result = result * PRIME + ($operLocation == null ? 43 : $operLocation.hashCode());
        final java.lang.Object $operParam = this.getOperParam();
        result = result * PRIME + ($operParam == null ? 43 : $operParam.hashCode());
        final java.lang.Object $jsonResult = this.getJsonResult();
        result = result * PRIME + ($jsonResult == null ? 43 : $jsonResult.hashCode());
        final java.lang.Object $errorMsg = this.getErrorMsg();
        result = result * PRIME + ($errorMsg == null ? 43 : $errorMsg.hashCode());
        final java.lang.Object $operTime = this.getOperTime();
        result = result * PRIME + ($operTime == null ? 43 : $operTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "OperateLogResponse(id=" + this.getId() + ", title=" + this.getTitle() + ", businessType="
                + this.getBusinessType() + ", method=" + this.getMethod() + ", requestMethod=" + this.getRequestMethod()
                + ", operatorName=" + this.getOperatorName() + ", deptName=" + this.getDeptName() + ", operUrl="
                + this.getOperUrl() + ", operIp=" + this.getOperIp() + ", operLocation=" + this.getOperLocation()
                + ", operParam=" + this.getOperParam() + ", jsonResult=" + this.getJsonResult() + ", status="
                + this.getStatus() + ", errorMsg=" + this.getErrorMsg() + ", costTime=" + this.getCostTime()
                + ", operTime=" + this.getOperTime() + ")";
    }

    @SuppressWarnings("all")
    public static class OperateLogResponseBuilder {

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
        private String operatorName;

        @SuppressWarnings("all")
        private String deptName;

        @SuppressWarnings("all")
        private String operUrl;

        @SuppressWarnings("all")
        private String operIp;

        @SuppressWarnings("all")
        private String operLocation;

        @SuppressWarnings("all")
        private String operParam;

        @SuppressWarnings("all")
        private String jsonResult;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String errorMsg;

        @SuppressWarnings("all")
        private Long costTime;

        @SuppressWarnings("all")
        private LocalDateTime operTime;

        @SuppressWarnings("all")
        OperateLogResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder title(final String title) {
            this.title = title;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder businessType(final Integer businessType) {
            this.businessType = businessType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder requestMethod(final String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operatorName(final String operatorName) {
            this.operatorName = operatorName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder deptName(final String deptName) {
            this.deptName = deptName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operUrl(final String operUrl) {
            this.operUrl = operUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operIp(final String operIp) {
            this.operIp = operIp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operLocation(final String operLocation) {
            this.operLocation = operLocation;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operParam(final String operParam) {
            this.operParam = operParam;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder jsonResult(final String jsonResult) {
            this.jsonResult = jsonResult;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder errorMsg(final String errorMsg) {
            this.errorMsg = errorMsg;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder costTime(final Long costTime) {
            this.costTime = costTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public OperateLogResponse.OperateLogResponseBuilder operTime(final LocalDateTime operTime) {
            this.operTime = operTime;
            return this;
        }

        @SuppressWarnings("all")
        public OperateLogResponse build() {
            return new OperateLogResponse(this.id, this.title, this.businessType, this.method, this.requestMethod,
                    this.operatorName, this.deptName, this.operUrl, this.operIp, this.operLocation, this.operParam,
                    this.jsonResult, this.status, this.errorMsg, this.costTime, this.operTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "OperateLogResponse.OperateLogResponseBuilder(id=" + this.id + ", title=" + this.title
                    + ", businessType=" + this.businessType + ", method=" + this.method + ", requestMethod="
                    + this.requestMethod + ", operatorName=" + this.operatorName + ", deptName=" + this.deptName
                    + ", operUrl=" + this.operUrl + ", operIp=" + this.operIp + ", operLocation=" + this.operLocation
                    + ", operParam=" + this.operParam + ", jsonResult=" + this.jsonResult + ", status=" + this.status
                    + ", errorMsg=" + this.errorMsg + ", costTime=" + this.costTime + ", operTime=" + this.operTime
                    + ")";
        }

    }

}
