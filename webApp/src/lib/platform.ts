/** The browser's preferred languages, most preferred first. */
export function browserLanguages(): readonly string[] {
  if (typeof navigator === 'undefined') return []
  if (navigator.languages?.length) return navigator.languages
  return navigator.language ? [navigator.language] : []
}

const applePattern = /Mac|iPhone|iPad|iPod/

/**
 * Whether the browser runs on a Mac, iPhone or iPad. Client hints answer first where the browser has
 * them; Safari doesn't, so the platform and user agent strings follow. iPadOS reports a Mac, which
 * still counts as Apple.
 */
export function isAppleDevice(): boolean {
  if (typeof navigator === 'undefined') return false
  const hintedPlatform = (navigator as Navigator & { userAgentData?: { platform?: string } }).userAgentData?.platform
  if (hintedPlatform) return hintedPlatform === 'macOS' || hintedPlatform === 'iOS'
  return applePattern.test(navigator.platform ?? '') || applePattern.test(navigator.userAgent ?? '')
}

export type AccountProvider = 'google' | 'apple'

/** Apple first on Apple devices, Google first everywhere else, wherever both account buttons show. */
export const accountProviders: readonly AccountProvider[] = isAppleDevice() ? ['apple', 'google'] : ['google', 'apple']
