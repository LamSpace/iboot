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

package com.iboot.authz;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条服务间授权规则。
 *
 * <p>规则声明若干路径模式与允许的调用方证书 CN；路径按 {@link AntPathMatcher}
 * 语义匹配。规则本身不判断启停，启停由 {@link ServiceAuthorizationProperties}
 * 控制。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class ServiceAuthorizationRule {

    /**
     * 路径模式，例如 {@code /api/**}。
     */
    private List<String> paths = new ArrayList<>();

    /**
     * 允许的调用方证书 CN，例如 {@code iboot-gateway}。
     */
    private List<String> callers = new ArrayList<>();

    /**
     * 判断路径是否命中本规则。
     *
     * @param path    请求路径
     * @param matcher 路径匹配器
     * @return 命中任一模式时返回 {@code true}
     */
    public boolean matches(final String path, final AntPathMatcher matcher) {
        for (String pattern : paths) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断调用方是否被本规则允许。
     *
     * @param caller 调用方 CN，可为 {@code null}
     * @return 在白名单内返回 {@code true}
     */
    public boolean allows(final String caller) {
        return caller != null && callers.contains(caller);
    }

    /**
     * 获取路径模式。
     *
     * @return 路径模式列表
     */
    public List<String> getPaths() {
        return paths;
    }

    /**
     * 设置路径模式。
     *
     * @param paths 路径模式列表
     */
    public void setPaths(final List<String> paths) {
        this.paths = paths == null ? new ArrayList<>() : paths;
    }

    /**
     * 获取允许的调用方。
     *
     * @return 调用方 CN 列表
     */
    public List<String> getCallers() {
        return callers;
    }

    /**
     * 设置允许的调用方。
     *
     * @param callers 调用方 CN 列表
     */
    public void setCallers(final List<String> callers) {
        this.callers = callers == null ? new ArrayList<>() : callers;
    }
}
