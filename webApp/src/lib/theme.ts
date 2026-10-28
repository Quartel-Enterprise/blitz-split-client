const systemDark = window.matchMedia('(prefers-color-scheme: dark)')

let theme = 'system'

/**
 * Follows the profile's theme: "light", "dark", or "system", which follows the system's scheme as it
 * changes. Applied before the shared code starts as "system", so the page never flashes.
 */
export function applyTheme(next: string) {
  theme = next
  const isDark = theme === 'dark' || (theme === 'system' && systemDark.matches)
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.style.colorScheme = isDark ? 'dark' : 'light'
}

systemDark.addEventListener('change', () => applyTheme(theme))
