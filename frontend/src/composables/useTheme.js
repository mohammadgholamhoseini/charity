export function useTheme() {
  if (typeof document !== 'undefined') {
    document.documentElement.classList.add('dark')
  }
  return { isDark: { value: true }, toggleDark: () => {} }
}
