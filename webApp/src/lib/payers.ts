import { t } from '@/i18n'
import { formatNames } from '@/lib/money'

/**
 * Who paid a bill several people paid: "Você e Ana" for two, "3 pessoas" for three or more. The names come in the
 * shared code's order, this browser's person first.
 */
export function payerNamesText(names: string[]): string {
  return names.length <= 2 ? formatNames(names) : t('splitBill_payersCount', { count: names.length })
}
