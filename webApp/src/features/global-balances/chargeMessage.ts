import type { JsChargeDraft } from 'blitzsplit-shared'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { t } from '@/i18n'
import { formatMoney, formatNames } from '@/lib/money'

/**
 * Cobrar's message in the page's language, from the debt, the groups and, if wanted, the Pix key; a guest's also says
 * what went into the bill. Once edited, the text typed.
 */
export function chargeMessageOf(charge: JsChargeDraft, pixKey: string | null | undefined): string {
  if (charge.editedMessage != null) return charge.editedMessage
  const money = (minor: number) => formatMoney(minor, charge.currency, charge.fractionDigits)
  const names = formatNames(charge.groupNames)
  const params = {
    name: nameOrFormer(charge.name),
    amount: money(charge.amountMinor),
    groups: charge.groupNames.length > 1 ? t('globalBalances_messageGroups', { names }) : t('globalBalances_messageGroup', { name: names }),
    bills: formatNames(charge.guestBills),
  }
  const body = !charge.isGuest || charge.guestBills.length === 0
    ? t('globalBalances_message', params)
    : charge.guestPaidMinor != null
      ? t('globalBalances_messageGuestPaid', { ...params, paid: money(charge.guestPaidMinor) })
      : t('globalBalances_messageGuest', params)
  return charge.includesPixKey && pixKey ? t('globalBalances_messagePix', { message: body, key: pixKey }) : body
}
