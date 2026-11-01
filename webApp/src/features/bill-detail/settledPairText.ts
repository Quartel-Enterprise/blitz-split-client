import type { JsSettledPair } from 'blitzsplit-shared'
import type { Money } from '@/features/split-bill/splitText'
import { t, type MessageKey } from '@/i18n'

/**
 * "Elisa já acertou com Bruno depois desta conta", "Você já acertou com Bruno…" or "Elisa já acertou com você…": a
 * payment made after the bill that editing or deleting it unsettles (7.4b).
 */
export function settledPairLead(change: JsSettledPair): string {
  const payer = change.payerName ?? t('billDetail_formerMember')
  const receiver = change.receiverName ?? t('billDetail_formerMember')
  if (change.payerIsMe) return t('settledPair_iPaid', { receiver })
  if (change.receiverIsMe) return t('settledPair_paidMe', { payer })
  return t('settledPair_paid', { payer, receiver })
}

/** "O acerto continua valendo e o saldo é refeito: com a mudança, Elisa volta a dever R$ 20,00 a Bruno." */
export function settledPairDetail(change: JsSettledPair, money: Money): string {
  return t('settledPair_redone', { outcome: outcomeOf(change, money) })
}

/** The lead and the detail in one go, as "Apagar esta conta?" says them. */
export function settledPairSentence(change: JsSettledPair, money: Money): string {
  return `${settledPairLead(change)}. ${settledPairDetail(change, money)}`
}

/** "volta a dever" what was settled, "passa a dever" more or the other way round, "fica devendo" less of it. */
const OWES_KEYS = {
  'owes-again': { named: 'settledPair_owesAgain', iOwe: 'settledPair_iOweAgain', owesMe: 'settledPair_owesMeAgain' },
  owes: { named: 'settledPair_owes', iOwe: 'settledPair_iOwe', owesMe: 'settledPair_owesMe' },
  'owes-less': { named: 'settledPair_owesLess', iOwe: 'settledPair_iOweLess', owesMe: 'settledPair_owesMeLess' },
} as const satisfies Record<string, Record<'named' | 'iOwe' | 'owesMe', MessageKey>>

function outcomeOf(change: JsSettledPair, money: Money): string {
  const debtor = change.debtorName ?? t('billDetail_formerMember')
  const creditor = change.creditorName ?? t('billDetail_formerMember')
  const amount = money(change.amountMinor)
  if (change.outcome === 'even') {
    if (change.debtorIsMe) return t('settledPair_evenWithMe', { other: creditor })
    if (change.creditorIsMe) return t('settledPair_evenWithMe', { other: debtor })
    return t('settledPair_even', { debtor, creditor })
  }
  const keys = OWES_KEYS[change.outcome as keyof typeof OWES_KEYS] ?? OWES_KEYS.owes
  if (change.debtorIsMe) return t(keys.iOwe, { creditor, amount })
  if (change.creditorIsMe) return t(keys.owesMe, { debtor, amount })
  return t(keys.named, { debtor, creditor, amount })
}

