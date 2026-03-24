package com.iboot.admin.interfaces.controller;

import com.iboot.admin.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 国际化控制器
 * 提供语言切换和当前语言查询接口
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Tag(name = "国际化管理", description = "语言切换、当前语言查询相关接口")
@RestController
@RequestMapping("/api/i18n")
public class I18nController {

    /**
     * Cookie 名称，用于存储用户语言偏好
     */
    private static final String LOCALE_COOKIE_NAME = "org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver.locale";

    /**
     * 修改用户语言偏好
     *
     * @param locale   语言代码，如 zh-CN、en-US
     * @param response HTTP 响应对象
     * @return 成功响应
     */
    @Operation(summary = "修改语言偏好")
    @PostMapping("/locale")
    public Result<Map<String, String>> changeLocale(@RequestParam String locale, HttpServletResponse response) {
        // 设置 Cookie，有效期 1 年
        ResponseCookie cookie = ResponseCookie.from(LOCALE_COOKIE_NAME, locale)
                .path("/")
                .maxAge(31536000)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        Map<String, String> data = Map.of(
                "locale", locale,
                "message", "Language changed successfully"
        );
        return Result.success(data);
    }

    /**
     * 获取当前语言
     *
     * @param acceptLanguage Accept-Language 请求头
     * @return 当前语言代码
     */
    @Operation(summary = "获取当前语言")
    @GetMapping("/locale")
    public Result<String> getCurrentLocale(
            @RequestParam(value = "locale", required = false) String locale,
            @RequestParam(value = "Accept-Language", required = false) String acceptLanguage) {
        // 优先使用参数，其次 Accept-Language 头，最后默认 zh-CN
        String currentLocale = locale;
        if (currentLocale == null || currentLocale.isEmpty()) {
            if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
                currentLocale = acceptLanguage.split(",")[0];
            } else {
                currentLocale = "zh-CN";
            }
        }
        return Result.success(currentLocale);
    }
}
