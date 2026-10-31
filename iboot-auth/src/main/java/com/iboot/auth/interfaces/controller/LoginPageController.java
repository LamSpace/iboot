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

package com.iboot.auth.interfaces.controller;

import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.auth.infrastructure.security.AuthServerProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 登录页控制器。
 *
 * <p>登录页由认证服务自己渲染，读取 {@code sys.name} 与门户保持品牌一致；失败原因从会话中取出
 * 并原样展示，使「用户名或密码错误」「用户已被禁用」「账户锁定」等提示与拆分前一致。
 *
 * <p>模板只在构造时读取一次，请求处理阶段不做文件 I/O。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Controller
public class LoginPageController {

    private static final Logger log = LoggerFactory.getLogger(LoginPageController.class);

    private static final String TEMPLATE_PATH = "login/login.html";

    private static final String CONFIG_KEY_SYSTEM_NAME = "sys.name";

    private static final String CONFIG_KEY_REGISTER_USER = "sys.account.registerUser";

    private static final String PLACEHOLDER_BRAND = "{{BRAND}}";

    private static final String PLACEHOLDER_NOTICE = "{{NOTICE}}";

    private static final String PLACEHOLDER_ERROR = "{{ERROR}}";

    private static final String PLACEHOLDER_REGISTER_LINK = "{{REGISTER_LINK}}";

    private final AuthDirectory authDirectory;

    private final AuthServerProperties properties;

    private final String template;

    /**
     * 创建登录页控制器。
     *
     * @param authDirectory 共享库访问器，用于读取系统名称与注册开关
     * @param properties 授权服务器配置
     */
    public LoginPageController(AuthDirectory authDirectory, AuthServerProperties properties) {
        this.authDirectory = authDirectory;
        this.properties = properties;
        this.template = loadTemplate();
    }

    /**
     * 根路径重定向到门户。
     *
     * <p>认证服务没有首页；直接访问时把用户送回门户，避免出现「找不到页面」的空白页。
     *
     * @return 门户地址的重定向
     */
    @GetMapping("/")
    public String root() {
        return "redirect:" + properties.getPortalUrl();
    }

    /**
     * 渲染登录页。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @return 登录页 HTML
     */
    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String loginPage(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        String notice = request.getParameter("logout") != null ? "已安全退出" : "";
        return template
                .replace(PLACEHOLDER_BRAND, escape(systemName()))
                .replace(PLACEHOLDER_NOTICE, escape(notice))
                .replace(PLACEHOLDER_ERROR, escape(errorMessage(request)))
                .replace(PLACEHOLDER_REGISTER_LINK, registerLink());
    }

    /**
     * 读取系统名称，未配置时回落到配置文件中的名称。
     *
     * @return 系统名称
     */
    private String systemName() {
        try {
            String configured = authDirectory.findConfigValue(CONFIG_KEY_SYSTEM_NAME);
            if (configured != null && !configured.isBlank()) {
                return configured;
            }
        } catch (RuntimeException e) {
            log.warn("读取系统名称失败，使用默认名称：{}", e.getMessage());
        }
        return properties.getBrandName();
    }

    /**
     * 取出上一次登录失败的原因，取出后从会话中移除。
     *
     * @param request HTTP 请求
     * @return 失败原因；没有失败时返回空串
     */
    private String errorMessage(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return "";
        }
        Object attribute = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (attribute instanceof AuthenticationException exception) {
            request.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            return exception.getMessage() == null ? "登录失败" : exception.getMessage();
        }
        return "";
    }

    /**
     * 依据系统参数决定是否显示注册入口。
     *
     * @return 注册链接 HTML；未开启注册时为空串
     */
    private String registerLink() {
        try {
            String enabled = authDirectory.findConfigValue(CONFIG_KEY_REGISTER_USER);
            if (!"true".equalsIgnoreCase(enabled)) {
                return "";
            }
        } catch (RuntimeException e) {
            log.warn("读取注册开关失败，隐藏注册入口：{}", e.getMessage());
            return "";
        }
        return "<a href=\"" + escape(properties.getPortalUrl()) + "/register\">注册账号</a>";
    }

    /**
     * 读取登录页模板。
     *
     * @return 模板内容
     */
    private String loadTemplate() {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取登录页模板失败：" + TEMPLATE_PATH, e);
        }
    }

    /**
     * 转义 HTML 特殊字符。
     *
     * @param value 原始文本
     * @return 转义后的文本
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
