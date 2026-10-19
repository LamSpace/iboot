import { computed, readonly, ref } from "vue";

export type ThemeMode = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";

const STORAGE_KEY = "theme";
const media = window.matchMedia("(prefers-color-scheme: dark)");

function normalizeMode(value: string | null): ThemeMode {
  return value === "light" || value === "dark" || value === "system"
    ? value
    : "system";
}

const mode = ref<ThemeMode>(normalizeMode(localStorage.getItem(STORAGE_KEY)));
const systemPrefersDark = ref(media.matches);

const resolvedTheme = computed<ResolvedTheme>(() => {
  if (mode.value === "system") {
    return systemPrefersDark.value ? "dark" : "light";
  }
  return mode.value;
});

function applyTheme(theme: ResolvedTheme) {
  const root = document.documentElement;
  root.classList.toggle("dark", theme === "dark");
  root.style.colorScheme = theme;
}

function setMode(value: ThemeMode) {
  mode.value = normalizeMode(value);
  try {
    localStorage.setItem(STORAGE_KEY, mode.value);
  } catch {
    // Storage can be unavailable in private browsing; the in-memory mode still works.
  }
  applyTheme(resolvedTheme.value);
}

media.addEventListener("change", (event) => {
  systemPrefersDark.value = event.matches;
  if (mode.value === "system") {
    applyTheme(resolvedTheme.value);
  }
});

// Keep the DOM in sync with the value chosen by the pre-mount bootstrap.
applyTheme(resolvedTheme.value);

export function useTheme() {
  return {
    mode: readonly(mode),
    resolvedTheme,
    setMode,
  };
}
