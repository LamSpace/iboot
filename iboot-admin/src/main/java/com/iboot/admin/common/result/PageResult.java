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

package com.iboot.admin.common.result;

import java.util.List;

/**
 * 分页响应结果
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class PageResult<T> extends Result<List<T>> {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;

    public PageResult() {
        super();
    }

    public PageResult(List<T> rows, Long total, Integer pageNum, Integer pageSize) {
        super(200, "查询成功", rows);
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> rows, Long total, Integer pageNum, Integer pageSize) {
        return new PageResult<>(rows, total, pageNum, pageSize);
    }

    /**
     * 总记录数
     */
    @SuppressWarnings("all")
    public Long getTotal() {
        return this.total;
    }

    /**
     * 总记录数
     */
    @SuppressWarnings("all")
    public void setTotal(final Long total) {
        this.total = total;
    }

    /**
     * 当前页码
     */
    @SuppressWarnings("all")
    public Integer getPageNum() {
        return this.pageNum;
    }

    /**
     * 当前页码
     */
    @SuppressWarnings("all")
    public void setPageNum(final Integer pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 每页数量
     */
    @SuppressWarnings("all")
    public Integer getPageSize() {
        return this.pageSize;
    }

    /**
     * 每页数量
     */
    @SuppressWarnings("all")
    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "PageResult(total=" + this.getTotal() + ", pageNum=" + this.getPageNum() + ", pageSize="
                + this.getPageSize() + ")";
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PageResult))
            return false;
        final PageResult<?> other = (PageResult<?>) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        if (!super.equals(o))
            return false;
        final java.lang.Object this$total = this.getTotal();
        final java.lang.Object other$total = other.getTotal();
        if (this$total == null ? other$total != null : !this$total.equals(other$total))
            return false;
        final java.lang.Object this$pageNum = this.getPageNum();
        final java.lang.Object other$pageNum = other.getPageNum();
        if (this$pageNum == null ? other$pageNum != null : !this$pageNum.equals(other$pageNum))
            return false;
        final java.lang.Object this$pageSize = this.getPageSize();
        final java.lang.Object other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !this$pageSize.equals(other$pageSize))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PageResult;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final java.lang.Object $total = this.getTotal();
        result = result * PRIME + ($total == null ? 43 : $total.hashCode());
        final java.lang.Object $pageNum = this.getPageNum();
        result = result * PRIME + ($pageNum == null ? 43 : $pageNum.hashCode());
        final java.lang.Object $pageSize = this.getPageSize();
        result = result * PRIME + ($pageSize == null ? 43 : $pageSize.hashCode());
        return result;
    }

}
