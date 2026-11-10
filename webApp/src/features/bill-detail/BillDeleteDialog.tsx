import type { JsBillDeletion } from 'blitzsplit-shared'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { t, wordGap } from '@/i18n'
import { formatMoney } from '@/lib/money'
import { settledPairSentence } from './settledPairText'

/**
 * "Apagar esta conta?", from Conta and from Editar conta: open while the ViewModel holds a deletion, saying what it
 * changes for this browser's person and the payment made after the bill that it unsettles (7.4b). Cancelling it
 * any way (the button, Escape) is the ViewModel's dismissal; Apagar stays disabled while the bill is being deleted.
 */
export function BillDeleteDialog({
  deletion,
  isDeleting,
  onConfirm,
  onDismiss,
}: {
  deletion: JsBillDeletion
  isDeleting: boolean
  onConfirm: () => void
  onDismiss: () => void
}) {
  const balance = deletion.myBalanceMinor
  const money = (minor: number) => formatMoney(minor, deletion.currency, deletion.fractionDigits)
  const balanceNote =
    balance != null && balance < 0
      ? t('billDelete_noLongerOwe', { amount: money(-balance) })
      : balance != null && balance > 0
        ? t('billDelete_noLongerGetBack', { amount: money(balance) })
        : null
  const settledNote = deletion.settledPair ? settledPairSentence(deletion.settledPair) : null
  return (
    <ConfirmDialog
      title={t('billDelete_title')}
      description={[t('billDelete_body', { description: deletion.description }), balanceNote, settledNote]
        .filter(Boolean)
        .join(wordGap)}
      confirmLabel={t('billDelete_confirm')}
      isBusy={isDeleting}
      onConfirm={onConfirm}
      onDismiss={onDismiss}
    />
  )
}
