package com.iboot.admin.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具。
 *
 * <p>从 Spring {@link MessageSource} 按当前请求的 {@link Locale} 解析消息码。
 * 在容器启动时通过构造器保存静态引用，供无法注入依赖的代码调用。
 *
 * <p>消息码不存在或解析失败时返回消息码本身，保证调用方始终能拿到可展示的字符串。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see I18nMessage
 */
@Component
public class I18nUtil {

    /**
     * Spring 消息源，由构造器注入。
     */
    private static MessageSource messageSource;

    /**
     * 保存 Spring 消息源。
     *
     * @param messageSource Spring 消息源
     */
    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 按当前请求语言获取国际化消息。
     *
     * @param code 消息码
     * @param args 消息参数
     * @return 解析后的消息；消息码不存在时返回消息码本身
     */
    public static String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(code, locale, args);
    }

    /**
     * 按指定语言获取国际化消息。
     *
     * @param code 消息码
     * @param locale 目标语言环境
     * @param args 消息参数
     * @return 解析后的消息；消息码不存在时返回消息码本身
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, code, locale);
        } catch (Exception e) {
            return code;
        }
    }

    /**
     * 获取 Spring 消息源。
     *
     * @return 当前 {@link MessageSource}
     */
    public static MessageSource getMessageSource() {
        return messageSource;
    }
}
