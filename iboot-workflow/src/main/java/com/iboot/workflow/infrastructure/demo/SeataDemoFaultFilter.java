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

package com.iboot.workflow.infrastructure.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Seata 演练故障过滤器。
 *
 * <p>仅测试 profile 注册。识别 {@code X-Seata-Demo-Fail: after-remote} 请求头，
 * 标记本次请求在远程额度扣减成功后抛出异常，用于验证 AT 回滚已提交分支。
 * 请求结束后清理标记，避免线程复用造成串扰。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@Profile("seata-demo")
public class SeataDemoFaultFilter extends OncePerRequestFilter {

    /**
     * 故障请求头名称。
     */
    public static final String HEADER = "X-Seata-Demo-Fail";

    /**
     * 远程扣减后失败的标记值。
     */
    public static final String AFTER_REMOTE = "after-remote";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        if (AFTER_REMOTE.equalsIgnoreCase(request.getHeader(HEADER))) {
            SeataDemoFaultContext.request();
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SeataDemoFaultContext.clear();
        }
    }
}
