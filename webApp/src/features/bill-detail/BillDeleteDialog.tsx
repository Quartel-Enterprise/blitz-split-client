import type { JsBillDeletion } from 'blitzsplit-shared'
import { Loader2 } from 'lucide-react'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { t } from '@/i18n'

/**
 * "Apagar esta conta?", from Conta and from Editar conta: open while the ViewModel holds a deletion. Cancelling it
 * any way (the button, Escape) is the ViewModel's dismissal; Apagar stays disabled while the bill is being deleted.
 */
export function BillDeleteDialog({
  deletion,
  isDeleting,
  money,
  onConfirm,
  onDismiss,
}: {
  deletion: JsBillDeletion
  isDeleting: boolean
  money: (minor: number) => string
  onConfirm: () => void
  onDismiss: () => void
}) {
  const balance = deletion.myBalanceMinor
  const balanceNote =
    balance != null && balance < 0
      ? t('billDelete_noLongerOwe', { amount: money(-balance) })
      : balance != null && balance > 0
        ? t('billDelete_noLongerGetBack', { amount: money(balance) })
        : null
  return (
    <AlertDialog
      open
      onOpenChange={(open) => {
        if (!open) onDismiss()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{t('billDelete_title')}</AlertDialogTitle>
          <AlertDialogDescription>
            {[t('billDelete_body', { description: deletion.description }), balanceNote].filter(Boolean).join(' ')}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('common_cancel')}</AlertDialogCancel>
          {/* Not an AlertDialogAction, which would close it through onOpenChange as a dismissal */}
          <Button variant="destructive" disabled={isDeleting} onClick={onConfirm}>
            {isDeleting && <Loader2 className="animate-spin" />}
            {t('billDelete_confirm')}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
