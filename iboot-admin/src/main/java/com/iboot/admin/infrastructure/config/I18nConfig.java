package com.iboot.admin.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * 国际化配置类
 * 配置 Spring MessageSource 和 LocaleResolver
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class I18nConfig {

    /**
     * 配置 MessageSource Bean
     * 用于解析国际化消息文件
     *
     * @return MessageSource 消息源
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // 设置消息文件基名
        messageSource.setBasenames("i18n/messages", "i18n/exception/exceptions", "i18n/business/business");
        // 设置默认编码为 UTF-8
        messageSource.setDefaultEncoding("UTF-8");
        // 设置缓存时间为 1 小时
        messageSource.setCacheSeconds(3600);
        // 如果找不到对应语言的翻译，回退到默认语言
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * 配置 LocaleResolver Bean
     * 根据 Accept-Language 请求头解析用户语言偏好
     *
     * @return LocaleResolver 语言解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        // 设置默认语言为简体中文
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return resolver;
    }
}
