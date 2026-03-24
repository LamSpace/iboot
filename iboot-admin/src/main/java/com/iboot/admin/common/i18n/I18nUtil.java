package com.iboot.admin.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 * 用于获取国际化消息
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 获取国际化消息
     *
     * @param code 消息码
     * @param args 消息参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(code, locale, args);
    }

    /**
     * 获取国际化消息（指定语言）
     *
     * @param code   消息码
     * @param locale 语言环境
     * @param args   消息参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, code, locale);
        } catch (Exception e) {
            return code;
        }
    }

    /**
     * 获取消息源
     *
     * @return MessageSource
     */
    public static MessageSource getMessageSource() {
        return messageSource;
    }
}
