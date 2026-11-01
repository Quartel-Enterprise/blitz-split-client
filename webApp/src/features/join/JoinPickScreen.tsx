import { ArrowLeft, CircleAlert, Loader2, UserPlus } from 'lucide-react'
import { JoinPickPresenter, type JsJoinMember, type JsJoinPickState, type JsNewMemberDraft, type JsRoute } from 'blitzsplit-shared'
import type { FormEvent } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

const nameId = 'new-member-name'

/**
 * "Quem é você no grupo?": the members nobody joined as yet to pick from, the ones who joined, and "Não estou na
 * lista", which joins under a new name. The invite's summary heads it; from 1024 px it sits beside the card in a
 * 1120 px container, and on the phone "Entrar como Bruno" stays pinned at the bottom (3.2).
 */
export function JoinPickScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new JoinPickPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsJoinPickState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), () => {
    toast(t('join_pickError'))
  })
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state) return null
  const joinButton = (
    <Button size="lg" className="w-full" disabled={!state.canJoin} onClick={() => presenter.join()}>
      {state.isJoining && <Loader2 className="animate-spin" />}
      {state.selectedName != null ? t('join_pickJoinAs', { name: state.selectedName }) : t('join_pickChoose')}
    </Button>
  )
  return (
    <div className="flex min-h-dvh flex-col bg-muted/40">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
      </header>
      {!state.isLoaded ? (
        <Loader2 aria-hidden className="m-auto size-8 animate-spin text-muted-foreground" />
      ) : (
        <>
          <main className="mx-auto flex w-full max-w-[560px] flex-1 flex-col gap-5 px-4 pt-5 pb-8 md:gap-6 md:px-6 md:pt-6 lg:max-w-[1120px] lg:flex-row lg:items-start lg:gap-8">
            <section className="flex flex-col gap-3 lg:max-w-[440px] lg:flex-1 lg:basis-80 lg:gap-4 lg:pt-6">
              {state.creatorName != null && (
                <span className="w-fit rounded-md bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">{t('join_pickSummaryChip', { name: state.creatorName })}</span>
              )}
              <h1 className="text-[1.75rem] leading-tight font-bold tracking-tight md:text-4xl">{t('join_pickSummaryTitle', { group: state.groupName })}</h1>
              <p className="text-muted-foreground md:text-base">{t('join_pickSummaryBody')}</p>
            </section>
            <section aria-labelledby="pick-title" className="flex flex-col gap-4 md:rounded-xl md:border md:bg-card md:p-6 md:shadow-sm lg:max-w-[520px] lg:flex-1 lg:basis-90">
              <div>
                <h2 id="pick-title" className="text-lg font-semibold">
                  {t('join_pickTitle')}
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">{t('join_pickSubtitle')}</p>
              </div>
              {state.takenName != null && (
                <div role="alert" className="flex gap-3 rounded-xl border border-destructive/30 bg-destructive/10 p-4">
                  <CircleAlert className="mt-0.5 size-5 shrink-0 text-destructive" />
                  <div>
                    <p className="font-semibold">{t('join_pickTakenTitle', { name: state.takenName })}</p>
                    <p className="text-sm">{state.creatorName != null ? t('join_pickTakenMessage', { name: state.creatorName }) : t('join_pickTakenMessageNoCreator')}</p>
                  </div>
                </div>
              )}
              {state.notJoined.length > 0 && (
                <section>
                  <h3 className="mb-2 text-sm font-semibold text-primary">{t('join_pickNotJoined')}</h3>
                  <div role="radiogroup" aria-label={t('join_pickNotJoined')} className="flex flex-col divide-y rounded-xl border bg-card">
                    {state.notJoined.map((member) => (
                      <MemberOption
                        key={member.id}
                        member={member}
                        isSelected={member.id === state.selectedMemberId}
                        isEnabled={!state.isJoining}
                        onSelect={() => presenter.select(member.id)}
                      />
                    ))}
                  </div>
                </section>
              )}
              {state.joined.length > 0 && (
                <section>
                  <h3 className="mb-2 text-sm font-semibold text-primary">{t('join_pickJoined')}</h3>
                  <ul className="flex flex-col divide-y rounded-xl border bg-card">
                    {state.joined.map((member) => (
                      <li key={member.id} className="flex items-center gap-3 px-4 py-2.5 text-muted-foreground">
                        <Avatar initial={member.initials} color={member.color} className="size-10 text-sm" />
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium">{member.name}</p>
                          <p className="text-xs">{t('join_pickStatusJoined')}</p>
                        </div>
                      </li>
                    ))}
                  </ul>
                </section>
              )}
              <Button variant="link" className="h-8 self-start px-1 underline underline-offset-[3px]" disabled={state.isJoining} onClick={() => presenter.notInList()}>
                <UserPlus />
                {t('join_pickNotInList')}
              </Button>
              {isDesktop && joinButton}
              <p className="text-xs text-muted-foreground">{t('join_pickFootnote')}</p>
            </section>
          </main>
          {!isDesktop && <footer className="sticky bottom-0 border-t bg-card p-4">{joinButton}</footer>}
        </>
      )}
      {state.newMember && <NewMemberDialog presenter={presenter} draft={state.newMember} canJoin={state.canJoinAsNewMember} isDesktop={isDesktop} />}
    </div>
  )
}

