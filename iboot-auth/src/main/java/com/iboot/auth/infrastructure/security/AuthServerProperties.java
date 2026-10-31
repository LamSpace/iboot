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

package com.iboot.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权服务器配置。
 *
 * <p>绑定 {@code iboot.auth.server.*}：签发者地址、门户客户端登记信息与签名密钥文件位置。
 * 客户端登记放在配置中而不建库表，避免扩大认证服务对共享库的写入边界。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.auth.server")
public class AuthServerProperties {

    /**
     * 令牌中的签发者地址，必须等于浏览器访问授权服务器所用的地址。
     */
    private String issuer = "http://localhost:8088";

    /**
     * 门户客户端的客户端标识。
     */
    private String clientId = "iboot-portal";

    /**
     * 门户客户端的展示名称。
     */
    private String clientName = "iBoot Portal";

    /**
     * 允许的回调地址。
     */
    private List<String> redirectUris = new ArrayList<>(List.of("http://localhost:5173/callback"));

    /**
     * 登出后允许跳转的地址。
     */
    private List<String> postLogoutRedirectUris = new ArrayList<>(List.of("http://localhost:5173/login"));

    /**
     * 访问令牌有效期（分钟）。
     */
    private long accessTokenMinutes = 30L;

    /**
     * 刷新令牌有效期（分钟），与共享会话的闲置超时保持一致。
     */
    private long refreshTokenMinutes = 30L;

    /**
     * 签名私钥文件路径；为空时在启动阶段生成临时密钥。
     *
     * <p>临时密钥在重启后更换：已签发令牌在资源服务的公钥缓存仍可验签，但多实例部署必须
     * 配置同一份密钥文件。
     */
    private String jwkKeyFile;

    /**
     * 登录页展示的系统名称。
     */
    private String brandName = "iBoot";

    /**
     * 门户地址，用于登录页的回跳链接与注册入口。
     */
    private String portalUrl = "http://localhost:5173";

    /**
     * 获取签发者地址。
     *
     * @return 签发者地址
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置签发者地址。
     *
     * @param issuer 签发者地址
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 获取客户端标识。
     *
     * @return 客户端标识
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置客户端标识。
     *
     * @param clientId 客户端标识
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * 获取客户端展示名称。
     *
     * @return 客户端展示名称
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * 设置客户端展示名称。
     *
     * @param clientName 客户端展示名称
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * 获取允许的回调地址。
     *
     * @return 回调地址列表
     */
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    /**
     * 设置允许的回调地址。
     *
     * @param redirectUris 回调地址列表
     */
    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    /**
     * 获取登出后允许跳转的地址。
     *
     * @return 登出跳转地址列表
     */
    public List<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    /**
     * 设置登出后允许跳转的地址。
     *
     * @param postLogoutRedirectUris 登出跳转地址列表
     */
    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    /**
     * 获取访问令牌有效期。
     *
     * @return 有效期（分钟）
     */
    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    /**
     * 设置访问令牌有效期。
     *
     * @param accessTokenMinutes 有效期（分钟）
     */
    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    /**
     * 获取刷新令牌有效期。
     *
     * @return 有效期（分钟）
     */
    public long getRefreshTokenMinutes() {
        return refreshTokenMinutes;
    }

    /**
     * 设置刷新令牌有效期。
     *
     * @param refreshTokenMinutes 有效期（分钟）
     */
    public void setRefreshTokenMinutes(long refreshTokenMinutes) {
        this.refreshTokenMinutes = refreshTokenMinutes;
    }

    /**
     * 获取签名私钥文件路径。
     *
     * @return 私钥文件路径，未配置时为 {@code null}
     */
    public String getJwkKeyFile() {
        return jwkKeyFile;
    }

    /**
     * 设置签名私钥文件路径。
     *
     * @param jwkKeyFile 私钥文件路径
     */
    public void setJwkKeyFile(String jwkKeyFile) {
        this.jwkKeyFile = jwkKeyFile;
    }

    /**
     * 获取登录页展示的系统名称。
     *
     * @return 系统名称
     */
    public String getBrandName() {
        return brandName;
    }

    /**
     * 设置登录页展示的系统名称。
     *
     * @param brandName 系统名称
     */
    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    /**
     * 获取门户地址。
     *
     * @return 门户地址
     */
    public String getPortalUrl() {
        return portalUrl;
    }

    /**
     * 设置门户地址。
     *
     * @param portalUrl 门户地址
     */
    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }
}
