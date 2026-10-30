import { Link2Off, Loader2, WifiOff, X } from 'lucide-react'
import { JoinInvitePresenter, type JsJoinInviteState, type JsJoinMember, type JsRoute } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { locale, t } from '@/i18n'

const shownFaces = 3

/**
 * "Convite": a group's invite link opened, over the home or the welcome. Offline it keeps the link and opens the
 * invite by itself once the browser is online.
 */
export function JoinInviteScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new JoinInvitePresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsJoinInviteState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return (
    <div className="min-h-dvh bg-muted/40">
      <header className="flex h-14 items-center px-2 sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('join_close')} onClick={() => presenter.notNow()}>
          <X />
        </Button>
      </header>
      <main className="mx-auto flex w-full max-w-lg flex-col gap-6 px-4 pb-8 sm:px-8">
        {state.status === 'loading' && <Loader2 aria-hidden className="mt-40 size-8 animate-spin self-center text-muted-foreground" />}
        {state.status === 'ready' && <Invite presenter={presenter} state={state} />}
        {state.status === 'invalid' && (
          <Problem icon={<Link2Off className="size-10" />} title={t('join_invalidTitle')} message={t('join_invalidMessage')}>
            <Button variant="outline" size="lg" onClick={() => presenter.goHome()}>
              {t('join_goHome')}
            </Button>
          </Problem>
        )}
        {state.status === 'offline' && (
          <Problem icon={<WifiOff className="size-10" />} title={t('join_offlineTitle')} message={t('join_offlineMessage')}>
            <p className="self-center rounded-md bg-card px-3 py-2 font-mono text-sm">{state.displayLink}</p>
            <Button size="lg" onClick={() => presenter.retry()}>
              {t('join_retry')}
            </Button>
            <Button variant="outline" size="lg" onClick={() => presenter.goHome()}>
              {t('join_goHome')}
            </Button>
          </Problem>
        )}
      </main>
    </div>
  )
}

function Invite({ presenter, state }: { presenter: JoinInvitePresenter; state: JsJoinInviteState }) {
  const count = t(state.memberCount === 1 ? 'join_countOne' : 'join_countOther', { count: state.memberCount })
  return (
    <>
      <span className="self-start rounded-full bg-secondary px-3 py-1 text-sm font-medium">{t('join_chip')}</span>
      <h1 className="text-3xl font-bold">
        {state.creatorName != null ? t('join_title', { creator: state.creatorName, group: state.groupName }) : t('join_titleNoCreator', { group: state.groupName })}
      </h1>
      <section className="flex items-center gap-4 rounded-2xl border bg-card p-5">
        <GroupAvatar initials={state.groupInitials} color={state.groupColor} className="size-14 text-lg" />
        <div className="min-w-0">
          <p className="truncate text-lg font-semibold">{state.groupName}</p>
          <p className="text-sm text-muted-foreground">{state.creatorName != null ? `${count} · ${t('join_createdBy', { name: state.creatorName })}` : count}</p>
        </div>
      </section>
      {state.joinedNames.length > 0 && (
        <div className="flex items-center gap-3">
          <Faces members={state.joined} />
          <p className="text-sm">{t(state.joinedNames.length === 1 ? 'join_alreadyInOne' : 'join_alreadyInOther', { names: new Intl.ListFormat(locale, { type: 'conjunction' }).format(state.joinedNames) })}</p>
        </div>
      )}
      <p className="text-sm text-muted-foreground">{t('join_hint')}</p>
      <div className="flex flex-col gap-2">
        <Button size="lg" onClick={() => presenter.join()}>
          {t('join_join')}
        </Button>
        <Button variant="ghost" size="lg" onClick={() => presenter.notNow()}>
          {t('join_notNow')}
        </Button>
      </div>
    </>
  )
}

/** The faces of the first who joined, and how many more there are. */
function Faces({ members }: { members: JsJoinMember[] }) {
  const more = members.length - shownFaces
  return (
    <div aria-hidden className="flex -space-x-2">
      {members.slice(0, shownFaces).map((member) => (
        <Avatar key={member.id} initial={member.initials} color={member.color} className="size-9 text-xs ring-2 ring-background" />
      ))}
      {more > 0 && <span className="flex size-9 items-center justify-center rounded-full bg-muted text-xs font-semibold ring-2 ring-background">+{more}</span>}
    </div>
  )
}

function Problem({ icon, title, message, children }: { icon: ReactNode; title: string; message: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-4 pt-12 text-center">
      <div className="self-center text-primary">{icon}</div>
      <h1 className="text-2xl font-bold">{title}</h1>
      <p className="text-muted-foreground">{message}</p>
      <div className="mt-2 flex flex-col gap-2">{children}</div>
    </section>
  )
}
