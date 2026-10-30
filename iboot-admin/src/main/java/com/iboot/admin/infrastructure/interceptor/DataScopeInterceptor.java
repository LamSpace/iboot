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

import com.iboot.admin.application.service.UserDataScopeResolver;
import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.domain.system.model.UserDataScope;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 数据权限拦截器。
 *
 * <p>拦截 MyBatis 查询，根据当前用户所有角色的数据范围向 SQL 追加部门或用户过滤条件；
 * 角色范围按数值优先级合并，计算结果按用户缓存。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.admin.common.annotation.DataScope
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})})
public class DataScopeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    private final UserDataScopeResolver userDataScopeResolver;

    private final DataScopeSqlBuilder dataScopeSqlBuilder;

    /**
     * 创建数据权限拦截器。
     *
     * @param userDataScopeResolver 用户数据权限解析器
     * @param dataScopeSqlBuilder   SQL 片段构造器
     */
    public DataScopeInterceptor(UserDataScopeResolver userDataScopeResolver,
                                DataScopeSqlBuilder dataScopeSqlBuilder) {
        this.userDataScopeResolver = userDataScopeResolver;
        this.dataScopeSqlBuilder = dataScopeSqlBuilder;
    }

    /**
     * 从 MappedStatement 解析数据权限注解。
     *
     * <p>MyBatis 的 MappedStatement 不直接暴露注解，需要由 statement id
     * （形如 {@code 接口全限定名.方法名}）反查 Mapper 接口。注解既可能标在方法上，
     * 也可能标在接口上，因此先查方法、再回退到接口。
     *
     * @param ms              MappedStatement
     * @param annotationClass 目标注解类型
     * @param <T>             注解类型
     * @return 找到的注解；方法上与接口上都没有时返回 {@code null}
     */
    private <T extends Annotation> T getAnnotationFromMappedStatement(MappedStatement ms, Class<T> annotationClass) {
        String statementId = ms.getId();
        int lastDotIndex = statementId.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            return null;
        }
        try {
            Class<?> mapperClass = Class.forName(statementId.substring(0, lastDotIndex));
            return findAnnotation(mapperClass, statementId.substring(lastDotIndex + 1), annotationClass);
        } catch (ClassNotFoundException e) {
            log.debug("无法获取 Mapper 类：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 在 Mapper 类上解析注解，方法级优先、接口级其次。
     *
     * @param mapperClass     Mapper 接口
     * @param methodName      Mapper 方法名
     * @param annotationClass 目标注解类型
     * @param <T>             注解类型
     * @return 找到的注解；方法上与接口上都没有时返回 {@code null}
     */
    <T extends Annotation> T findAnnotation(Class<?> mapperClass, String methodName, Class<T> annotationClass) {
        for (Method method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(annotationClass)) {
                return method.getAnnotation(annotationClass);
            }
        }
        return mapperClass.getAnnotation(annotationClass);
    }

    /**
     * 从 MappedStatement 中提取表名。
     *
     * @param ms        MappedStatement
     * @param parameter 查询参数对象，用于生成 BoundSql
     * @return 表名（小写）；无法识别时返回空串
     */
    private String extractTableName(MappedStatement ms, Object parameter) {
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql().trim().toUpperCase();
        // 从 SELECT 或 UPDATE 语句中提取表名
        int fromIndex = sql.indexOf("FROM ");
        if (fromIndex >= 0) {
            int startIndex = fromIndex + 5;
            int endIndex = sql.indexOf(" ", startIndex);
            if (endIndex == -1)
                endIndex = sql.length();
            String tableName = sql.substring(startIndex, endIndex).trim();
            // 移除可能的别名（SELECT * FROM sys_user u -> sys_user）
            int aliasIndex = tableName.indexOf(" ");
            if (aliasIndex > 0) {
                tableName = tableName.substring(0, aliasIndex);
            }
            return tableName.toLowerCase();
        }
        // 处理 UPDATE 语句
        int updateIndex = sql.indexOf("UPDATE ");
        if (updateIndex >= 0) {
            int startIndex = updateIndex + 7;
            int endIndex = sql.indexOf(" ", startIndex);
            if (endIndex == -1)
                endIndex = sql.length();
            return sql.substring(startIndex, endIndex).trim().toLowerCase();
        }
        return "";
    }

    @Override
    /**
     * 拦截 MyBatis 查询并按当前用户的数据权限改写 SQL。
     *
     * <p>只处理带 {@code @DataScope} 标记的查询；其他调用直接放行。
     *
     * @param invocation MyBatis 调用
     * @return 查询结果
     * @throws Throwable 执行失败时抛出
     */
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        // 检查方法是否有@DataScope 注解
        DataScope dataScope = getAnnotationFromMappedStatement(ms, DataScope.class);
        if (dataScope == null) {
            return invocation.proceed();
        }
        Long currentUserId;
        String currentUsername;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
            currentUsername = SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            // 用户未登录，不应用数据权限（可能是系统查询或定时任务）
            log.debug("无法获取当前用户，跳过数据权限处理：{}", e.getMessage());
            return invocation.proceed();
        }
        UserDataScope userDataScope = userDataScopeResolver.resolve(currentUserId, currentUsername);
        if (userDataScope == null || userDataScope.superAdmin() || userDataScope.dataScope() == null
                || userDataScope.isAll()) {
            // 超级管理员、无角色或全部数据权限，不需要过滤
            return invocation.proceed();
        }
        // 从 SQL 中提取表名
        String tableName = extractTableName(ms, args[1]);
        boolean deptTable = "sys_dept".equals(tableName);
        // 生成数据权限 SQL 片段
        String dataScopeSql = dataScopeSqlBuilder.build(userDataScope, dataScope.userAlias(), deptTable,
                currentUserId);
        if (dataScopeSql == null || dataScopeSql.isEmpty()) {
            return invocation.proceed();
        }
        // 重写 SQL，添加数据权限过滤条件
        return proceedWithRewrittenSql(invocation, ms, args, dataScopeSql);
    }

    /**
     * 重写 SQL 并执行
     */
    private Object proceedWithRewrittenSql(Invocation invocation, MappedStatement ms, Object[] args,
                                           String dataScopeSql) throws Exception {
        BoundSql boundSql = ms.getBoundSql(args[1]);
        String originalSql = boundSql.getSql().trim();
        // 在 WHERE 条件后追加数据权限条件
        String rewrittenSql = appendDataScopeToSql(originalSql, dataScopeSql);
        // 创建新的 MappedStatement
        MappedStatement newMs = createNewMappedStatement(ms, rewrittenSql);
        // 替换第一个参数（MappedStatement）
        args[0] = newMs;
        if (args.length == 6) {
            // 6 参数分支由调用方传入 BoundSql 并直接使用，仅替换 MappedStatement 不会生效，
            // 必须同步替换最后一个参数，否则执行的仍是原始 SQL。
            args[5] = new BoundSql(ms.getConfiguration(), rewrittenSql,
                    boundSql.getParameterMappings(), boundSql.getParameterObject());
        }
        // 获取 Executor 的 query 方法
        Method method;
        if (args.length == 6) {
            // 6 参数 query 方法：query(MappedStatement, Object, RowBounds, ResultHandler,
            // CacheKey, BoundSql)
            method = Executor.class.getMethod("query", MappedStatement.class, Object.class, RowBounds.class,
                    ResultHandler.class, CacheKey.class, BoundSql.class);
        } else {
            // 4 参数 query 方法：query(MappedStatement, Object, RowBounds, ResultHandler)
            method = Executor.class.getMethod("query", MappedStatement.class, Object.class, RowBounds.class,
                    ResultHandler.class);
        }
        // 创建新的 Invocation
        Invocation newInvocation = new Invocation(invocation.getTarget(), method, args);
        return newInvocation.proceed();
    }

    /**
     * 将数据权限 SQL 追加到原始 SQL 中
     */
    private String appendDataScopeToSql(String originalSql, String dataScopeSql) {
        // 转换为小写便于处理（但保留原始大小写用于替换）
        String lowerSql = originalSql.toLowerCase();
        // 查找 WHERE 关键字位置
        int whereIndex = lowerSql.indexOf("where");
        if (whereIndex > 0) {
            // 已有 WHERE 子句：在原条件两侧加括号，避免 AND 与原条件的 OR 混在一起
            int tailIndex = findTailIndex(lowerSql, whereIndex + 5);
            return originalSql.substring(0, whereIndex + 5) + " " + dataScopeSql + " AND ("
                    + originalSql.substring(whereIndex + 5, tailIndex) + ") "
                    + originalSql.substring(tailIndex);
        }
        // 没有 WHERE 子句：插入在 ORDER BY / GROUP BY / LIMIT 之前
        int insertPosition = findTailIndex(lowerSql, 0);
        return originalSql.substring(0, insertPosition) + " WHERE " + dataScopeSql
                + originalSql.substring(insertPosition);
    }

    /**
     * 定位 ORDER BY / GROUP BY / LIMIT 中最早出现的位置。
     *
     * @param lowerSql 已转为小写的 SQL
     * @param from     起始搜索位置
     * @return 插入位置；三者都不存在时返回 SQL 长度
     */
    private int findTailIndex(String lowerSql, int from) {
        int position = lowerSql.length();
        for (String keyword : new String[]{"order by", "group by", "limit"}) {
            int index = lowerSql.indexOf(keyword, from);
            if (index >= 0 && index < position) {
                position = index;
            }
        }
        return position;
    }

    /**
     * 创建包含新 SQL 的 MappedStatement
     */
    private MappedStatement createNewMappedStatement(MappedStatement originalMs, String newSql) {
        SqlSource newSqlSource = new SqlSource() {
            @Override
            public BoundSql getBoundSql(Object parameterObject) {
                BoundSql boundSql = originalMs.getBoundSql(parameterObject);
                return new BoundSql(originalMs.getConfiguration(), newSql, boundSql.getParameterMappings(),
                        parameterObject);
            }
        };
        MappedStatement.Builder msBuilder = new MappedStatement.Builder(originalMs.getConfiguration(),
                originalMs.getId(), newSqlSource, originalMs.getSqlCommandType());
        // 数据权限与二级缓存不兼容：CacheKey 由原始语句生成，命中缓存会绕过用户过滤
        msBuilder.useCache(false);
        // 复制原始 MappedStatement 的属性
        msBuilder.fetchSize(originalMs.getFetchSize());
        msBuilder.timeout(originalMs.getTimeout());
        msBuilder.statementType(originalMs.getStatementType());
        msBuilder.resultSetType(originalMs.getResultSetType());
        msBuilder.resultMaps(originalMs.getResultMaps());
        msBuilder.keyGenerator(originalMs.getKeyGenerator());
        if (originalMs.getKeyProperties() != null) {
            for (String keyProperty : originalMs.getKeyProperties()) {
                msBuilder.keyProperty(keyProperty);
            }
        }
        msBuilder.databaseId(originalMs.getDatabaseId());
        msBuilder.lang(originalMs.getLang());
        return msBuilder.build();
    }

    /**
     * 清除用户权限缓存（当角色或部门数据变更时调用）。
     *
     * @param userId 用户 ID；为 {@code null} 时清空全部
     */
    public void clearUserScopeCache(Long userId) {
        userDataScopeResolver.clearUserScopeCache(userId);
    }

}
