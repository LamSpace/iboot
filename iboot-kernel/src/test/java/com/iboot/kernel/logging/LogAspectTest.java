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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LogAspect} 的记录内容测试。
 *
 * <p>操作日志从 admin 迁到内核后，主要风险是字段映射错位或丢失异常信息；这里直接断言
 * 交给记录器的字段，不依赖数据库。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class LogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 异常路径应记录标题、业务类型、请求信息与异常消息。
     *
     * @throws Exception 反射获取注解失败时抛出
     */
    @Test
    void recordsFieldsOnThrowingPath() throws Exception {
        OperateLogRecorder recorder = mock(OperateLogRecorder.class);
        LogAspect aspect = new LogAspect(recorder);
        Method method = SampleController.class.getDeclaredMethod("run");
        Log annotation = method.getAnnotation(Log.class);

        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new SampleController());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/demo");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        aspect.doAfterThrowing(joinPoint, annotation, new IllegalStateException("boom"));

        ArgumentCaptor<OperateLog> captor = ArgumentCaptor.forClass(OperateLog.class);
        verify(recorder).record(captor.capture());
        OperateLog recorded = captor.getValue();
        assertThat(recorded.getTitle()).isEqualTo("测试操作");
        assertThat(recorded.getBusinessType()).isEqualTo(BusinessTypeEnum.INSERT.getCode());
        assertThat(recorded.getRequestMethod()).isEqualTo("POST");
        assertThat(recorded.getUrl()).isEqualTo("/api/demo");
        assertThat(recorded.getStatus()).isZero();
        assertThat(recorded.getErrorMsg()).isEqualTo("boom");
    }

    /**
     * 测试用的控制器，提供一个带 {@link Log} 的方法。
     */
    static class SampleController {

        /**
         * 被切面拦截的目标方法。
         */
        @Log(title = "测试操作", businessType = BusinessTypeEnum.INSERT)
        public void run() {
            // 仅用于提供注解实例
        }
    }
}
