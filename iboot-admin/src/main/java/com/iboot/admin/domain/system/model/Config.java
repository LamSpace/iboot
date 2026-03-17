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
 * 系统配置实体
 *
 * @author iBoot
 */
public class Config {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型：0-默认，1-系统内置
     */
    private Integer configType;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;

    @SuppressWarnings("all")
    public Config() {
    }

    /**
     * Creates a new {@code Config} instance.
     *
     * @param id          配置ID
     * @param configName  配置名称
     * @param configKey   配置键
     * @param configValue 配置值
     * @param configType  配置类型：0-默认，1-系统内置
     * @param createBy    创建人
     * @param createTime  创建时间
     * @param updateBy    更新人
     * @param updateTime  更新时间
     * @param deleted     逻辑删除：0-未删除，1-已删除
     * @param remark      备注
     */
    @SuppressWarnings("all")
    public Config(final Long id, final String configName, final String configKey, final String configValue,
                  final Integer configType, final String createBy, final LocalDateTime createTime, final String updateBy,
                  final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.configName = configName;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static Config.ConfigBuilder builder() {
        return new Config.ConfigBuilder();
    }

    /**
     * 更新配置值
     */
    public void updateValue(String newValue) {
        this.configValue = newValue;
    }

    /**
     * 检查是否为系统内置配置
     */
    public boolean isSystemConfig() {
        return this.configType != null && this.configType == 1;
    }

    /**
     * 配置ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 配置ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 配置名称
     */
    @SuppressWarnings("all")
    public String getConfigName() {
        return this.configName;
    }

    /**
     * 配置名称
     */
    @SuppressWarnings("all")
    public void setConfigName(final String configName) {
        this.configName = configName;
    }

    /**
     * 配置键
     */
    @SuppressWarnings("all")
    public String getConfigKey() {
        return this.configKey;
    }

    /**
     * 配置键
     */
    @SuppressWarnings("all")
    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    /**
     * 配置值
     */
    @SuppressWarnings("all")
    public String getConfigValue() {
        return this.configValue;
    }

    /**
     * 配置值
     */
    @SuppressWarnings("all")
    public void setConfigValue(final String configValue) {
        this.configValue = configValue;
    }

    /**
     * 配置类型：0-默认，1-系统内置
     */
    @SuppressWarnings("all")
    public Integer getConfigType() {
        return this.configType;
    }

    /**
     * 配置类型：0-默认，1-系统内置
     */
    @SuppressWarnings("all")
    public void setConfigType(final Integer configType) {
        this.configType = configType;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
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

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Config))
            return false;
        final Config other = (Config) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$configType = this.getConfigType();
        final java.lang.Object other$configType = other.getConfigType();
        if (this$configType == null ? other$configType != null : !this$configType.equals(other$configType))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$configName = this.getConfigName();
        final java.lang.Object other$configName = other.getConfigName();
        if (this$configName == null ? other$configName != null : !this$configName.equals(other$configName))
            return false;
        final java.lang.Object this$configKey = this.getConfigKey();
        final java.lang.Object other$configKey = other.getConfigKey();
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey))
            return false;
        final java.lang.Object this$configValue = this.getConfigValue();
        final java.lang.Object other$configValue = other.getConfigValue();
        if (this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy))
            return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Config;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $configType = this.getConfigType();
        result = result * PRIME + ($configType == null ? 43 : $configType.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $configName = this.getConfigName();
        result = result * PRIME + ($configName == null ? 43 : $configName.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $configValue = this.getConfigValue();
        result = result * PRIME + ($configValue == null ? 43 : $configValue.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "Config(id=" + this.getId() + ", configName=" + this.getConfigName() + ", configKey="
                + this.getConfigKey() + ", configValue=" + this.getConfigValue() + ", configType="
                + this.getConfigType() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime()
                + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime() + ", deleted="
                + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class ConfigBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String configName;

        @SuppressWarnings("all")
        private String configKey;

        @SuppressWarnings("all")
        private String configValue;

        @SuppressWarnings("all")
        private Integer configType;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        ConfigBuilder() {
        }

        /**
         * 配置ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 配置名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder configName(final String configName) {
            this.configName = configName;
            return this;
        }

        /**
         * 配置键
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder configKey(final String configKey) {
            this.configKey = configKey;
            return this;
        }

        /**
         * 配置值
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder configValue(final String configValue) {
            this.configValue = configValue;
            return this;
        }

        /**
         * 配置类型：0-默认，1-系统内置
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder configType(final Integer configType) {
            this.configType = configType;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Config.ConfigBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public Config build() {
            return new Config(this.id, this.configName, this.configKey, this.configValue, this.configType,
                    this.createBy, this.createTime, this.updateBy, this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Config.ConfigBuilder(id=" + this.id + ", configName=" + this.configName + ", configKey="
                    + this.configKey + ", configValue=" + this.configValue + ", configType=" + this.configType
                    + ", createBy=" + this.createBy + ", createTime=" + this.createTime + ", updateBy=" + this.updateBy
                    + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
