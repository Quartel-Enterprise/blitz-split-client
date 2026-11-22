import { formattingLocale, isolated, locale, pickLocale } from '@/i18n'
import { browserLanguages } from '@/lib/platform'

// Why: pickLocale answers English for a tag it doesn't know ("fi"), which must not pick that tag for the picker.
const isSupportedTag = (tag: string) => pickLocale([tag]) !== 'en' || /^en(-|$)/i.test(tag)
const supportedTag = browserLanguages().find((tag) => isSupportedTag(tag) && pickLocale([tag]) === locale)
const appTag = supportedTag ?? locale
/**
 * The tag currencies are named in: the app's language, with the region of the first browser language in it
 * ("pt-PT" for a Portuguese browser in Portugal, "es-MX" for a Mexican one), so the picker never speaks another
 * language than the UI. A bare "es", from the browser or from "Español (Latinoamérica)" picked on a browser in
 * Spain, has no region of its own and speaks "es-419", Latin America's, as the page's lang does. Arabic writes Latin
 * digits (`formattingLocale`), so every number, amount and date the app formats, and the decimal separator the shared
 * code fills the amount fields with, stay the ones the fields parse. Serbian speaks its Latin script, `sr-Latn`, with the
 * browser's region, since a bare `sr` is Cyrillic to `Intl`.
 */
export const uiTag = formattingLocale(appTag.toLowerCase() === 'es' ? 'es-419' : locale === 'sr' ? new Intl.Locale(appTag, { script: 'Latn' }).toString() : appTag)

// Why: the picker draws every currency on each render, and building the Intl formatters each time is costly.
const currencyLabels = new Map<string, string>()

/** Bitcoin and ether, which no browser names: "Moeda" lists them under "Criptomoedas" (`cryptoCurrencies`). */
const cryptoLabels: Record<string, string> = { BTC: 'Bitcoin (₿)', ETH: 'Ethereum (Ξ)' }

/**
 * A group's currency as the picker names it, in the app's language: "Brazilian Real (R$)", "Bitcoin (₿)". Falls back
 * to the ISO code where the browser can't name it.
 */
export function currencyLabel(code: string, tag: string = uiTag): string {
  const crypto = cryptoLabels[code]
  if (crypto !== undefined) return crypto
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
    return `${name.charAt(0).toLocaleUpperCase(tag)}${name.slice(1)} (${isolated(symbol)})`
  } catch {
    return code
  }
}
