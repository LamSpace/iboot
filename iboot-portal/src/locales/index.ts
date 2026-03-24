import { createI18n } from "vue-i18n";
import zhCN from "./zh-CN/index";
import enUS from "./en-US/index";

// 默认语言
const defaultLocale = localStorage.getItem("locale") || "zh-CN";

const i18n = createI18n({
  legacy: false, // Composition API 模式
  locale: defaultLocale,
  fallbackLocale: "zh-CN",
  messages: {
    "zh-CN": zhCN,
    "en-US": enUS,
  },
  globalInjection: true,
});

export default i18n;
