import { computed, ref } from 'vue'

const STORAGE_KEY = 'yarijoo-theme'
const isDark = ref(false)
let initialized = false

function applyTheme(dark) {
  isDark.value = dark
  document.documentElement.classList.toggle('dark', dark)
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
}

export function initTheme() {
  if (typeof window === 'undefined' || initialized) return
  const saved = localStorage.getItem(STORAGE_KEY)
  applyTheme(saved === 'dark')
  initialized = true
}

export function useTheme() {
  initTheme()

  function toggleDark() {
    const next = !isDark.value
    applyTheme(next)
    localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light')
  }

  return {
    isDark: computed(() => isDark.value),
    toggleDark
  }
}
