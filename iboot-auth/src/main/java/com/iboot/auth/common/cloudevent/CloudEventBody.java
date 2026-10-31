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

package com.iboot.auth.common.cloudevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iboot.kernel.web.Result;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * CloudEvents 响应体。
 *
 * <p>把统一响应 {@link Result} 包装为 CloudEvents 1.0 结构，包含 {@code specversion}、
 * {@code id}、{@code type}、{@code source}、{@code time}、{@code datacontenttype}
 * 和 {@code data}；{@code null} 字段不参与 JSON 序列化。
 *
 * <p>结构与其余服务逐字段一致，前端与回归脚本按同一规则拆包。
 *
 * @param <T> 业务数据类型
 * @author iBoot Team
 * @since 1.0.0
 * @see CloudEventTypes
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudEventBody<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * CloudEvents 规范版本，固定 {@code 1.0}。
     */
    @JsonProperty("specversion")
    private String specversion = "1.0";

    /**
     * 事件 ID，使用 UUID 生成，确保唯一。
     */
    @JsonProperty("id")
    private String id;

    /**
     * 事件类型，取值见 {@link CloudEventTypes}。
     */
    @JsonProperty("type")
    private String type;

    /**
     * 事件源，通常是请求路径。
     */
    @JsonProperty("source")
    private String source;

    /**
     * 事件发生时间，ISO-8601 字符串。
     */
    @JsonProperty("time")
    private String time;

    /**
     * 数据内容类型，固定 {@code application/json}。
     */
    @JsonProperty("datacontenttype")
    private String datacontenttype = "application/json";

    /**
     * 业务响应数据。
     */
    @JsonProperty("data")
    private Result<T> data;

    /**
     * 创建空响应体，供框架反序列化使用。
     */
    public CloudEventBody() {
    }

    /**
     * 从统一响应构建事件体，事件类型按状态码判定。
     *
     * @param result 统一响应
     * @param source 事件源，通常为请求路径
     * @param <T> 业务数据类型
     * @return 事件体；{@code result} 为 {@code null} 时仍返回携带空数据的事件体
     */
    public static <T> CloudEventBody<T> fromResult(Result<T> result, String source) {
        CloudEventBody<T> body = new CloudEventBody<>();
        body.setId("ce-" + UUID.randomUUID());
        body.setType(result != null && Integer.valueOf(200).equals(result.getCode())
                ? CloudEventTypes.RESPONSE_SUCCESS
                : CloudEventTypes.RESPONSE_ERROR);
        body.setSource(source);
        body.setTime(OffsetDateTime.now(ZoneId.systemDefault()).toString());
        body.setData(result);
        return body;
    }

    /**
     * 返回 CloudEvents 规范版本。
     *
     * @return 规范版本
     */
    public String getSpecversion() {
        return specversion;
    }

    /**
     * 设置 CloudEvents 规范版本。
     *
     * @param specversion 规范版本
     */
    public void setSpecversion(String specversion) {
        this.specversion = specversion;
    }

    /**
     * 返回事件 ID。
     *
     * @return 事件 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置事件 ID。
     *
     * @param id 事件 ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 返回事件类型。
     *
     * @return 事件类型
     */
    public String getType() {
        return type;
    }

    /**
     * 设置事件类型。
     *
     * @param type 事件类型
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 返回事件源。
     *
     * @return 事件源
     */
    public String getSource() {
        return source;
    }

    /**
     * 设置事件源。
     *
     * @param source 事件源
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 返回事件发生时间。
     *
     * @return ISO-8601 时间字符串
     */
    public String getTime() {
        return time;
    }

    /**
     * 设置事件发生时间。
     *
     * @param time ISO-8601 时间字符串
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 返回数据内容类型。
     *
     * @return 内容类型
     */
    public String getDatacontenttype() {
        return datacontenttype;
    }

    /**
     * 设置数据内容类型。
     *
     * @param datacontenttype 内容类型
     */
    public void setDatacontenttype(String datacontenttype) {
        this.datacontenttype = datacontenttype;
    }

    /**
     * 返回业务响应数据。
     *
     * @return 统一响应
     */
    public Result<T> getData() {
        return data;
    }

    /**
     * 设置业务响应数据。
     *
     * @param data 统一响应
     */
    public void setData(Result<T> data) {
        this.data = data;
    }
}
