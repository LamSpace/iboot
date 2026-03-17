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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;

import java.time.LocalDateTime;

/**
 * 系统配置导出VO
 *
 * @author iBoot
 */
public class ConfigExportVO {

    @ExcelColumn(name = "配置ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "配置名称", order = 2, width = 25)
    private String configName;

    @ExcelColumn(name = "配置键", order = 3, width = 30)
    private String configKey;

    @ExcelColumn(name = "配置值", order = 4, width = 40)
    private String configValue;

    @ExcelColumn(name = "配置类型", order = 5, width = 10, dictType = "sys_config_type")
    private Integer configType;

    @ExcelColumn(name = "备注", order = 6, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 7, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public ConfigExportVO() {
    }

    @SuppressWarnings("all")
    public ConfigExportVO(final Long id, final String configName, final String configKey, final String configValue,
                          final Integer configType, final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.configName = configName;
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static ConfigExportVO.ConfigExportVOBuilder builder() {
        return new ConfigExportVO.ConfigExportVOBuilder();
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
    public String getConfigName() {
        return this.configName;
    }

    @SuppressWarnings("all")
    public void setConfigName(final String configName) {
        this.configName = configName;
    }

    @SuppressWarnings("all")
    public String getConfigKey() {
        return this.configKey;
    }

    @SuppressWarnings("all")
    public void setConfigKey(final String configKey) {
        this.configKey = configKey;
    }

    @SuppressWarnings("all")
    public String getConfigValue() {
        return this.configValue;
    }

    @SuppressWarnings("all")
    public void setConfigValue(final String configValue) {
        this.configValue = configValue;
    }

    @SuppressWarnings("all")
    public Integer getConfigType() {
        return this.configType;
    }

    @SuppressWarnings("all")
    public void setConfigType(final Integer configType) {
        this.configType = configType;
    }

    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ConfigExportVO))
            return false;
        final ConfigExportVO other = (ConfigExportVO) o;
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
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ConfigExportVO;
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
        final java.lang.Object $configName = this.getConfigName();
        result = result * PRIME + ($configName == null ? 43 : $configName.hashCode());
        final java.lang.Object $configKey = this.getConfigKey();
        result = result * PRIME + ($configKey == null ? 43 : $configKey.hashCode());
        final java.lang.Object $configValue = this.getConfigValue();
        result = result * PRIME + ($configValue == null ? 43 : $configValue.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "ConfigExportVO(id=" + this.getId() + ", configName=" + this.getConfigName() + ", configKey="
                + this.getConfigKey() + ", configValue=" + this.getConfigValue() + ", configType="
                + this.getConfigType() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class ConfigExportVOBuilder {

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
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        ConfigExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder configName(final String configName) {
            this.configName = configName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder configKey(final String configKey) {
            this.configKey = configKey;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder configValue(final String configValue) {
            this.configValue = configValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder configType(final Integer configType) {
            this.configType = configType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public ConfigExportVO.ConfigExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public ConfigExportVO build() {
            return new ConfigExportVO(this.id, this.configName, this.configKey, this.configValue, this.configType,
                    this.remark, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "ConfigExportVO.ConfigExportVOBuilder(id=" + this.id + ", configName=" + this.configName
                    + ", configKey=" + this.configKey + ", configValue=" + this.configValue + ", configType="
                    + this.configType + ", remark=" + this.remark + ", createTime=" + this.createTime + ")";
        }

    }

}
