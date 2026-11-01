import { ArrowRight, ChevronRight, CircleCheck } from 'lucide-react'
import { PickPaymentPresenter, type JsPickPaymentGroup, type JsPickPaymentPair, type JsPickPaymentState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { nameOrFormer } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { PersonFace } from '@/features/register-payment/RegisterPaymentDialog'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { formatMoney } from '@/lib/money'

/**
 * "Quem pagou a quem?", Início's "Registrar pagamento" before the form, presented as Registrar pagamento is: a dialog
 * on desktop and tablet, a drawer on the phone. Every open pair of every group, under the group's name; a pair opens
 * Registrar pagamento in this dialog's place. Closing it any way is the ViewModel's close.
 */
export function PickPaymentDialog() {
  const presenter = usePresenter(() => new PickPaymentPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsPickPaymentState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state?.isLoaded) return null

  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      pane={{
        title: t('pickPayment_title'),
        body: state.showsEmpty ? (
          <div className="flex flex-col items-center gap-2 py-6 text-center">
            <CircleCheck aria-hidden className="size-8 text-muted-foreground" />
            <p className="font-semibold">{t('pickPayment_emptyTitle')}</p>
            <p className="max-w-xs text-sm text-muted-foreground">{t('pickPayment_emptyBody')}</p>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {state.groups.map((group) => (
              <PairsOfGroup key={group.groupId} group={group} onPick={(pair) => presenter.pickPair(group.groupId, pair.fromMemberId, pair.toMemberId)} />
            ))}
          </div>
        ),
        footer: (
          <Button variant="outline" onClick={() => presenter.close()}>
            {t(state.showsEmpty ? 'common_close' : 'common_cancel')}
          </Button>
        ),
      }}
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      className="gap-5 sm:max-w-[480px]"
    />
  )
}

function PairsOfGroup({ group, onPick }: { group: JsPickPaymentGroup; onPick: (pair: JsPickPaymentPair) => void }) {
  const headingId = `pick-payment-${group.groupId}`
  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-1.5">
      <h3 id={headingId} className="flex items-center gap-2 text-[13px] font-medium text-muted-foreground">
        <GroupAvatar initials={group.initials} color={group.color} className="size-6 rounded-md text-[10px]" />
        <span className="truncate">{group.name}</span>
      </h3>
      <ul className="flex flex-col divide-y rounded-lg border">
        {group.pairs.map((pair) => (
          <li key={`${pair.fromMemberId}/${pair.toMemberId}`}>
            <button
              onClick={() => onPick(pair)}
              className="flex min-h-14 w-full items-center gap-3 px-3 py-2 text-left text-sm outline-none hover:bg-accent focus-visible:ring-[3px] focus-visible:ring-ring focus-visible:ring-inset"
            >
              <span aria-hidden className="flex shrink-0 items-center gap-1">
                <PersonFace person={pair.from} className="size-8 text-xs" />
                <ArrowRight className="size-3.5 text-muted-foreground" />
                <PersonFace person={pair.to} className="size-8 text-xs" />
              </span>
              <span className="min-w-0 flex-1">{pairSentence(pair, formatMoney(pair.amountMinor, group.currency, group.fractionDigits))}</span>
              <ChevronRight aria-hidden className="size-4 shrink-0 text-muted-foreground" />
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** "Ana te deve R$ 162,30", "Você deve R$ 98,00 a Bruno" or "Carla deve R$ 30,00 a Bruno". */
function pairSentence(pair: JsPickPaymentPair, amount: string): string {
  const from = nameOrFormer(pair.from?.name)
  const to = nameOrFormer(pair.to?.name)
  if (pair.from?.isMe) return t('pickPayment_youOwe', { name: to, amount })
  if (pair.to?.isMe) return t('pickPayment_owesYou', { name: from, amount })
  return t('pickPayment_owes', { from, to, amount })
}
