/*
 * The codes of the shared PayingMethodType, as the bridge sends them. Records keyed by them make the
 * type check ask for every case's words and colors, so a type added later can't show an empty label.
 */
import type { JsPayingMethod } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'

export type PayingMethodType = 'credit_card' | 'debit_card' | 'cash' | 'pix' | 'bank_transfer' | 'bitcoin' | 'other'

const typeNames: Record<PayingMethodType, MessageKey> = {
  credit_card: 'payingMethod_typeCreditCard',
  debit_card: 'payingMethod_typeDebitCard',
  cash: 'payingMethod_typeCash',
  pix: 'payingMethod_typePix',
  bank_transfer: 'payingMethod_typeBankTransfer',
  bitcoin: 'payingMethod_typeBitcoin',
  other: 'payingMethod_typeOther',
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
