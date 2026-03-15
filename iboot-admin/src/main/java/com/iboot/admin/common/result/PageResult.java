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

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 分页响应结果
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
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
}
