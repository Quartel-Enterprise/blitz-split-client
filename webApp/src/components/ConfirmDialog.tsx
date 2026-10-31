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
 * A destructive question ("Apagar este acerto?"), open while the ViewModel asks it. Cancelling it any way (the button,
 * Escape) is `onDismiss`; the confirm button stays disabled while `isBusy`.
 */
export function ConfirmDialog({
  title,
  description,
  confirmLabel,
  isBusy = false,
  onConfirm,
  onDismiss,
}: {
  title: string
  description: string
  confirmLabel: string
  isBusy?: boolean
  onConfirm: () => void
  onDismiss: () => void
}) {
  return (
    <AlertDialog
      open
      onOpenChange={(open) => {
        if (!open) onDismiss()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('common_cancel')}</AlertDialogCancel>
          {/* Not an AlertDialogAction, which would close it through onOpenChange as a dismissal */}
          <Button variant="destructive" disabled={isBusy} onClick={onConfirm}>
            {isBusy && <Loader2 className="animate-spin" />}
            {confirmLabel}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
