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
 * 公开系统配置响应 DTO 用于前端登录页、页头等场景获取系统公开配置
 *
 * @author iBoot
 */
@Schema(description = "公开系统配置响应")
public class PublicConfigResponse {

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "是否允许用户注册")
    private Boolean registerEnabled;

    @SuppressWarnings("all")
    public PublicConfigResponse() {
    }

    @SuppressWarnings("all")
    public PublicConfigResponse(final String systemName, final Boolean registerEnabled) {
        this.systemName = systemName;
        this.registerEnabled = registerEnabled;
    }

    @SuppressWarnings("all")
    public static PublicConfigResponse.PublicConfigResponseBuilder builder() {
        return new PublicConfigResponse.PublicConfigResponseBuilder();
    }

    @SuppressWarnings("all")
    public String getSystemName() {
        return this.systemName;
    }

    @SuppressWarnings("all")
    public void setSystemName(final String systemName) {
        this.systemName = systemName;
    }

    @SuppressWarnings("all")
    public Boolean getRegisterEnabled() {
        return this.registerEnabled;
    }

    @SuppressWarnings("all")
    public void setRegisterEnabled(final Boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PublicConfigResponse))
            return false;
        final PublicConfigResponse other = (PublicConfigResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$registerEnabled = this.getRegisterEnabled();
        final java.lang.Object other$registerEnabled = other.getRegisterEnabled();
        if (this$registerEnabled == null ? other$registerEnabled != null
                : !this$registerEnabled.equals(other$registerEnabled))
            return false;
        final java.lang.Object this$systemName = this.getSystemName();
        final java.lang.Object other$systemName = other.getSystemName();
        if (this$systemName == null ? other$systemName != null : !this$systemName.equals(other$systemName))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PublicConfigResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $registerEnabled = this.getRegisterEnabled();
        result = result * PRIME + ($registerEnabled == null ? 43 : $registerEnabled.hashCode());
        final java.lang.Object $systemName = this.getSystemName();
        result = result * PRIME + ($systemName == null ? 43 : $systemName.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "PublicConfigResponse(systemName=" + this.getSystemName() + ", registerEnabled="
                + this.getRegisterEnabled() + ")";
    }

    @SuppressWarnings("all")
    public static class PublicConfigResponseBuilder {

        @SuppressWarnings("all")
        private String systemName;

        @SuppressWarnings("all")
        private Boolean registerEnabled;

        @SuppressWarnings("all")
        PublicConfigResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PublicConfigResponse.PublicConfigResponseBuilder systemName(final String systemName) {
            this.systemName = systemName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public PublicConfigResponse.PublicConfigResponseBuilder registerEnabled(final Boolean registerEnabled) {
            this.registerEnabled = registerEnabled;
            return this;
        }

        @SuppressWarnings("all")
        public PublicConfigResponse build() {
            return new PublicConfigResponse(this.systemName, this.registerEnabled);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "PublicConfigResponse.PublicConfigResponseBuilder(systemName=" + this.systemName
                    + ", registerEnabled=" + this.registerEnabled + ")";
        }

    }

}
