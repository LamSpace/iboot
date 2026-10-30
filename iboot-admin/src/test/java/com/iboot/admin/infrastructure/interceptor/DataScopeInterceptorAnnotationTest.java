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

package com.iboot.admin.infrastructure.interceptor;

import com.iboot.admin.common.annotation.DataScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 数据权限注解解析的单元测试。
 *
 * <p>覆盖三种情况：注解标在 Mapper 方法上、标在 Mapper 接口上、以及两处都没有。
 * 方法级优先是本次修复的核心语义——历史上只解析接口级注解，导致全部方法级
 * {@code @DataScope} 被忽略、数据权限从未生效。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DataScopeInterceptorAnnotationTest {

    /**
     * 注解标在方法上的 Mapper。
     */
    interface MethodAnnotatedMapper {

        /**
         * 带数据权限的查询。
         *
         * @return 结果数量
         */
        @DataScope(deptAlias = "m", userAlias = "m")
        int selectWithScope();
    }

    /**
     * 注解标在接口上的 Mapper。
     */
    @DataScope(deptAlias = "c", userAlias = "c")
    interface ClassAnnotatedMapper {

        /**
         * 未单独标注的查询，应继承接口级注解。
         *
         * @return 结果数量
         */
        int selectInherited();
    }

    /**
     * 完全未标注的 Mapper。
     */
    interface PlainMapper {

        /**
         * 普通查询。
         *
         * @return 结果数量
         */
        int selectPlain();
    }

    private final DataScopeInterceptor interceptor = new DataScopeInterceptor(null, null);

    /**
     * 方法级注解应被解析。
     */
    @Test
    void resolvesMethodLevelAnnotation() {
        DataScope scope = interceptor.findAnnotation(MethodAnnotatedMapper.class, "selectWithScope", DataScope.class);

        assertNotNull(scope);
        assertEquals("m", scope.userAlias());
    }

    /**
     * 接口级注解应作为方法级缺失时的兜底。
     */
    @Test
    void fallsBackToClassLevelAnnotation() {
        DataScope scope = interceptor.findAnnotation(ClassAnnotatedMapper.class, "selectInherited", DataScope.class);

        assertNotNull(scope);
        assertEquals("c", scope.userAlias());
    }

    /**
     * 两处都没有时返回 null，调用方据此直接放行。
     */
    @Test
    void returnsNullWhenAbsent() {
        assertNull(interceptor.findAnnotation(PlainMapper.class, "selectPlain", DataScope.class));
    }

    /**
     * 方法名在接口上不存在时返回 null，而不是抛出异常。
     */
    @Test
    void returnsNullForUnknownMethod() {
        assertNull(interceptor.findAnnotation(PlainMapper.class, "missing", DataScope.class));
    }
}
