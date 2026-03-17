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

package com.iboot.admin.infrastructure.push;

import java.io.Serializable;

/**
 * 推送事件定义
 * <p>
 * 与 CloudEvents 规范兼容，用于 SSE 推送
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class PushEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件 ID，确保唯一性
     */
    private String id;

    /**
     * 事件类型（与 CloudEventTypes 对应）
     */
    private String type;

    /**
     * 事件源
     */
    private String source;

    /**
     * 事件发生时间
     */
    private String time;

    /**
     * 数据内容类型
     */
    private String dataContentType;

    /**
     * 实际的业务数据
     */
    private Object data;

    /**
     * 目标用户 ID（用于单播推送）
     */
    private Long targetUserId;

    /**
     * Creates a new {@code PushEvent} instance.
     *
     * @param id              事件 ID，确保唯一性
     * @param type            事件类型（与 CloudEventTypes 对应）
     * @param source          事件源
     * @param time            事件发生时间
     * @param dataContentType 数据内容类型
     * @param data            实际的业务数据
     * @param targetUserId    目标用户 ID（用于单播推送）
     */
    @SuppressWarnings("all")
    public PushEvent(final String id, final String type, final String source, final String time,
                     final String dataContentType, final Object data, final Long targetUserId) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.time = time;
        this.dataContentType = dataContentType;
        this.data = data;
        this.targetUserId = targetUserId;
    }

    @SuppressWarnings("all")
    public PushEvent() {
        this.dataContentType = PushEvent.$default$dataContentType();
    }

    /**
     * 生成事件 ID
     *
     * @return 事件 ID
     */
    private static String generateId() {
        return "push-" + java.util.UUID.randomUUID();
    }

    @SuppressWarnings("all")
    private static String $default$dataContentType() {
        return "application/json";
    }

    @SuppressWarnings("all")
    public static PushEvent.PushEventBuilder builder() {
        return new PushEvent.PushEventBuilder();
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
     */
    @SuppressWarnings("all")
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * 事件类型（与 CloudEventTypes 对应）
     */
    @SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    /**
     * 事件类型（与 CloudEventTypes 对应）
     */
    @SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
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
     */
    @SuppressWarnings("all")
    public void setSource(final String source) {
        this.source = source;
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
     */
    @SuppressWarnings("all")
    public void setTime(final String time) {
        this.time = time;
    }

    /**
     * 数据内容类型
     */
    @SuppressWarnings("all")
    public String getDataContentType() {
        return this.dataContentType;
    }

    /**
     * 数据内容类型
     */
    @SuppressWarnings("all")
    public void setDataContentType(final String dataContentType) {
        this.dataContentType = dataContentType;
    }

    /**
     * 实际的业务数据
     */
    @SuppressWarnings("all")
    public Object getData() {
        return this.data;
    }

    /**
     * 实际的业务数据
     */
    @SuppressWarnings("all")
    public void setData(final Object data) {
        this.data = data;
    }

    /**
     * 目标用户 ID（用于单播推送）
     */
    @SuppressWarnings("all")
    public Long getTargetUserId() {
        return this.targetUserId;
    }

    /**
     * 目标用户 ID（用于单播推送）
     */
    @SuppressWarnings("all")
    public void setTargetUserId(final Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PushEvent))
            return false;
        final PushEvent other = (PushEvent) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$targetUserId = this.getTargetUserId();
        final java.lang.Object other$targetUserId = other.getTargetUserId();
        if (this$targetUserId == null ? other$targetUserId != null : !this$targetUserId.equals(other$targetUserId))
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
        final java.lang.Object this$dataContentType = this.getDataContentType();
        final java.lang.Object other$dataContentType = other.getDataContentType();
        if (this$dataContentType == null ? other$dataContentType != null
                : !this$dataContentType.equals(other$dataContentType))
            return false;
        final java.lang.Object this$data = this.getData();
        final java.lang.Object other$data = other.getData();
        if (this$data == null ? other$data != null : !this$data.equals(other$data))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PushEvent;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $targetUserId = this.getTargetUserId();
        result = result * PRIME + ($targetUserId == null ? 43 : $targetUserId.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $source = this.getSource();
        result = result * PRIME + ($source == null ? 43 : $source.hashCode());
        final java.lang.Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        final java.lang.Object $dataContentType = this.getDataContentType();
        result = result * PRIME + ($dataContentType == null ? 43 : $dataContentType.hashCode());
        final java.lang.Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "PushEvent(id=" + this.getId() + ", type=" + this.getType() + ", source=" + this.getSource() + ", time="
                + this.getTime() + ", dataContentType=" + this.getDataContentType() + ", data=" + this.getData()
                + ", targetUserId=" + this.getTargetUserId() + ")";
    }

    @SuppressWarnings("all")
    public static class PushEventBuilder {

        @SuppressWarnings("all")
        private String id;

        @SuppressWarnings("all")
        private String type;

        @SuppressWarnings("all")
        private String source;

        @SuppressWarnings("all")
        private String time;

        @SuppressWarnings("all")
        private boolean dataContentType$set;

        @SuppressWarnings("all")
        private String dataContentType$value;

        @SuppressWarnings("all")
        private Object data;

        @SuppressWarnings("all")
        private Long targetUserId;

        @SuppressWarnings("all")
        PushEventBuilder() {
        }

        /**
         * 事件 ID，确保唯一性
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder id(final String id) {
            this.id = id;
            return this;
        }

        /**
         * 事件类型（与 CloudEventTypes 对应）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * 事件源
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder source(final String source) {
            this.source = source;
            return this;
        }

        /**
         * 事件发生时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder time(final String time) {
            this.time = time;
            return this;
        }

        /**
         * 数据内容类型
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder dataContentType(final String dataContentType) {
            this.dataContentType$value = dataContentType;
            dataContentType$set = true;
            return this;
        }

        /**
         * 实际的业务数据
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder data(final Object data) {
            this.data = data;
            return this;
        }

        /**
         * 目标用户 ID（用于单播推送）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PushEvent.PushEventBuilder targetUserId(final Long targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        @SuppressWarnings("all")
        public PushEvent build() {
            String dataContentType$value = this.dataContentType$value;
            if (!this.dataContentType$set)
                dataContentType$value = PushEvent.$default$dataContentType();
            return new PushEvent(this.id, this.type, this.source, this.time, dataContentType$value, this.data,
                    this.targetUserId);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PushEvent.PushEventBuilder(id=" + this.id + ", type=" + this.type + ", source=" + this.source
                    + ", time=" + this.time + ", dataContentType$value=" + this.dataContentType$value + ", data="
                    + this.data + ", targetUserId=" + this.targetUserId + ")";
        }

    }

}
