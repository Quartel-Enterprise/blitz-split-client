import { ArrowLeft, CloudOff, Link2Off, Loader2, UserPlus } from 'lucide-react'
import { ProfileLinkPresenter, type JsProfileLinkState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, PageProblem } from '@/components/common'
import { Button } from '@/components/ui/button'
import { t } from '@/i18n'

/**
 * A profile link opened, blitzsplit.com/u/<code>: a page of its own, outside the home's frame, in a centered column,
 * while it asks the server or when it has no contact to open. The person's own link opens Profile, and a contact's
 * (someone they share a group with) the contact's profile, in its place.
 */
export function ProfileLinkScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new ProfileLinkPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsProfileLinkState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return (
    <div className="min-h-dvh bg-background">
      <header className="sticky top-0 z-10 border-b bg-background/95 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-[720px] items-center gap-2 px-2">
          <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
            <ArrowLeft />
          </Button>
        </div>
      </header>
      <main className="mx-auto w-full max-w-md px-4 pb-12">
        {state.stage === 'loading' && (
          <div className="flex justify-center pt-16">
            <Loader2 aria-label={t('profileLink_loading')} className="size-8 animate-spin text-muted-foreground" />
          </div>
        )}
        {state.stage === 'stranger' && <Stranger presenter={presenter} state={state} />}
        {state.stage === 'unreachable' && (
          <PageProblem icon={<CloudOff className="size-10" />} title={t('profileLink_unreachableTitle')} message={t('profileLink_unreachableBody')}>
            <Button size="lg" onClick={() => presenter.retry()}>
              {t('profileLink_retry')}
            </Button>
          </PageProblem>
        )}
        {state.stage === 'not-found' && (
          <PageProblem icon={<Link2Off className="size-10" />} title={t('profileLink_notFoundTitle')} message={t('profileLink_notFoundBody')} />
        )}
      </main>
    </div>
  )
}

function Stranger({ presenter, state }: { presenter: ProfileLinkPresenter; state: JsProfileLinkState }) {
  const name = state.name ?? ''
  return (
    <section className="flex flex-col items-center gap-3 pt-16 text-center">
      <Avatar initial={state.initials ?? ''} color={state.avatarColor ?? 'blue'} photoUrl={state.photoUrl} className="size-28 text-4xl" />
      <h1 className="mt-2 text-2xl font-semibold">{name}</h1>
      <p className="text-muted-foreground">{t('profileLink_noGroups', { name })}</p>
      {state.canInvite && (
        <Button size="lg" variant="secondary" className="mt-3" onClick={() => presenter.invite()}>
          <UserPlus /> {t('profileLink_invite')}
        </Button>
      )}
    </section>
  )
}
