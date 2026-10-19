package com.iboot.admin.common.i18n;

/**
 * 国际化消息占位对象。
 *
 * <p>延迟保存消息码和参数，在真正需要展示时调用 {@link #resolve()} 按当前请求语言解析。
 * 适用于先构造异常或响应、后解析消息的场景，避免在非请求线程中丢失语言上下文。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see I18nUtil
 */
public class I18nMessage {

    /**
     * 国际化消息码。
     */
    private final String code;

    /**
     * 消息格式化参数；没有参数时为空数组。
     */
    private final Object[] args;

    /**
     * 创建无参数的消息。
     *
     * @param code 国际化消息码
     */
    public I18nMessage(String code) {
        this(code, new Object[]{});
    }

    /**
     * 创建带参数的消息。
     *
     * @param code 国际化消息码
     * @param args 消息格式化参数
     */
    public I18nMessage(String code, Object... args) {
        this.code = code;
        this.args = args;
    }

    /**
     * 按当前请求语言解析消息。
     *
     * @return 解析后的消息；消息码不存在时返回消息码本身
     */
    public String resolve() {
        return I18nUtil.getMessage(code, args);
    }

    /**
     * 获取消息码。
     *
     * @return 国际化消息码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取消息参数。
     *
     * @return 消息参数数组；没有参数时为空数组
     */
    public Object[] getArgs() {
        return args;
    }
}
