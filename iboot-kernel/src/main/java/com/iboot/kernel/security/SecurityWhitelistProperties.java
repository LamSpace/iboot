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

package com.iboot.kernel.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 各服务的免鉴权路径白名单。
 *
 * <p>白名单属于服务边界的一部分，因此由宿主通过 {@code iboot.security.whitelist} 声明，
 * 内核只提供与业务无关的默认值（接口文档、健康检查与错误页）。匹配语义与迁移前一致：
 * <ul>
 *     <li>以 {@code /x/**} 形式结尾的模式按前缀匹配；</li>
 *     <li>以扩展名通配符开头的模式按文件后缀匹配；</li>
 *     <li>其余含 {@code *} 的模式按通配符前的前缀匹配；</li>
 *     <li>不含通配符的模式按精确匹配。</li>
 * </ul>
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.security")
public class SecurityWhitelistProperties {

    /**
     * 免鉴权路径模式列表。
     */
    private List<String> whitelist = List.of(
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config", "/swagger-resources/**", "/webjars/**", "/doc.html",
            "/actuator/**", "/error");

    /**
     * 判断给定路径是否无需鉴权。
     *
     * @param requestURI 请求路径
     * @return 命中任一模式时返回 {@code true}
     */
    public boolean isWhitelisted(String requestURI) {
        for (String pattern : whitelist) {
            if (matchesPattern(requestURI, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按既有语义匹配单个模式。
     *
     * @param path 请求路径
     * @param pattern 白名单模式
     * @return 是否匹配
     */
    private static boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        } else if (pattern.startsWith("/**/*.")) {
            String extension = pattern.substring(5);
            return path.endsWith(extension);
        } else if (pattern.contains("*")) {
            String prefix = pattern.substring(0, pattern.indexOf('*'));
            return path.startsWith(prefix);
        } else {
            return path.equals(pattern);
        }
    }

    /**
     * 获取白名单模式列表。
     *
     * @return 白名单模式列表
     */
    public List<String> getWhitelist() {
        return whitelist;
    }

    /**
     * 设置白名单模式列表。
     *
     * @param whitelist 白名单模式列表
     */
    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
