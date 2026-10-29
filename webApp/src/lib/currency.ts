import { locale } from '@/i18n'
import { browserLanguages } from '@/lib/platform'

const defaultSymbol = '$'

const euro = ['AT', 'BE', 'HR', 'CY', 'EE', 'FI', 'FR', 'DE', 'GR', 'IE', 'IT', 'LV', 'LT', 'LU', 'MT', 'NL', 'PT', 'SK', 'SI', 'ES', 'AD', 'MC', 'SM', 'VA', 'ME', 'XK']

/** ISO 4217 currency of each region the badge knows; any other region shows the default "$". */
const currencyByRegion: Record<string, string> = {
  // The Americas
  US: 'USD', CA: 'CAD', MX: 'MXN', BR: 'BRL', AR: 'ARS', CL: 'CLP', CO: 'COP', PE: 'PEN', UY: 'UYU',
  PY: 'PYG', BO: 'BOB', VE: 'VES', EC: 'USD', SV: 'USD', PA: 'USD', PR: 'USD', GT: 'GTQ', HN: 'HNL',
  NI: 'NIO', CR: 'CRC', DO: 'DOP', CU: 'CUP', JM: 'JMD', TT: 'TTD', BS: 'BSD', BB: 'BBD', BZ: 'BZD',
  GY: 'GYD', SR: 'SRD', HT: 'HTG',
  // The eurozone and the rest of Europe
  ...Object.fromEntries(euro.map((region) => [region, 'EUR'])),
  GB: 'GBP', CH: 'CHF', LI: 'CHF', SE: 'SEK', NO: 'NOK', DK: 'DKK', FO: 'DKK', GL: 'DKK', IS: 'ISK',
  PL: 'PLN', CZ: 'CZK', HU: 'HUF', RO: 'RON', TR: 'TRY',
  // Asia and Oceania
  JP: 'JPY', CN: 'CNY', HK: 'HKD', TW: 'TWD', IN: 'INR', KR: 'KRW', SG: 'SGD', AU: 'AUD', NZ: 'NZD',
  // Africa and the Middle East
  ZA: 'ZAR', IL: 'ILS', AE: 'AED', AO: 'AOA', MZ: 'MZN', CV: 'CVE',
}

/**
 * The narrow currency symbol of a language tag's region ("pt-BR" → "R$", "de-DE" → "€"), or "$" when
 * the tag has no region the table knows. A tag without a region takes its likely one ("pt" → BR).
 */
export function currencySymbolFor(tag: string | undefined): string {
  if (!tag) return defaultSymbol
  try {
    const region = new Intl.Locale(tag).maximize().region
    const currency = region ? currencyByRegion[region] : undefined
    if (!currency) return defaultSymbol
    const parts = new Intl.NumberFormat(tag, { style: 'currency', currency, currencyDisplay: 'narrowSymbol' }).formatToParts(0)
    // NFKC turns Japanese's full-width "￥" into "¥"
    const symbol = parts.find((part) => part.type === 'currency')?.value.normalize('NFKC').trim()
    return symbol || defaultSymbol
  } catch {
    return defaultSymbol
  }
}

/** The brand badge's symbol: the currency of the browser's preferred language's region. */
export const regionCurrencySymbol = currencySymbolFor(browserLanguages()[0])

/**
 * The tag currencies are named in: the app's language, with the region of the first browser language in it
 * ("pt-PT" for a Portuguese browser in Portugal), so the picker never speaks another language than the UI.
 */
const uiTag = browserLanguages().find((tag) => tag.toLowerCase().split('-')[0] === locale) ?? locale

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
