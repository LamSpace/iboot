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

package com.iboot.admin.common.cloudevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iboot.admin.common.result.Result;

import java.io.Serializable;

/**
 * CloudEvents 响应体包装器
 * <p>
 * 将 {@link Result} 包装为 CloudEvents 规范格式
 *
 * @param <T> 数据类型
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudEventBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * CloudEvents 规范版本
     */
    @JsonProperty("specversion")
    private String specversion = "1.0";

    /**
     * 事件 ID，确保唯一性
     */
    @JsonProperty("id")
    private String id;

    /**
     * 事件类型
     */
    @JsonProperty("type")
    private String type;

    /**
     * 事件源
     */
    @JsonProperty("source")
    private String source;

    /**
     * 事件发生时间
     */
    @JsonProperty("time")
    private String time;

    /**
     * 数据内容类型
     */
    @JsonProperty("datacontenttype")
    private String datacontenttype = "application/json";

    /**
     * 实际的业务数据（原有的 Result 响应）
     */
    @JsonProperty("data")
    private Result<T> data;

    /**
     * 默认构造函数
     */
    public CloudEventBody() {
    }

    /**
     * 从 Result 构建 CloudEventBody
     *
     * @param type   事件类型
     * @param source 事件源
     * @param data   业务数据
     */
    public CloudEventBody(String type, String source, Result<T> data) {
        this.id = generateId();
        this.type = type;
        this.source = source;
        this.time = java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString();
        this.data = data;
    }

    /**
     * 创建成功响应的 CloudEventBody
     *
     * @param source 事件源
     * @param data   业务数据
     * @param <T>    数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> success(String source, T data) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_SUCCESS);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.success(data));
        return body;
    }

    /**
     * 创建成功响应的 CloudEventBody（带自定义消息）
     *
     * @param source  事件源
     * @param message 成功消息
     * @param data    业务数据
     * @param <T>     数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> success(String source, String message, T data) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_SUCCESS);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.success(message, data));
        return body;
    }

    /**
     * 创建错误响应的 CloudEventBody
     *
     * @param source  事件源
     * @param message 错误消息
     * @param <T>     数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> error(String source, String message) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.error(message));
        return body;
    }

    /**
     * 创建错误响应的 CloudEventBody（带自定义状态码）
     *
     * @param source  事件源
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> error(String source, Integer code, String message) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(Result.error(code, message));
        return body;
    }

    /**
     * 从 Result 构建 CloudEventBody
     *
     * @param result 响应结果
     * @param source 事件源
     * @param <T>    数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(determineEventType(result));
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    /**
     * 从 Result 构建 CloudEventBody（带指定事件类型）
     *
     * @param result 响应结果
     * @param type   事件类型
     * @param source 事件源
     * @param <T>    数据类型
     *
     * @return CloudEventBody 实例
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String type, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId(generateId());
        body.setType(type);
        body.setSource(source);
        body.setTime(java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    /**
     * 根据 Result 的状态码确定事件类型
     *
     * @param result 响应结果
     *
     * @return 事件类型
     */
    private static String determineEventType(Result<?> result) {
        if (result != null && result.getCode() != null && result.getCode() == 200) {
            return CloudEventTypes.RESPONSE_SUCCESS;
        }
        return CloudEventTypes.RESPONSE_ERROR;
    }

    /**
     * 生成事件 ID
     *
     * @return 事件 ID
     */
    private static String generateId() {
        return "ce-" + java.util.UUID.randomUUID();
    }

    /**
     * CloudEvents 规范版本
     */
    @SuppressWarnings("all")
    public String getSpecversion() {
        return this.specversion;
    }

    /**
     * CloudEvents 规范版本
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setSpecversion(final String specversion) {
        this.specversion = specversion;
        return this;
    }

    /**
     * 事件 ID，确保唯一性
     */
    @SuppressWarnings("all")
    public String getId() {
        return this.id;
    }

    /**
     * 事件 ID，确保唯一性
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setId(final String id) {
        this.id = id;
        return this;
    }

    /**
     * 事件类型
     */
    @SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    /**
     * 事件类型
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setType(final String type) {
        this.type = type;
        return this;
    }

    /**
     * 事件源
     */
    @SuppressWarnings("all")
    public String getSource() {
        return this.source;
    }

    /**
     * 事件源
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setSource(final String source) {
        this.source = source;
        return this;
    }

    /**
     * 事件发生时间
     */
    @SuppressWarnings("all")
    public String getTime() {
        return this.time;
    }

    /**
     * 事件发生时间
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setTime(final String time) {
        this.time = time;
        return this;
    }

    /**
     * 数据内容类型
     */
    @SuppressWarnings("all")
    public String getDatacontenttype() {
        return this.datacontenttype;
    }

    /**
     * 数据内容类型
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setDatacontenttype(final String datacontenttype) {
        this.datacontenttype = datacontenttype;
        return this;
    }

    /**
     * 实际的业务数据（原有的 Result 响应）
     */
    @SuppressWarnings("all")
    public Result<T> getData() {
        return this.data;
    }

    /**
     * 实际的业务数据（原有的 Result 响应）
     *
     * @return {@code this}.
     */
    @SuppressWarnings("all")
    public CloudEventBody<T> setData(final Result<T> data) {
        this.data = data;
        return this;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CloudEventBody))
            return false;
        final CloudEventBody<?> other = (CloudEventBody<?>) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$specversion = this.getSpecversion();
        final java.lang.Object other$specversion = other.getSpecversion();
        if (this$specversion == null ? other$specversion != null : !this$specversion.equals(other$specversion))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type))
            return false;
        final java.lang.Object this$source = this.getSource();
        final java.lang.Object other$source = other.getSource();
        if (this$source == null ? other$source != null : !this$source.equals(other$source))
            return false;
        final java.lang.Object this$time = this.getTime();
        final java.lang.Object other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time))
            return false;
        final java.lang.Object this$datacontenttype = this.getDatacontenttype();
        final java.lang.Object other$datacontenttype = other.getDatacontenttype();
        if (this$datacontenttype == null ? other$datacontenttype != null
                : !this$datacontenttype.equals(other$datacontenttype))
            return false;
        final java.lang.Object this$data = this.getData();
        final java.lang.Object other$data = other.getData();
        if (this$data == null ? other$data != null : !this$data.equals(other$data))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CloudEventBody;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $specversion = this.getSpecversion();
        result = result * PRIME + ($specversion == null ? 43 : $specversion.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $source = this.getSource();
        result = result * PRIME + ($source == null ? 43 : $source.hashCode());
        final java.lang.Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        final java.lang.Object $datacontenttype = this.getDatacontenttype();
        result = result * PRIME + ($datacontenttype == null ? 43 : $datacontenttype.hashCode());
        final java.lang.Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "CloudEventBody(specversion=" + this.getSpecversion() + ", id=" + this.getId() + ", type="
                + this.getType() + ", source=" + this.getSource() + ", time=" + this.getTime() + ", datacontenttype="
                + this.getDatacontenttype() + ", data=" + this.getData() + ")";
    }

}
