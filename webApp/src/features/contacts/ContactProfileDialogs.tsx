import { AlertCircle, Copy, Loader2, Share2, WifiOff } from 'lucide-react'
import type { ContactProfilePresenter, JsAddToGroup, JsContact, JsGuestMergeSheet } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

const mergeLinkId = 'merge-link'

/**
 * "Adicionar Carla a um grupo": the user's groups, those the person is already in disabled, one marked. A dialog on
 * desktop and tablet, a drawer on the phone; closing it any way is the ViewModel's dismiss.
 */
export function AddToGroupDialog({ presenter, draft, contact }: { presenter: ContactProfilePresenter; draft: JsAddToGroup; contact: JsContact }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const name = contact.name
  const title = t('contactProfile_addToGroupPerson', { name })
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[480px]"
      onOpenChange={(open) => {
        if (!open) presenter.dismissAddToGroup()
      }}
      pane={{
        title,
        body: (
          <div className="flex flex-col gap-3">
            <div role="radiogroup" aria-label={title} className="flex flex-col divide-y overflow-hidden rounded-lg border">
              {draft.options.map((option) => {
                const isChecked = option.groupId === draft.selectedGroupId
                return (
                  <button
                    key={option.groupId}
                    type="button"
                    role="radio"
                    aria-checked={isChecked}
                    disabled={option.isAlreadyIn}
                    onClick={() => presenter.pickAddGroup(option.groupId)}
                    className="flex items-center gap-3 px-3 py-2.5 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:ring-inset disabled:pointer-events-none disabled:opacity-60"
                  >
                    <GroupAvatar initials={option.initials} color={option.color} className="size-8 text-xs" />
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate text-sm font-medium">{option.name}</span>
                      <span className="text-xs text-muted-foreground">
                        {option.isAlreadyIn
                          ? t('contactProfile_alreadyIn')
                          : t(option.memberCount === 1 ? 'members_countOne' : 'members_countOther', { count: option.memberCount })}
                      </span>
                    </span>
                    <RadioMark isChecked={isChecked} />
                  </button>
                )
              })}
            </div>
            <p className="text-[13px] text-muted-foreground">{t(contact.isGuest ? 'contactProfile_addNoteGuest' : 'contactProfile_addNote', { name })}</p>
          </div>
        ),
        footer: (
          <>
            <Button variant="outline" onClick={() => presenter.dismissAddToGroup()}>
              {t('common_cancel')}
            </Button>
            <Button disabled={!draft.canAdd} onClick={() => presenter.confirmAddToGroup()}>
              {t('contactProfile_add')}
            </Button>
          </>
        ),
      }}
    />
  )
}

/**
 * "Juntar Diego a uma conta": the guest's personal link, made by the server as it opens (online only), to copy or
 * share; "Pedir para quem já usa o app" isn't built yet.
 */
export function MergeDialog({
  presenter,
  sheet,
  contact,
  isOnline,
}: {
  presenter: ContactProfilePresenter
  sheet: JsGuestMergeSheet
  contact: JsContact
  isOnline: boolean
}) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const name = contact.name
  const link = sheet.displayLink
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[480px]"
      onOpenChange={(open) => {
        if (!open) presenter.dismissMerge()
      }}
      pane={{
        title: t('contactProfile_mergeTitle', { name }),
        description: t('contactProfile_mergeBody', { name, amount: formatSignedMoney(contact.netMinor, contact.currency, contact.fractionDigits) }),
        body: (
          <div className="flex flex-col gap-4">
            <div role="radiogroup" aria-label={t('contactProfile_mergeHow', { name })} className="flex flex-col gap-2">
              <MergeOption isChecked title={t('contactProfile_mergePersonal')} body={t('contactProfile_mergePersonalSub', { name })} />
              <MergeOption
                isChecked={false}
                title={t('contactProfile_mergeAsk')}
                body={t('contactProfile_mergeAskSub')}
                onClick={() => presenter.askAppUser()}
              />
            </div>
            <div className="flex flex-col gap-2">
              <span id={mergeLinkId} className="text-sm font-medium">
                {t('contactProfile_mergeLink')}
              </span>
              <MergeLink sheet={sheet} isOnline={isOnline} onRetry={() => presenter.retryMerge()} />
            </div>
          </div>
        ),
        footer: (
          <>
            <Button variant="outline" disabled={!link} onClick={() => presenter.copyMerge()}>
              <Copy />
              {t('contactProfile_copyLink')}
            </Button>
            <Button disabled={!link} onClick={() => presenter.shareMerge()}>
              <Share2 />
              {t('contactProfile_share')}
            </Button>
          </>
        ),
      }}
    />
  )
}

function MergeLink({ sheet, isOnline, onRetry }: { sheet: JsGuestMergeSheet; isOnline: boolean; onRetry: () => void }) {
  const field = (content: ReactNode, isMono = false) => (
    <span
      aria-labelledby={mergeLinkId}
      className={cn('flex h-9 items-center gap-2 rounded-md border bg-muted px-3 text-[13px]', isMono ? 'font-mono' : 'text-muted-foreground')}
    >
      {content}
    </span>
  )
  if (sheet.displayLink) return field(sheet.displayLink, true)
  if (sheet.hasFailed) {
    return (
      <div role="alert" className="flex flex-col items-start gap-2 text-[13px] text-destructive">
        <span className="flex items-center gap-1.5">
          <AlertCircle className="size-4" />
          {t('contactProfile_mergeFailed')}
        </span>
        <Button variant="outline" size="sm" onClick={onRetry}>
          {t('invite_retry')}
        </Button>
      </div>
    )
  }
  if (!isOnline) {
    return (
      <>
        {field(t('contactProfile_mergeOfflineLink'))}
        <p role="status" className="flex items-start gap-1.5 text-[13px] text-muted-foreground">
          <WifiOff aria-hidden className="mt-0.5 size-3.5 shrink-0" />
          {t('contactProfile_mergeOffline')}
        </p>
      </>
    )
  }
  return field(
    <>
      <Loader2 aria-hidden className="size-3.5 animate-spin" />
      <span role="status">{t('contactProfile_mergeRequesting')}</span>
    </>,
  )
}

function MergeOption({ isChecked, title, body, onClick }: { isChecked: boolean; title: string; body: string; onClick?: () => void }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isChecked}
      onClick={onClick}
      className={cn(
        'flex items-start gap-3 rounded-lg border px-3.5 py-3 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
        isChecked ? 'border-primary ring-1 ring-primary' : 'hover:bg-accent/50',
      )}
    >
      <span className="mt-0.5">
        <RadioMark isChecked={isChecked} />
      </span>
      <span className="flex flex-1 flex-col gap-0.5">
        <span className="text-sm font-medium">{title}</span>
        <span className="text-[13px] text-muted-foreground">{body}</span>
      </span>
    </button>
  )
}

function RadioMark({ isChecked }: { isChecked: boolean }) {
  return (
    <span aria-hidden className={cn('flex size-4 shrink-0 items-center justify-center rounded-full border', isChecked ? 'border-primary' : 'border-muted-foreground')}>
      {isChecked && <span className="size-2 rounded-full bg-primary" />}
    </span>
  )
}
