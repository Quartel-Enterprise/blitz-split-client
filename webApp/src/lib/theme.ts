const systemDark = window.matchMedia('(prefers-color-scheme: dark)')
const storageKey = 'theme'

let theme = 'system'

/**
 * Follows the profile's theme: "light", "dark", or "system", which follows the system's scheme as it
 * changes. The last one is kept in the browser too, so a reload paints it before the shared code
 * reads it from the database, instead of flashing the system's.
 */
export function applyTheme(next: string) {
  theme = next
  const isDark = theme === 'dark' || (theme === 'system' && systemDark.matches)
  document.documentElement.classList.toggle('dark', isDark)
  document.documentElement.style.colorScheme = isDark ? 'dark' : 'light'
  try {
    localStorage.setItem(storageKey, theme)
  } catch {
    // Without storage, the next load starts on the system's scheme until the shared code answers.
  }
}

/** The theme the last visit applied, or "system" when there is none. */
export function lastTheme(): string {
  try {
    return localStorage.getItem(storageKey) ?? 'system'
  } catch {
    return 'system'
  }
}

systemDark.addEventListener('change', () => applyTheme(theme))
