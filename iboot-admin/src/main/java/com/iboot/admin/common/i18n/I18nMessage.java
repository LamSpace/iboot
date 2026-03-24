package com.iboot.admin.common.i18n;

/**
 * 国际化消息封装类
 * 用于携带消息码和参数，延迟解析
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class I18nMessage {

    /**
     * 消息码
     */
    private final String code;

    /**
     * 消息参数
     */
    private final Object[] args;

    public I18nMessage(String code) {
        this(code, new Object[]{});
    }

    public I18nMessage(String code, Object... args) {
        this.code = code;
        this.args = args;
    }

    /**
     * 解析为国际化消息
     *
     * @return 国际化消息
     */
    public String resolve() {
        return I18nUtil.getMessage(code, args);
    }

    /**
     * 获取消息码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取消息参数
     */
    public Object[] getArgs() {
        return args;
    }
}
