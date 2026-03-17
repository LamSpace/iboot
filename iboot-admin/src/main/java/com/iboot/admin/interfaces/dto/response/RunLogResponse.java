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

/**
 * 运行日志响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "运行日志响应")
public class RunLogResponse {

    @Schema(description = "ES 文档ID")
    private String id;

    @Schema(description = "日志时间")
    private String timestamp;

    @Schema(description = "日志级别")
    private String level;

    @Schema(description = "线程名")
    private String thread;

    @Schema(description = "Logger 类名")
    private String logger;

    @Schema(description = "日志消息")
    private String message;

    @SuppressWarnings("all")
    public RunLogResponse() {
    }

    @SuppressWarnings("all")
    public RunLogResponse(final String id, final String timestamp, final String level, final String thread,
                          final String logger, final String message) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.thread = thread;
        this.logger = logger;
        this.message = message;
    }

    @SuppressWarnings("all")
    public static RunLogResponse.RunLogResponseBuilder builder() {
        return new RunLogResponse.RunLogResponseBuilder();
    }

    @SuppressWarnings("all")
    public String getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public void setId(final String id) {
        this.id = id;
    }

    @SuppressWarnings("all")
    public String getTimestamp() {
        return this.timestamp;
    }

    @SuppressWarnings("all")
    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    @SuppressWarnings("all")
    public String getLevel() {
        return this.level;
    }

    @SuppressWarnings("all")
    public void setLevel(final String level) {
        this.level = level;
    }

    @SuppressWarnings("all")
    public String getThread() {
        return this.thread;
    }

    @SuppressWarnings("all")
    public void setThread(final String thread) {
        this.thread = thread;
    }

    @SuppressWarnings("all")
    public String getLogger() {
        return this.logger;
    }

    @SuppressWarnings("all")
    public void setLogger(final String logger) {
        this.logger = logger;
    }

    @SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RunLogResponse))
            return false;
        final RunLogResponse other = (RunLogResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$timestamp = this.getTimestamp();
        final java.lang.Object other$timestamp = other.getTimestamp();
        if (this$timestamp == null ? other$timestamp != null : !this$timestamp.equals(other$timestamp))
            return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level))
            return false;
        final java.lang.Object this$thread = this.getThread();
        final java.lang.Object other$thread = other.getThread();
        if (this$thread == null ? other$thread != null : !this$thread.equals(other$thread))
            return false;
        final java.lang.Object this$logger = this.getLogger();
        final java.lang.Object other$logger = other.getLogger();
        if (this$logger == null ? other$logger != null : !this$logger.equals(other$logger))
            return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RunLogResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        final java.lang.Object $thread = this.getThread();
        result = result * PRIME + ($thread == null ? 43 : $thread.hashCode());
        final java.lang.Object $logger = this.getLogger();
        result = result * PRIME + ($logger == null ? 43 : $logger.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "RunLogResponse(id=" + this.getId() + ", timestamp=" + this.getTimestamp() + ", level=" + this.getLevel()
                + ", thread=" + this.getThread() + ", logger=" + this.getLogger() + ", message=" + this.getMessage()
                + ")";
    }

    @SuppressWarnings("all")
    public static class RunLogResponseBuilder {

        @SuppressWarnings("all")
        private String id;

        @SuppressWarnings("all")
        private String timestamp;

        @SuppressWarnings("all")
        private String level;

        @SuppressWarnings("all")
        private String thread;

        @SuppressWarnings("all")
        private String logger;

        @SuppressWarnings("all")
        private String message;

        @SuppressWarnings("all")
        RunLogResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder id(final String id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder timestamp(final String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder level(final String level) {
            this.level = level;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder thread(final String thread) {
            this.thread = thread;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder logger(final String logger) {
            this.logger = logger;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public RunLogResponse.RunLogResponseBuilder message(final String message) {
            this.message = message;
            return this;
        }

        @SuppressWarnings("all")
        public RunLogResponse build() {
            return new RunLogResponse(this.id, this.timestamp, this.level, this.thread, this.logger, this.message);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "RunLogResponse.RunLogResponseBuilder(id=" + this.id + ", timestamp=" + this.timestamp + ", level="
                    + this.level + ", thread=" + this.thread + ", logger=" + this.logger + ", message=" + this.message
                    + ")";
        }

    }

}
