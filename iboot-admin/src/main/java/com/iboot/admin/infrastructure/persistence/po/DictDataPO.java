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
 * 字典数据持久化对象
 * <p>
 * 对应数据库表：sys_dict_data
 * </p>
 *
 * @author iBoot
 */
public class DictDataPO {

    /**
     * 字典数据 ID
     */
    private Long id;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 字典键值
     */
    private String dictValue;

    /**
     * 显示顺序
     */
    private Integer dictSort;

    /**
     * 样式属性
     */
    private String cssClass;

    /**
     * 表格回显样式
     */
    private String listClass;

    /**
     * 是否默认：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;

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
    public DictDataPO() {
    }

    /**
     * 字典数据 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 字典数据 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 字典类型
     */
    @SuppressWarnings("all")
    public String getDictType() {
        return this.dictType;
    }

    /**
     * 字典类型
     */
    @SuppressWarnings("all")
    public void setDictType(final String dictType) {
        this.dictType = dictType;
    }

    /**
     * 字典标签
     */
    @SuppressWarnings("all")
    public String getDictLabel() {
        return this.dictLabel;
    }

    /**
     * 字典标签
     */
    @SuppressWarnings("all")
    public void setDictLabel(final String dictLabel) {
        this.dictLabel = dictLabel;
    }

    /**
     * 字典键值
     */
    @SuppressWarnings("all")
    public String getDictValue() {
        return this.dictValue;
    }

    /**
     * 字典键值
     */
    @SuppressWarnings("all")
    public void setDictValue(final String dictValue) {
        this.dictValue = dictValue;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public Integer getDictSort() {
        return this.dictSort;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public void setDictSort(final Integer dictSort) {
        this.dictSort = dictSort;
    }

    /**
     * 样式属性
     */
    @SuppressWarnings("all")
    public String getCssClass() {
        return this.cssClass;
    }

    /**
     * 样式属性
     */
    @SuppressWarnings("all")
    public void setCssClass(final String cssClass) {
        this.cssClass = cssClass;
    }

    /**
     * 表格回显样式
     */
    @SuppressWarnings("all")
    public String getListClass() {
        return this.listClass;
    }

    /**
     * 表格回显样式
     */
    @SuppressWarnings("all")
    public void setListClass(final String listClass) {
        this.listClass = listClass;
    }

    /**
     * 是否默认：0-否，1-是
     */
    @SuppressWarnings("all")
    public Integer getIsDefault() {
        return this.isDefault;
    }

    /**
     * 是否默认：0-否，1-是
     */
    @SuppressWarnings("all")
    public void setIsDefault(final Integer isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
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
        if (!(o instanceof DictDataPO))
            return false;
        final DictDataPO other = (DictDataPO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$dictSort = this.getDictSort();
        final java.lang.Object other$dictSort = other.getDictSort();
        if (this$dictSort == null ? other$dictSort != null : !this$dictSort.equals(other$dictSort))
            return false;
        final java.lang.Object this$isDefault = this.getIsDefault();
        final java.lang.Object other$isDefault = other.getIsDefault();
        if (this$isDefault == null ? other$isDefault != null : !this$isDefault.equals(other$isDefault))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$dictType = this.getDictType();
        final java.lang.Object other$dictType = other.getDictType();
        if (this$dictType == null ? other$dictType != null : !this$dictType.equals(other$dictType))
            return false;
        final java.lang.Object this$dictLabel = this.getDictLabel();
        final java.lang.Object other$dictLabel = other.getDictLabel();
        if (this$dictLabel == null ? other$dictLabel != null : !this$dictLabel.equals(other$dictLabel))
            return false;
        final java.lang.Object this$dictValue = this.getDictValue();
        final java.lang.Object other$dictValue = other.getDictValue();
        if (this$dictValue == null ? other$dictValue != null : !this$dictValue.equals(other$dictValue))
            return false;
        final java.lang.Object this$cssClass = this.getCssClass();
        final java.lang.Object other$cssClass = other.getCssClass();
        if (this$cssClass == null ? other$cssClass != null : !this$cssClass.equals(other$cssClass))
            return false;
        final java.lang.Object this$listClass = this.getListClass();
        final java.lang.Object other$listClass = other.getListClass();
        if (this$listClass == null ? other$listClass != null : !this$listClass.equals(other$listClass))
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
        return other instanceof DictDataPO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $dictSort = this.getDictSort();
        result = result * PRIME + ($dictSort == null ? 43 : $dictSort.hashCode());
        final java.lang.Object $isDefault = this.getIsDefault();
        result = result * PRIME + ($isDefault == null ? 43 : $isDefault.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $dictType = this.getDictType();
        result = result * PRIME + ($dictType == null ? 43 : $dictType.hashCode());
        final java.lang.Object $dictLabel = this.getDictLabel();
        result = result * PRIME + ($dictLabel == null ? 43 : $dictLabel.hashCode());
        final java.lang.Object $dictValue = this.getDictValue();
        result = result * PRIME + ($dictValue == null ? 43 : $dictValue.hashCode());
        final java.lang.Object $cssClass = this.getCssClass();
        result = result * PRIME + ($cssClass == null ? 43 : $cssClass.hashCode());
        final java.lang.Object $listClass = this.getListClass();
        result = result * PRIME + ($listClass == null ? 43 : $listClass.hashCode());
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
        return "DictDataPO(id=" + this.getId() + ", dictType=" + this.getDictType() + ", dictLabel="
                + this.getDictLabel() + ", dictValue=" + this.getDictValue() + ", dictSort=" + this.getDictSort()
                + ", cssClass=" + this.getCssClass() + ", listClass=" + this.getListClass() + ", isDefault="
                + this.getIsDefault() + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

}
