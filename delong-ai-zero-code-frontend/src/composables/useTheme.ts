import { computed } from 'vue'
import { useThemeStore, type ThemeMode } from '@/stores/themeStore'

export function useTheme() {
  const store = useThemeStore()

  const isDark = computed(() => store.isDark)
  const mode = computed(() => store.mode)

  const setTheme = (m: ThemeMode) => store.setMode(m)
  const toggleTheme = () => store.toggleTheme()

  return { isDark, mode, setTheme, toggleTheme }
}
