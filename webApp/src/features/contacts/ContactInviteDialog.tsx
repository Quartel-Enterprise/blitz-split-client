import { Check, Copy, Loader2, Plus, Share2, WifiOff } from 'lucide-react'
import { ContactInvitePresenter, type JsContactInviteAction, type JsContactInviteState, type JsRoute } from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { QrCode } from '@/features/invite/InviteDialog'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { copyLink, shareOrCopy } from '@/lib/share'
import { cn } from '@/lib/utils'

const linkId = 'contact-invite-link'

/**
 * "Convidar com link ou QR" (or "Convidar Diego", a guest's personal link) from Contatos: an invite is always to a
 * group, so it asks which, then shows that group's QR code and link. A 480 px dialog, a drawer on the phone. It works
 * offline once the group has its link.
 */
export function ContactInviteDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new ContactInvitePresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsContactInviteState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsContactInviteAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy') {
      copyLink(action.link)
      return
    }
    const group = action.groupName ?? ''
    const text = action.name
      ? t('contactInvite_shareNamed', { name: action.name, group, link: action.link })
      : t('invite_shareMessage', { group, link: action.link })
    shareOrCopy(text, action.link)
  })
  if (!presenter || !state?.isLoaded) return null

  const hasLink = !state.isLinkPending && state.link != null
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      className="sm:max-w-[480px]"
      onOpenChange={(open) => {
        if (!open) presenter.close()
      }}
      pane={{
        title: state.name ? t('contactProfile_invite', { name: state.name }) : t('contacts_inviteWithLink'),
        description: t('contactInvite_pick'),
        body: <InviteBody presenter={presenter} state={state} isDesktop={isDesktop} />,
        footer: (
          <>
            <Button variant="outline" disabled={!hasLink} onClick={() => presenter.copy()} aria-live="polite">
              {state.isLinkCopied ? <Check /> : <Copy />}
              {state.isLinkCopied ? t('invite_copied') : t('contactProfile_copyLink')}
            </Button>
            <Button disabled={!hasLink} onClick={() => presenter.share()}>
              <Share2 />
              {t('contactProfile_share')}
            </Button>
          </>
        ),
      }}
    />
  )
}

function InviteBody({ presenter, state, isDesktop }: { presenter: ContactInvitePresenter; state: JsContactInviteState; isDesktop: boolean }) {
  const sub = state.isPersonal
    ? t('contactInvite_subPersonal', { name: state.name ?? '' })
    : state.name
      ? t('contactInvite_subNamed', { name: state.name })
      : t('contactInvite_sub')
  return (
    <div className="flex flex-col gap-4">
      <div role="radiogroup" aria-label={t('contactInvite_groupLabel')} className="flex flex-wrap gap-2">
        {state.groups.map((group) => {
          const isChecked = group.groupId === state.selectedGroupId
          return (
            <button
              key={group.groupId}
              type="button"
              role="radio"
              aria-checked={isChecked}
              onClick={() => presenter.pickGroup(group.groupId)}
              className={cn(
                'flex h-8 items-center gap-1.5 rounded-md border bg-card px-3 text-[13px] font-medium whitespace-nowrap outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50',
                isChecked && 'border-primary pl-2 ring-1 ring-primary',
              )}
            >
              {isChecked && <Check aria-hidden className="size-3.5 text-primary" />}
              {group.name}
            </button>
          )
        })}
        {/* Why: a guest's personal link only exists in the groups they are in. */}
        {!state.isPersonal && (
          <button
            type="button"
            onClick={() => presenter.newGroup()}
            className="flex h-8 items-center gap-1.5 rounded-md border border-dashed pr-3 pl-2 text-[13px] font-medium whitespace-nowrap text-muted-foreground outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50"
          >
            <Plus aria-hidden className="size-3.5" />
            {t('contactInvite_newGroup')}
          </button>
        )}
      </div>
      {state.hasNoGroups && <p className="text-sm text-muted-foreground">{t('contactInvite_noGroups')}</p>}
      {state.selectedGroupName != null && (
        <>
          <div className="flex items-center gap-4">
            {state.isLinkPending ? (
              <span
                aria-hidden
                className={cn('flex shrink-0 items-center justify-center rounded-xl border border-dashed text-muted-foreground', isDesktop ? 'size-34' : 'size-28')}
              >
                <Loader2 className="size-5 animate-spin" />
              </span>
            ) : (
              <QrCode size={state.qrSize} modules={state.qrModules} className={cn('shrink-0 border', isDesktop ? 'size-34' : 'size-28')} />
            )}
            <span className="flex min-w-0 flex-1 flex-col gap-1.5">
              <span className="text-sm font-semibold">{state.selectedGroupName}</span>
              <span className="text-[13px] text-muted-foreground">{sub}</span>
              {state.showsOfflineNotice && (
                <span className="flex items-start gap-1.5 text-xs text-muted-foreground">
                  <WifiOff aria-hidden className="mt-0.5 size-3 shrink-0" />
                  {t('contactInvite_offline')}
                </span>
              )}
            </span>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor={state.isLinkPending ? undefined : linkId}>{t('contactInvite_linkLabel')}</Label>
            {state.isLinkPending ? (
              <p role="status" className="flex h-9 items-center rounded-md border border-dashed px-3 text-[13px] text-muted-foreground">
                {t('contactInvite_pending')}
              </p>
            ) : (
              <Input
                id={linkId}
                readOnly
                value={state.displayLink ?? ''}
                onFocus={(event) => event.currentTarget.select()}
                className="bg-muted font-mono text-[13px]"
              />
            )}
          </div>
        </>
      )}
    </div>
  )
}
