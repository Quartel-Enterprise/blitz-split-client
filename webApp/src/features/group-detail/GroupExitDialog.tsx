import { Loader2, LogOut, Trash2, TriangleAlert } from 'lucide-react'
import { toast } from 'sonner'
import type { JsDebtRow, JsGroupExit, JsGroupExitDialog } from 'blitzsplit-shared'
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
import { balanceTone, debtSentence } from '@/features/balances/BalanceCard'
import { t } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

/** The actions Despesas and Saldos toast: leaving or deleting that didn't reach the server, a change not saved. */
export function toastGroupAction(action: string) {
  if (action === 'leave-error') toast(t('groupExit_leaveError'))
  if (action === 'delete-error') toast(t('groupExit_deleteError'))
  if (action === 'change-error') toast(t('groupDetail_changeError'))
}

/** What the dialogs of "Sair do grupo" and "Apagar grupo" ask of the ViewModel. */
export interface GroupExitEvents {
  leaveGroup(): void
  deleteGroup(): void
  confirmGroupExit(): void
  dismissGroupExit(): void
  backToGroups(): void
}

/**
 * What "Sair do grupo" and "Apagar grupo" open (5.3c to 5.3f), and "Grupo apagado" (6.1k), over Despesas or Saldos.
 * Closing one any way (the button, Escape) is `dismissGroupExit`; the main button waits while the server answers.
 */
export function GroupExitDialog({
  exit,
  currency,
  fractionDigits,
  events,
}: {
  exit: JsGroupExit
  currency: string
  fractionDigits: number
  events: GroupExitEvents
}) {
  if (exit.deletedNotice) {
    const notice = exit.deletedNotice
    return (
      <AlertDialog
        open
        onOpenChange={(open) => {
          if (!open) events.backToGroups()
        }}
      >
        <AlertDialogContent className="sm:max-w-md">
          <AlertDialogHeader>
            <Trash2 aria-hidden className="size-6 self-center text-muted-foreground sm:self-start" />
            <AlertDialogTitle>{t('groupExit_deletedTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {notice.deletedByName
                ? t('groupExit_deletedBody', { name: notice.deletedByName, group: notice.groupName })
                : t('groupExit_deletedBodyNobody', { group: notice.groupName })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <Button onClick={() => events.backToGroups()}>{t('groupExit_backToGroups')}</Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    )
  }
  const dialog = exit.dialog
  if (!dialog) return null
  const copy = copyOf(dialog)
  return (
    <AlertDialog
      open
      onOpenChange={(open) => {
        if (!open) events.dismissGroupExit()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <copy.icon aria-hidden className={cn('size-6 self-center sm:self-start', copy.isDestructive ? 'text-destructive' : 'text-muted-foreground')} />
          <AlertDialogTitle>{copy.title}</AlertDialogTitle>
          <AlertDialogDescription>{copy.body}</AlertDialogDescription>
        </AlertDialogHeader>
        {dialog.debts.length > 0 && (
          <ul className="flex flex-col divide-y rounded-lg border bg-muted/40 text-sm">
            {dialog.debts.map((debt) => (
              <li key={`${debt.fromMemberId}-${debt.toMemberId}`} className="flex items-center justify-between gap-3 px-3 py-2">
                <span>{debtSentence(debt)}</span>
                <DebtAmount debt={debt} currency={currency} fractionDigits={fractionDigits} />
              </li>
            ))}
          </ul>
        )}
        <AlertDialogFooter>
          <AlertDialogCancel disabled={exit.isRunning}>{copy.dismiss}</AlertDialogCancel>
          {/* Not an AlertDialogAction, which would close it through onOpenChange as a dismissal */}
          <Button variant={copy.isDestructive ? 'destructive' : 'default'} disabled={exit.isRunning} onClick={() => events.confirmGroupExit()}>
            {exit.isRunning && <Loader2 className="animate-spin" />}
            {copy.confirm}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function copyOf(dialog: JsGroupExitDialog) {
  const group = dialog.groupName
  switch (dialog.kind) {
    case 'leave-blocked':
      return {
        icon: TriangleAlert,
        title: t('groupExit_leaveBlockedTitle'),
        body: t('groupExit_leaveBlockedBody', { group }),
        confirm: t(dialog.canSettle ? 'balances_settle' : 'groupExit_seeBalances'),
        dismiss: t('groupExit_notNow'),
        isDestructive: false,
      }
    case 'confirm-leave':
      return {
        icon: LogOut,
        title: t('groupExit_leaveTitle', { group }),
        body:
          dialog.otherMemberCount === 0
            ? t('groupExit_leaveBodyAlone')
            : t(dialog.otherMemberCount === 1 ? 'groupExit_leaveBodyOne' : 'groupExit_leaveBodyOther', { count: dialog.otherMemberCount }),
        confirm: t('groupExit_leaveConfirm'),
        dismiss: t('common_cancel'),
        isDestructive: true,
      }
    case 'delete-blocked':
      return {
        icon: TriangleAlert,
        title: t('groupExit_deleteBlockedTitle'),
        body: t(dialog.debts.length === 1 ? 'groupExit_deleteBlockedBodyOne' : 'groupExit_deleteBlockedBodyOther', {
          group,
          count: dialog.debts.length,
        }),
        confirm: t('groupExit_seeBalances'),
        dismiss: t('groupExit_notNow'),
        isDestructive: false,
      }
    default: {
      const members = t(dialog.memberCount === 1 ? 'groupExit_membersOne' : 'groupExit_membersOther', { count: dialog.memberCount })
      const bills = t(dialog.billCount === 1 ? 'groupExit_billsOne' : 'groupExit_billsOther', { count: dialog.billCount })
      return {
        icon: Trash2,
        title: t('groupExit_deleteTitle', { group }),
        body: dialog.billCount === 0 ? t('groupExit_deleteBodyNoBills', { members }) : t('groupExit_deleteBody', { bills, members }),
        confirm: t('groupExit_deleteConfirm'),
        dismiss: t('common_cancel'),
        isDestructive: true,
      }
    }
  }
}

/** "−R$ 98,00" what you owe, "+R$ 42,00" what you get back, "R$ 42,00" between the others. */
function DebtAmount({ debt, currency, fractionDigits }: { debt: JsDebtRow; currency: string; fractionDigits: number }) {
  if (debt.kind === 'you-owe') {
    return <strong className={cn('tabular-nums', balanceTone(-1))}>{formatSignedMoney(-debt.amountMinor, currency, fractionDigits)}</strong>
  }
  if (debt.kind === 'owes-you') {
    return <strong className={cn('tabular-nums', balanceTone(1))}>{formatSignedMoney(debt.amountMinor, currency, fractionDigits)}</strong>
  }
  return <strong className="tabular-nums text-muted-foreground">{formatMoney(debt.amountMinor, currency, fractionDigits)}</strong>
}
