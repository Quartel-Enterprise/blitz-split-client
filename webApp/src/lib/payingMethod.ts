/*
 * The codes of the shared PayingMethodType and CardColor, as the bridge sends them. Records keyed by them make the
 * type check ask for every case's words and colors, so a type added later can't show an empty label.
 */
import type { JsPayingMethod } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'

export type PayingMethodType = 'credit_card' | 'debit_card' | 'cash' | 'pix' | 'bank_transfer' | 'bitcoin' | 'other'
export type CardColor = 'purple' | 'blue' | 'green' | 'orange' | 'red' | 'pink' | 'gold' | 'graphite'

const typeNames: Record<PayingMethodType, MessageKey> = {
  credit_card: 'payingMethod_typeCreditCard',
  debit_card: 'payingMethod_typeDebitCard',
  cash: 'payingMethod_typeCash',
  pix: 'payingMethod_typePix',
  bank_transfer: 'payingMethod_typeBankTransfer',
  bitcoin: 'payingMethod_typeBitcoin',
  other: 'payingMethod_typeOther',
}

export const cardColorNames: Record<CardColor, MessageKey> = {
  purple: 'payingMethod_colorPurple',
  blue: 'payingMethod_colorBlue',
  green: 'payingMethod_colorGreen',
  orange: 'payingMethod_colorOrange',
  red: 'payingMethod_colorRed',
  pink: 'payingMethod_colorPink',
  gold: 'payingMethod_colorGold',
  graphite: 'payingMethod_colorGraphite',
}

// Why: a card's tile is the same color in light and dark, as on Android and the Apple app; white glyphs read on each.
export const cardColorHexes: Record<CardColor, string> = {
  purple: '#6D4AB8',
  blue: '#1565C0',
  green: '#2E7D32',
  orange: '#E65100',
  red: '#C62828',
  pink: '#AD1457',
  gold: '#9A7500',
  graphite: '#455A64',
}

/** "Credit card", "Pix"…; a code a newer app version added reads as "Other" rather than its raw code. */
export function payingMethodTypeName(type: string): string {
  return t(typeNames[type as PayingMethodType] ?? 'payingMethod_typeOther')
}

/** The method's name, or its type's when it has none. */
export function payingMethodLabel(method: JsPayingMethod): string {
  return method.name.trim() !== '' ? method.name : payingMethodTypeName(method.type)
}

/** The type's name under a named method, nothing under one that already reads as its type. */
export function payingMethodSubtitle(method: JsPayingMethod): string | null {
  return method.name.trim() !== '' ? payingMethodTypeName(method.type) : null
}
