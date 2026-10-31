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

package com.iboot.kernel.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志切面。
 *
 * <p>拦截 {@code @Log} 标记的方法，记录模块、业务类型、请求参数、响应数据、
 * 耗时和操作人等信息，并交给 {@link OperateLogRecorder} 异步落库。
 *
 * <p>序列化使用内核自带的 Jackson 3 映射器，不依赖宿主的 {@code ObjectMapper} Bean。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see Log
 */
@Aspect
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 请求参数与响应数据的序列化器。
     */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final OperateLogRecorder operateLogRecorder;

    @SuppressWarnings("all")
    public LogAspect(final OperateLogRecorder operateLogRecorder) {
        this.operateLogRecorder = operateLogRecorder;
    }

    /**
     * 记录开始时间
     */
    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    /**
     * 处理日志
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            // 获取当前用户
            String operatorName = "";
            String deptName = "";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                try {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> loginUser = (Map<String, Object>) principal;
                        operatorName = loginUser.get("username") != null ? loginUser.get("username").toString() : "";
                        deptName = loginUser.get("deptName") != null ? loginUser.get("deptName").toString() : "";
                    }
                } catch (Exception ex) {
                    log.debug("获取用户信息失败：{}", ex.getMessage());
                }
            }
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = method.getName();
            // 计算耗时
            long costTime = 0;
            if (START_TIME.get() != null) {
                costTime = System.currentTimeMillis() - START_TIME.get();
                START_TIME.remove();
            }
            // 构建日志对象
            OperateLog operateLog = OperateLog.builder()
                    .title(controllerLog.title())
                    .businessType(controllerLog.businessType().getCode())
                    .method(className + "." + methodName + "()")
                    .requestMethod(request.getMethod())
                    .operatorName(operatorName)
                    .deptName(deptName)
                    .url(request.getRequestURI())
                    .ip(getIpAddress(request))
                    .location("")
                    .status(e != null ? 0 : 1)
                    .costTime(costTime)
                    .operateTime(LocalDateTime.now())
                    .build();
            // 保存请求参数
            if (controllerLog.isSaveRequestData()) {
                setRequestValue(joinPoint, operateLog);
            }
            // 保存响应数据
            if (controllerLog.isSaveResponseData() && jsonResult != null) {
                String resultStr = JSON.writeValueAsString(jsonResult);
                operateLog.setResult(StringUtils.substring(resultStr, 0, 2000));
            }
            // 保存异常信息
            if (e != null) {
                operateLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }
            // 异步保存日志
            operateLogRecorder.record(operateLog);
        } catch (Exception ex) {
            log.error("记录操作日志异常：{}", ex.getMessage());
        }
    }

    /**
     * 获取请求参数并设置到日志中
     */
    private void setRequestValue(JoinPoint joinPoint, OperateLog operateLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String params = JSON.writeValueAsString(args);
                operateLog.setParam(StringUtils.substring(params, 0, 2000));
            }
        } catch (Exception ex) {
            log.debug("获取请求参数失败：{}", ex.getMessage());
        }
    }

    /**
     * 获取真实 IP 地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多个代理的情况，第一个 IP 为客户端真实 IP
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
