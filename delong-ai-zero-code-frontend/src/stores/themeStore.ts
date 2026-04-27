import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'app-theme-mode'

const resolveTheme = (mode: ThemeMode): 'light' | 'dark' => {
  if (mode === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return mode
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>((localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'system')
  const resolved = ref<'light' | 'dark'>(resolveTheme(mode.value))
  const isDark = computed(() => resolved.value === 'dark')

  const applyTheme = () => {
    resolved.value = resolveTheme(mode.value)
    document.documentElement.setAttribute('data-theme', resolved.value)
  }

  const setMode = (newMode: ThemeMode) => {
    mode.value = newMode
    localStorage.setItem(STORAGE_KEY, newMode)
    applyTheme()
  }

  const toggleTheme = () => {
    setMode(resolved.value === 'dark' ? 'light' : 'dark')
  }

  // Listen for system theme changes
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  const handleSystemChange = () => {
    if (mode.value === 'system') {
      applyTheme()
    }
  }
  mediaQuery.addEventListener('change', handleSystemChange)

  // Initialize
  applyTheme()

  return { mode, resolved, isDark, setMode, toggleTheme }
})