function MemberOption({ member, isSelected, isEnabled, onSelect }: { member: JsJoinMember; isSelected: boolean; isEnabled: boolean; onSelect: () => void }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={isSelected}
      disabled={!isEnabled}
      onClick={onSelect}
      className="flex items-center gap-3 px-4 py-2.5 text-left first:rounded-t-xl last:rounded-b-xl hover:bg-muted/60 disabled:opacity-60"
    >
      <Avatar initial={member.initials} color={member.color} className="size-10 text-sm" />
      <span className="min-w-0 flex-1 truncate font-medium">{member.name}</span>
      <span aria-hidden className={cn('flex size-5 items-center justify-center rounded-full border-2', isSelected ? 'border-primary' : 'border-muted-foreground')}>
        {isSelected && <span className="size-2.5 rounded-full bg-primary" />}
      </span>
    </button>
  )
}

/** "Entrar com outro nome": someone new to the group, under a name nobody in it has. A dialog, a drawer on the phone. */
function NewMemberDialog({ presenter, draft, canJoin, isDesktop }: { presenter: JoinPickPresenter; draft: JsNewMemberDraft; canJoin: boolean; isDesktop: boolean }) {
  // Why: the ViewModel caps the name as it handles it, so the field reads the capped text back.
  const [name, setName] = useDraft(draft.name, (value) => {
    presenter.changeNewName(value)
    return presenter.getNewName()
  })
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.dismissNewMember()
  }
  const field = (
    <div className="flex flex-col gap-2">
      <Label htmlFor={nameId}>{t('join_pickNewName')}</Label>
      <Input
        id={nameId}
        autoFocus
        autoComplete="name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        aria-invalid={draft.isNameTaken}
        aria-describedby={`${nameId}-helper`}
      />
      <p id={`${nameId}-helper`} className={cn('text-sm', draft.isNameTaken ? 'text-destructive' : 'text-muted-foreground')}>
        {t('join_pickNewHelper')}
      </p>
    </div>
  )
  const cancel = (
    <Button type="button" variant={isDesktop ? 'ghost' : 'outline'} className={cn(!isDesktop && 'h-11')} onClick={() => presenter.dismissNewMember()}>
      {t('common_cancel')}
    </Button>
  )
  const join = (
    <Button type="submit" className={cn(!isDesktop && 'h-11')} disabled={!canJoin}>
      {t('join_join')}
    </Button>
  )
  const onSubmit = (event: FormEvent) => {
    event.preventDefault()
    presenter.joinAsNewMember()
  }
  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[440px]">
        <form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <DialogHeader>
            <DialogTitle>{t('join_pickNewTitle')}</DialogTitle>
            <DialogDescription>{t('join_pickNewMessage')}</DialogDescription>
          </DialogHeader>
          {field}
          <DialogFooter>
            {cancel}
            {join}
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent>
        <form className="flex flex-col" onSubmit={onSubmit}>
          <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
            <DrawerTitle>{t('join_pickNewTitle')}</DrawerTitle>
            <DrawerDescription>{t('join_pickNewMessage')}</DrawerDescription>
          </DrawerHeader>
          <div className="px-4">{field}</div>
          <DrawerFooter>
            {join}
            {cancel}
          </DrawerFooter>
        </form>
      </DrawerContent>
    </Drawer>
  )
}
