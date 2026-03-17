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
import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.enums.DataScopeEnum;
import com.iboot.admin.infrastructure.persistence.mapper.DeptMapper;
import com.iboot.admin.infrastructure.persistence.mapper.RoleMapper;
import com.iboot.admin.infrastructure.persistence.mapper.UserMapper;
import com.iboot.admin.infrastructure.persistence.po.RolePO;
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
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限拦截器 拦截 MyBatis 查询操作，根据当前用户角色数据权限自动拼接 SQL 过滤条件
 *
 * @author iBoot
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})})
public class DataScopeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    private final ObjectProvider<RoleMapper> roleMapperProvider;

    private final ObjectProvider<DeptMapper> deptMapperProvider;

    private final ObjectProvider<UserMapper> userMapperProvider;

    /**
     * 用户数据权限缓存（避免频繁查询数据库）
     */
    private final Map<String, UserDataScope> userScopeCache = new ConcurrentHashMap<>();

    public DataScopeInterceptor(ObjectProvider<RoleMapper> roleMapperProvider,
                                ObjectProvider<DeptMapper> deptMapperProvider, ObjectProvider<UserMapper> userMapperProvider) {
        this.roleMapperProvider = roleMapperProvider;
        this.deptMapperProvider = deptMapperProvider;
        this.userMapperProvider = userMapperProvider;
    }

    private RoleMapper getRoleMapper() {
        return roleMapperProvider.getObject();
    }

    private DeptMapper getDeptMapper() {
        return deptMapperProvider.getObject();
    }

    private UserMapper getUserMapper() {
        return userMapperProvider.getObject();
    }

    /**
     * 从 MappedStatement 获取 Mapper 接口上的注解 MyBatis 3.5+ 中 MappedStatement 没有直接的 getAnnotation 方法 需要通过 Configuration 和
     * MapperRegistry 获取
     */
    private <T extends Annotation> T getAnnotationFromMappedStatement(MappedStatement ms, Class<T> annotationClass) {
        try {
            Configuration configuration = ms.getConfiguration();
            String mapperName = ms.getId();
            int lastDotIndex = mapperName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String mapperClassName = mapperName.substring(0, lastDotIndex);
                Class<?> mapperClass = Class.forName(mapperClassName);
                if (mapperClass != null) {
                    return mapperClass.getAnnotation(annotationClass);
                }
            }
        } catch (ClassNotFoundException e) {
            log.debug("无法获取 Mapper 类：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 MappedStatement 中提取表名
     */
    private String extractTableName(MappedStatement ms) {
        BoundSql boundSql = ms.getBoundSql(ms.getId());
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
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        // 检查方法是否有@DataScope 注解
        DataScope dataScope = getAnnotationFromMappedStatement(ms, DataScope.class);
        if (dataScope == null) {
            return invocation.proceed();
        }
        // 获取当前用户 ID
        Long currentUserId;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // 用户未登录，不应用数据权限（可能是系统查询或定时任务）
            log.debug("无法获取当前用户，跳过数据权限处理：{}", e.getMessage());
            return invocation.proceed();
        }
        // 超级管理员拥有全部数据权限
        if (isSuperAdmin(currentUserId)) {
            return invocation.proceed();
        }
        // 获取用户的数据权限范围
        UserDataScope userDataScope = getUserDataScope(currentUserId);
        if (userDataScope == null || DataScopeEnum.ALL.getCode().equals(userDataScope.getDataScope())) {
            // 全部数据权限，不需要过滤
            return invocation.proceed();
        }
        // 从 SQL 中提取表名
        String tableName = extractTableName(ms);
        // 生成数据权限 SQL 片段
        String dataScopeSql = generateDataScopeSql(userDataScope, dataScope, currentUserId, tableName);
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
            // 已有 WHERE 子句，在 WHERE 后追加 AND 条件
            return originalSql.substring(0, whereIndex + 5) + " " + dataScopeSql + " AND ("
                    + originalSql.substring(whereIndex + 5);
        } else {
            // 没有 WHERE 子句，需要添加
            // 查找 ORDER BY、LIMIT、GROUP BY 等关键字
            int orderIndex = lowerSql.indexOf("order by");
            int limitIndex = lowerSql.indexOf("limit");
            int groupIndex = lowerSql.indexOf("group by");
            int insertPosition = originalSql.length();
            if (orderIndex > 0 && orderIndex < insertPosition)
                insertPosition = orderIndex;
            if (limitIndex > 0 && limitIndex < insertPosition)
                insertPosition = limitIndex;
            if (groupIndex > 0 && groupIndex < insertPosition)
                insertPosition = groupIndex;
            return originalSql.substring(0, insertPosition) + " WHERE " + dataScopeSql
                    + originalSql.substring(insertPosition);
        }
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
     * 判断是否为超级管理员
     */
    private boolean isSuperAdmin(Long userId) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            return Constants.ADMIN_USERNAME.equals(username);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取用户的数据权限范围
     */
    private UserDataScope getUserDataScope(Long userId) {
        String cacheKey = "user_" + userId;
        // 先从缓存获取
        UserDataScope cached = userScopeCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        // 查询用户角色
        List<RolePO> roles = getRoleMapper().selectByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        // 找到优先级最高的数据权限（数字越小优先级越高）
        Integer dataScope = null;
        List<Long> customDeptIds = new ArrayList<>();
        for (RolePO role : roles) {
            Integer roleDataScope = role.getDataScope();
            if (roleDataScope == null) {
                continue;
            }
            // 全部数据权限优先级最高
            if (DataScopeEnum.ALL.getCode().equals(roleDataScope)) {
                dataScope = DataScopeEnum.ALL.getCode();
                break;
            }
            // 自定义数据权限需要收集部门 ID
            if (DataScopeEnum.CUSTOM.getCode().equals(roleDataScope)) {
                List<Long> deptIds = getRoleMapper().selectDeptIdsByRoleId(role.getId());
                if (deptIds != null && !deptIds.isEmpty()) {
                    customDeptIds.addAll(deptIds);
                }
                if (dataScope == null) {
                    dataScope = roleDataScope;
                }
            } else {
                // 其他权限类型，取优先级最高的
                if (dataScope == null || roleDataScope < dataScope) {
                    dataScope = roleDataScope;
                }
            }
        }
        UserDataScope userDataScope = new UserDataScope(dataScope, customDeptIds);
        userScopeCache.put(cacheKey, userDataScope);
        return userDataScope;
    }

    /**
     * 生成数据权限 SQL 片段
     */
    private String generateDataScopeSql(UserDataScope userDataScope, DataScope annotation, Long currentUserId,
                                        String tableName) {
        Integer dataScope = userDataScope.getDataScope();
        if (dataScope == null) {
            return null;
        }
        String userAlias = annotation.userAlias();
        // 部门表特殊处理：使用 id 而不是 dept_id
        boolean isDeptTable = "sys_dept".equals(tableName);
        String deptIdColumn = isDeptTable ? "id" : "dept_id";
        switch (dataScope) {
            case 1:
                // 全部数据权限
                return null; // 不需要过滤
            case 2:
                // 自定义数据权限
                List<Long> deptIds = userDataScope.getCustomDeptIds();
                if (deptIds == null || deptIds.isEmpty()) {
                    return "1=0"; // 无权限
                }
                return userAlias + "." + deptIdColumn + " IN (" + buildIdList(deptIds) + ")";
            case 3:
                // 本部门数据权限
                if (isDeptTable) {
                    // 部门表：直接等于用户所在部门 ID
                    Long userDeptId = getUserDeptId(currentUserId);
                    if (userDeptId == null) {
                        return "1=0";
                    }
                    return userAlias + "." + deptIdColumn + " = " + userDeptId;
                } else {
                    return userAlias + ".dept_id = (SELECT dept_id FROM sys_user WHERE id = " + currentUserId + ")";
                }
            case 4:
                // 本部门及以下数据权限
                Long userDeptId = getUserDeptId(currentUserId);
                if (userDeptId == null) {
                    return "1=0";
                }
                List<Long> childrenDeptIds = getChildrenDeptIds(userDeptId);
                childrenDeptIds.add(userDeptId);
                return userAlias + "." + deptIdColumn + " IN (" + buildIdList(childrenDeptIds) + ")";
            case 5:
                // 仅本人数据权限
                return userAlias + ".id = " + currentUserId;
            default:
                return null;
        }
    }

    /**
     * 构建 ID 列表字符串
     */
    private String buildIdList(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    /**
     * 获取用户所在部门 ID
     */
    private Long getUserDeptId(Long userId) {
        com.iboot.admin.infrastructure.persistence.po.UserPO user = getUserMapper().selectById(userId);
        if (user != null) {
            return user.getDeptId();
        }
        return null;
    }

    /**
     * 获取部门的所有子部门 ID
     */
    private List<Long> getChildrenDeptIds(Long deptId) {
        return getDeptMapper().selectChildrenIds(deptId);
    }

    /**
     * 清除用户权限缓存（当角色或部门数据变更时调用）
     */
    public void clearUserScopeCache(Long userId) {
        if (userId != null) {
            userScopeCache.remove("user_" + userId);
        } else {
            userScopeCache.clear();
        }
    }

    /**
     * 用户数据权限封装类
     */
    private static class UserDataScope {

        private final Integer dataScope;

        private final List<Long> customDeptIds;

        private final long expireTime;

        public UserDataScope(Integer dataScope, List<Long> customDeptIds) {
            this.dataScope = dataScope;
            this.customDeptIds = customDeptIds != null ? new ArrayList<>(customDeptIds) : new ArrayList<>();
            // 5 分钟过期
            this.expireTime = System.currentTimeMillis() + 5 * 60 * 1000;
        }

        public Integer getDataScope() {
            return dataScope;
        }

        public List<Long> getCustomDeptIds() {
            return customDeptIds;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

    }

}
