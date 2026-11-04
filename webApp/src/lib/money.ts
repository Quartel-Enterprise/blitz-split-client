import { t } from '@/i18n'
import { uiTag } from '@/lib/currency'

// Why: lists redraw every row on each keystroke, and building the Intl formatters each time is costly.
const formatters = new Map<string, Intl.NumberFormat>()

function moneyFormat(currency: string, fractionDigits: number): Intl.NumberFormat {
  const key = `${currency}:${fractionDigits}`
  let format = formatters.get(key)
  if (!format) {
    format = new Intl.NumberFormat(uiTag, {
      style: 'currency',
      currency,
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    })
    formatters.set(key, format)
  }
  return format
}

/**
 * Preferências' "Bitcoin em satoshis" on this device, which `App` sets as the shared code says it, before drawing the
 * screens under it, so every bitcoin amount (a list's, a field's) follows it.
 */
let showsBitcoinInSats = false

export function setShowsBitcoinInSats(isOn: boolean) {
  showsBitcoinInSats = isOn
}

/** The cryptocurrencies, which no browser has a symbol for: "₿" and "Ξ", where Intl puts a currency's own. */
const cryptoSymbols: Record<string, string> = { BTC: '₿', ETH: 'Ξ' }

function isSats(currency: string | undefined): boolean {
  return currency === 'BTC' && showsBitcoinInSats
}

/**
 * The decimals an amount shows: none when they are all zero ("R$ 500"), every one of the currency otherwise
 * ("R$ 12,50"), amount by amount; a cryptocurrency only the ones it needs ("₿ 0,00125"), bitcoin in satoshis none. It
 * mirrors `GroupCurrency.getDisplayFractionDigits` of the shared code, which the Android and Apple apps call, since
 * this side gets the currency as a code and its digits. Amount fields keep the currency's own.
 */
function shownDigits(minor: number, fractionDigits: number, currency?: string): number {
  if (isSats(currency)) return 0
  let fraction = Math.abs(minor % 10 ** fractionDigits)
  if (fraction === 0) return 0
  if (currency === undefined || !(currency in cryptoSymbols)) return fractionDigits
  let digits = fractionDigits
  while (fraction % 10 === 0) {
    fraction /= 10
    digits--
  }
  return digits
}

/** The number an amount shows: the major unit, or whole sats for bitcoin in satoshis (its minor unit). */
function shownNumber(minor: number, fractionDigits: number, currency?: string): number {
  return isSats(currency) ? minor : minor / 10 ** fractionDigits
}

/**
 * An amount in the currency's minor unit as the UI's language writes it: "R$ 312,50" in Portuguese,
 * "R$312.50" in English, "R$ 312" with no cents, "₿ 0,00125" and "125.000 sats". Falls back to the ISO code where the
 * browser doesn't know the currency.
 */
export function formatMoney(minor: number, currency: string, fractionDigits: number): string {
  if (isSats(currency)) return t('money_sats', { amount: formatPlainMoney(minor, fractionDigits, currency) })
  const major = shownNumber(minor, fractionDigits, currency)
  const digits = shownDigits(minor, fractionDigits, currency)
  try {
    const symbol = cryptoSymbols[currency]
    if (symbol === undefined) return moneyFormat(currency, digits).format(major)
    return moneyFormat(currency, digits)
      .formatToParts(major)
      .map((part) => (part.type === 'currency' ? symbol : part.value))
      .join('')
  } catch {
    return `${currency} ${major.toFixed(digits)}`
  }
}

/** A balance with its sign, "+R$ 600" or "−R$ 118,50" (a true minus sign), and no sign at zero. */
export function formatSignedMoney(minor: number, currency: string, fractionDigits: number): string {
  return withSign(minor, formatMoney(Math.abs(minor), currency, fractionDigits))
}

/** A balance with its sign and no symbol, "+214,90" or "−98", where the currency is the line's own. */
export function formatSignedPlainMoney(minor: number, fractionDigits: number, currency?: string): string {
  return withSign(minor, formatPlainMoney(Math.abs(minor), fractionDigits, currency))
}

function withSign(minor: number, amount: string): string {
  if (minor > 0) return `+${amount}`
  if (minor < 0) return `−${amount}`
  return amount
}

/**
 * "120,50" or "120.50" with no symbol, where the currency is already on the same line ("you get 120 and owe 218.50").
 * The currency trims a cryptocurrency's decimals and counts bitcoin in sats.
 */
export function formatPlainMoney(minor: number, fractionDigits: number, currency?: string): string {
  const digits = shownDigits(minor, fractionDigits, currency)
  const key = `plain:${digits}`
  let format = formatters.get(key)
  if (!format) {
    format = new Intl.NumberFormat(uiTag, { minimumFractionDigits: digits, maximumFractionDigits: digits })
    formatters.set(key, format)
  }
  return format.format(shownNumber(minor, fractionDigits, currency))
}

/** The symbol before an amount field: "R$", "$", or "US$" for dollars in Portuguese; "₿", "Ξ" or "sats". */
export function currencySymbol(currency: string): string {
  if (isSats(currency)) return t('money_satsSymbol')
  const crypto = cryptoSymbols[currency]
  if (crypto !== undefined) return crypto
  try {
    const parts = new Intl.NumberFormat(uiTag, { style: 'currency', currency }).formatToParts(0)
    return parts.find((part) => part.type === 'currency')?.value.trim() || currency
  } catch {
    return currency
  }
}

/**
 * "0,00" or "0.00", an amount field's placeholder, with the decimal separator of the UI's language: the tag
 * `startShared` hands `startBlitzSplit`, whose shared code fills the fields with that separator.
 */
export function amountPlaceholder(fractionDigits: number, currency?: string): string {
  if (isSats(currency)) return '0'
  try {
    return new Intl.NumberFormat(uiTag, { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits }).format(0)
  } catch {
    return (0).toFixed(fractionDigits)
  }
}

/** "15%" in the UI's language. */
export function formatPercent(value: number): string {
  return new Intl.NumberFormat(uiTag, { style: 'percent', maximumFractionDigits: 0 }).format(value / 100)
}

/** "Diego", "Diego e Elisa", "Ana, Diego e Elisa": names in the UI's language. */
export function formatNames(names: string[]): string {
  try {
    return new Intl.ListFormat(uiTag, { type: 'conjunction' }).format(names)
  } catch {
    return names.join(', ')
  }
}
