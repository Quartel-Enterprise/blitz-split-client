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
 * An amount in the currency's minor unit as the UI's language writes it: "R$ 312,00" in Portuguese,
 * "R$312.00" in English. Falls back to the ISO code where the browser doesn't know the currency.
 */
export function formatMoney(minor: number, currency: string, fractionDigits: number): string {
  const major = minor / 10 ** fractionDigits
  try {
    return moneyFormat(currency, fractionDigits).format(major)
  } catch {
    return `${currency} ${major.toFixed(fractionDigits)}`
  }
}

/** A balance with its sign, "+R$ 600,00" or "−R$ 118,00" (a true minus sign), and no sign at zero. */
export function formatSignedMoney(minor: number, currency: string, fractionDigits: number): string {
  const amount = formatMoney(Math.abs(minor), currency, fractionDigits)
  if (minor > 0) return `+${amount}`
  if (minor < 0) return `−${amount}`
  return amount
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
