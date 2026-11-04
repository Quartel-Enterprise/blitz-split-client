import type { JsSimplifyConflict } from 'blitzsplit-shared'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { t } from '@/i18n'

/** "você" for this browser's user (inside a sentence), "Ex-membro" with no name: a JsSimplifyName or a payment's person. */
export function simplifyName(person: { name: string | null | undefined; isMe: boolean }): string {
  return person.isMe ? t('simplify_you') : nameOrFormer(person.name)
}

/** "1 pagamento", "5 pagamentos". */
export function paymentCount(count: number): string {
  return count === 1 ? t('simplify_paymentCountOne') : t('simplify_paymentCountOther', { count })
}

/** "Ana desligou depois de você. Vale a última mudança: Simplificar dívidas está desligado." (13.1e) */
export function conflictText(conflict: JsSimplifyConflict): string {
  const name = conflict.changedByName ?? t('simplify_someone')
  return t(conflict.simplifiesDebts ? 'simplifyConflict_on' : 'simplifyConflict_off', { name })
}

/** What Editar grupo and the group's screen ask of their ViewModel about 13.1e's notice. */
export interface SimplifyConflictEvents {
  dismissSimplifyConflict(): void
  openSimplifyConflictActivity(): void
}

/**
 * 13.1e: someone else changed "Simplificar dívidas" after this browser did, and the last change counts. A toast with
 * "Ver na Atividade", which only goes away by itself, a swipe or its action; either way the ViewModel hears of it once.
 */
export function useSimplifyConflictNotice(presenter: SimplifyConflictEvents | null, conflict: JsSimplifyConflict | null | undefined) {
  const text = conflict ? conflictText(conflict) : null
  useEffect(() => {
    if (!presenter || !text) return
    // Why: sonner calls onDismiss for toast.dismiss(id) too, so only a toast still showing tells the ViewModel.
    let isCurrent = true
    const dismissed = () => {
      if (!isCurrent) return
      isCurrent = false
      presenter.dismissSimplifyConflict()
    }
    const id = toast(text, {
      duration: 8000,
      action: {
        label: t('simplifyConflict_activity'),
        onClick: () => {
          isCurrent = false
          presenter.openSimplifyConflictActivity()
        },
      },
      onAutoClose: dismissed,
      onDismiss: dismissed,
    })
    return () => {
      isCurrent = false
      toast.dismiss(id)
    }
  }, [presenter, text])
}
