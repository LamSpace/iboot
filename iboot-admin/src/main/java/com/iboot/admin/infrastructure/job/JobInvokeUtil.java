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

package com.iboot.admin.infrastructure.job;

import com.iboot.admin.common.util.SpringUtils;
import com.iboot.admin.domain.job.model.Job;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * 任务执行工具类
 *
 * @author iBoot
 */
public final class JobInvokeUtil {

    private static final Logger log = LoggerFactory.getLogger(JobInvokeUtil.class);

    private JobInvokeUtil() {
    }

    /**
     * 执行方法
     *
     * @param job 系统任务
     */
    public static void invokeMethod(Job job) throws Exception {
        String invokeTarget = job.getInvokeTarget();
        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        List<Object[]> methodParams = getMethodParams(invokeTarget);

        if (!isValidBeanName(beanName)) {
            throw new IllegalArgumentException("调用目标Bean名称不合法: " + beanName);
        }

        Object bean = SpringUtils.getBean(beanName);
        invokeMethod(bean, methodName, methodParams);
    }

    /**
     * 校验bean名称是否合法
     */
    private static boolean isValidBeanName(String beanName) {
        return StringUtils.isNotBlank(beanName) && !beanName.contains("$") && !beanName.contains("@");
    }

    /**
     * 调用任务方法
     *
     * @param bean         目标对象
     * @param methodName   方法名称
     * @param methodParams 方法参数
     */
    private static void invokeMethod(Object bean, String methodName, List<Object[]> methodParams)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        if (methodParams != null && !methodParams.isEmpty()) {
            Method method = bean.getClass().getMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, getMethodParamsValue(methodParams));
        } else {
            Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);
        }
    }

    /**
     * 获取bean名称
     *
     * @param invokeTarget 目标字符串
     * @return bean名称
     */
    public static String getBeanName(String invokeTarget) {
        String beanName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringBeforeLast(beanName, ".");
    }

    /**
     * 获取方法名称
     *
     * @param invokeTarget 目标字符串
     * @return 方法名称
     */
    public static String getMethodName(String invokeTarget) {
        String methodName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringAfterLast(methodName, ".");
    }

    /**
     * 获取method方法参数相关列表
     *
     * @param invokeTarget 目标字符串
     * @return 参数列表
     */
    public static List<Object[]> getMethodParams(String invokeTarget) {
        String methodStr = StringUtils.substringBetween(invokeTarget, "(", ")");
        if (StringUtils.isBlank(methodStr)) {
            return new LinkedList<>();
        }
        String[] methodParams = methodStr.split(",(?=(?:[^']*'[^']*')*[^']*$)");
        List<Object[]> resultList = new LinkedList<>();
        for (String methodParam : methodParams) {
            String str = StringUtils.trimToEmpty(methodParam);
            // 字符串类型
            if (StringUtils.startsWithIgnoreCase(str, "'") && StringUtils.endsWithIgnoreCase(str, "'")) {
                resultList.add(new Object[]{StringUtils.substring(str, 1, str.length() - 1), String.class});
            }
            // Boolean类型
            else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
                resultList.add(new Object[]{Boolean.valueOf(str), Boolean.class});
            }
            // Long类型
            else if (StringUtils.endsWithIgnoreCase(str, "L")) {
                resultList.add(new Object[]{Long.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Long.class});
            }
            // Double类型
            else if (StringUtils.endsWithIgnoreCase(str, "D")) {
                resultList.add(new Object[]{Double.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Double.class});
            }
            // Integer类型
            else if (StringUtils.isNumeric(str)) {
                resultList.add(new Object[]{Integer.valueOf(str), Integer.class});
            }
            // 其他类型都当作字符串处理
            else {
                resultList.add(new Object[]{str, String.class});
            }
        }
        return resultList;
    }

    /**
     * 获取参数类型数组
     *
     * @param methodParams 参数列表
     * @return 参数类型数组
     */
    public static Class<?>[] getMethodParamsType(List<Object[]> methodParams) {
        Class<?>[] paramTypes = new Class<?>[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            paramTypes[i] = (Class<?>) methodParams.get(i)[1];
        }
        return paramTypes;
    }

    /**
     * 获取参数值数组
     *
     * @param methodParams 参数列表
     * @return 参数值数组
     */
    public static Object[] getMethodParamsValue(List<Object[]> methodParams) {
        Object[] paramValues = new Object[methodParams.size()];
        for (int i = 0; i < methodParams.size(); i++) {
            paramValues[i] = methodParams.get(i)[0];
        }
        return paramValues;
    }
}
