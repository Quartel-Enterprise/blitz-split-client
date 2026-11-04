import { locale } from '@/i18n'
import { browserLanguages } from '@/lib/platform'

/**
 * The tag currencies are named in: the app's language, with the region of the first browser language in it
 * ("pt-PT" for a Portuguese browser in Portugal), so the picker never speaks another language than the UI.
 */
export const uiTag = browserLanguages().find((tag) => tag.toLowerCase().split('-')[0] === locale) ?? locale

// Why: the picker draws every currency on each render, and building the Intl formatters each time is costly.
const currencyLabels = new Map<string, string>()

/**
 * A group's currency as the picker names it, in the app's language: "Brazilian Real (R$)". Falls back to the
 * ISO code where the browser can't name it.
 */
export function currencyLabel(code: string, tag: string = uiTag): string {
  const key = `${tag}:${code}`
  const cached = currencyLabels.get(key)
  if (cached !== undefined) return cached
  const label = buildCurrencyLabel(code, tag)
  currencyLabels.set(key, label)
  return label
}

function buildCurrencyLabel(code: string, tag: string): string {
  try {
    const name = new Intl.DisplayNames(tag, { type: 'currency' }).of(code) ?? code
    const parts = new Intl.NumberFormat(tag, { style: 'currency', currency: code }).formatToParts(0)
    const symbol = parts.find((part) => part.type === 'currency')?.value.trim() || code
    return `${name.charAt(0).toLocaleUpperCase(tag)}${name.slice(1)} (${symbol})`
  } catch {
    return code
  }
}
