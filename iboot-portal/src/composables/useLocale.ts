import { useI18n } from "vue-i18n";
import { computed, watch } from "vue";
import { useAppStore } from "@/stores/app";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import en from "element-plus/es/locale/lang/en";
import { changeLocale } from "@/api/i18n";

const elementLocales: Record<string, any> = {
  "zh-CN": zhCn,
  "en-US": en,
};

export function useLocale() {
  const { locale, t } = useI18n();
  const appStore = useAppStore();

  const currentLocale = computed(() => locale.value);

  const elementLocale = computed(
    () => elementLocales[locale.value as keyof typeof elementLocales] || zhCn,
  );

  const setLocale = (newLocale: string) => {
    locale.value = newLocale;
    localStorage.setItem("locale", newLocale);
    appStore.setLocale(newLocale);
  };

  const changeLocaleAsync = async (newLocale: string) => {
    setLocale(newLocale);
    // 调用后端 API 保存用户语言偏好
    try {
      await changeLocale(newLocale);
    } catch (e) {
      console.error("Failed to save locale preference:", e);
    }
  };

  // 监听语言变化，更新 Element Plus 语言
  watch(
    () => locale.value,
    (newLocale) => {
      // Element Plus 的语言通过 App.vue 中的 ElConfigProvider 自动更新
      console.log("Locale changed to:", newLocale);
    },
  );

  return {
    currentLocale,
    elementLocale,
    setLocale,
    changeLocale: changeLocaleAsync,
    t,
  };
}
