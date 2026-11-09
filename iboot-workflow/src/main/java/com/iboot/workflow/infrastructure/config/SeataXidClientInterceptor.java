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

package com.iboot.workflow.infrastructure.config;

import org.apache.seata.core.context.RootContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Seata XID 客户端拦截器。
 *
 * <p>当前线程存在全局事务标识时，将其写入 {@code TX_XID} 请求头，使下游服务的写入
 * 登记为同一个全局事务分支；没有标识时不写头，请求按普通本地调用处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class SeataXidClientInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 全局事务标识请求头。
     */
    public static final String XID_HEADER = "TX_XID";

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                        final ClientHttpRequestExecution execution) throws IOException {
        String xid = RootContext.getXID();
        if (xid != null && !xid.isBlank()) {
            request.getHeaders().set(XID_HEADER, xid);
        }
        return execution.execute(request, body);
    }
}
