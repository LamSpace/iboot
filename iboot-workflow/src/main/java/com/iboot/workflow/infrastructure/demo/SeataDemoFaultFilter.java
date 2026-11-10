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
 * <p>仅在 {@code seata-demo} 或 {@code seata-tcc-demo} profile 注册。识别
 * {@code X-Seata-Demo-Fail} 请求头：AT 演练使用 {@code after-remote}，TCC 演练使用
 * {@code after-tcc-try}。请求结束后清理标记，避免线程复用造成串扰。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@Profile({"seata-demo", "seata-tcc-demo"})
public class SeataDemoFaultFilter extends OncePerRequestFilter {

    /**
     * 故障请求头名称。
     */
    public static final String HEADER = "X-Seata-Demo-Fail";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        String mode = request.getHeader(HEADER);
        if (SeataDemoFaultContext.AFTER_REMOTE.equalsIgnoreCase(mode)
                || SeataDemoFaultContext.AFTER_TCC_TRY.equalsIgnoreCase(mode)) {
            SeataDemoFaultContext.request(mode.toLowerCase());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SeataDemoFaultContext.clear();
        }
    }
}
