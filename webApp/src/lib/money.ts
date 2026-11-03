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
 * The decimals an amount shows: none when they are all zero ("R$ 500"), every one of the currency otherwise
 * ("R$ 12,50"), amount by amount. The shared code's `GroupCurrency.getDisplayFractionDigits`, which the Android and
 * Apple apps call. Amount fields keep the currency's own.
 */
function shownDigits(minor: number, fractionDigits: number): number {
  return minor % 10 ** fractionDigits === 0 ? 0 : fractionDigits
}

/**
 * An amount in the currency's minor unit as the UI's language writes it: "R$ 312,50" in Portuguese,
 * "R$312.50" in English, "R$ 312" with no cents. Falls back to the ISO code where the browser doesn't know the
 * currency.
 */
export function formatMoney(minor: number, currency: string, fractionDigits: number): string {
  const major = minor / 10 ** fractionDigits
  const digits = shownDigits(minor, fractionDigits)
  try {
    return moneyFormat(currency, digits).format(major)
  } catch {
    return `${currency} ${major.toFixed(digits)}`
  }
}

/** A balance with its sign, "+R$ 600" or "−R$ 118,50" (a true minus sign), and no sign at zero. */
export function formatSignedMoney(minor: number, currency: string, fractionDigits: number): string {
  return withSign(minor, formatMoney(Math.abs(minor), currency, fractionDigits))
}

/** A balance with its sign and no symbol, "+214,90" or "−98", where the currency is the line's own. */
export function formatSignedPlainMoney(minor: number, fractionDigits: number): string {
  return withSign(minor, formatPlainMoney(Math.abs(minor), fractionDigits))
}

function withSign(minor: number, amount: string): string {
  if (minor > 0) return `+${amount}`
  if (minor < 0) return `−${amount}`
  return amount
}

/** "120,50" or "120.50" with no symbol, where the currency is already on the same line ("you get 120 and owe 218.50"). */
export function formatPlainMoney(minor: number, fractionDigits: number): string {
  const digits = shownDigits(minor, fractionDigits)
  const key = `plain:${digits}`
  let format = formatters.get(key)
  if (!format) {
    format = new Intl.NumberFormat(uiTag, { minimumFractionDigits: digits, maximumFractionDigits: digits })
    formatters.set(key, format)
  }
  return format.format(minor / 10 ** fractionDigits)
}

/** The symbol before an amount field: "R$", "$", or "US$" for dollars in Portuguese. */
export function currencySymbol(currency: string): string {
  try {
    const parts = new Intl.NumberFormat(uiTag, { style: 'currency', currency }).formatToParts(0)
    return parts.find((part) => part.type === 'currency')?.value.trim() || currency
  } catch {
    return currency
  }
}

/**
 * "0,00" or "0.00", an amount field's placeholder, with the decimal separator of navigator.language: the language
 * the shared code reads its decimal separator from when it fills a field.
 */
export function amountPlaceholder(fractionDigits: number): string {
  try {
    return new Intl.NumberFormat(typeof navigator === 'undefined' ? undefined : navigator.language, { minimumFractionDigits: fractionDigits, maximumFractionDigits: fractionDigits }).format(0)
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
