import { ArrowLeft, Check, ChevronRight, CircleCheck, Copy, EllipsisVertical, Link2, Pencil, Share2, ShieldCheck, Trash2, UserPlus } from 'lucide-react'
import {
  MembersPresenter,
  type JsMemberNameEditor,
  type JsMembersAction,
  type JsMembersMember,
  type JsMembersState,
  type JsPersonalInvite,
  type JsRoute,
} from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, GroupTabs } from '@/components/common'
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
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

const nameId = 'member-name'
const personalLinkId = 'personal-link'

/**
 * "Membros", the group's tab beside Despesas and Saldos: who is in it and who joined, the invite link, and each
 * person's options (rename, a personal invite link, and removing someone who hasn't joined).
 */
export function MembersScreen({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new MembersPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsMembersState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: JsMembersAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy' && action.link) {
      navigator.clipboard.writeText(action.link).catch(() => toast(t('members_copyFailed')))
    } else if (action.type === 'share-personal' && action.link) {
      sharePersonalLink(action.link, action.memberName ?? '', action.groupName ?? '')
    } else if (action.type === 'change-error') {
      toast(t('members_changeError'))
    } else if (action.type === 'coming-soon') {
      toast(t('members_comingSoon'))
    }
  })
  if (!presenter || !state?.isLoaded) return null

  const count = t(state.memberCount === 1 ? 'members_countOne' : 'members_countOther', { count: state.memberCount })
  const joined = t(state.joinedCount === 1 ? 'members_joinedOne' : 'members_joinedOther', { count: state.joinedCount })
  return (
    <div className="min-h-dvh bg-muted/40">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        <h1 className="mr-auto truncate text-lg font-semibold">{t('members_title')}</h1>
        <Button onClick={() => presenter.add()}>
          <UserPlus />
          {t('members_add')}
        </Button>
      </header>
      <main className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-4 sm:p-8">
        <div>
          <h2 className="text-2xl font-bold">{state.groupName}</h2>
          <p className="text-sm text-muted-foreground">
            {count} · {joined}
          </p>
        </div>
        <GroupTabs
          selected="members"
          onSelect={(tab) => {
            if (tab === 'expenses') presenter.expensesTab()
            else if (tab === 'balances') presenter.balancesTab()
          }}
        />
        {state.joinedAs != null && <JoinedNotice presenter={presenter} name={state.joinedAs} showsProtectAccess={state.showsProtectAccess} />}
        <section className="flex items-center gap-3 rounded-xl border bg-card p-3 pl-4">
          <Link2 className="size-5 shrink-0 text-muted-foreground" />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold">{t('members_linkTitle')}</p>
            <p className="truncate font-mono text-xs text-muted-foreground">{state.displayLink ?? t('members_linkPending')}</p>
          </div>
          <Button variant="secondary" onClick={() => presenter.share()}>
            {t('members_share')}
          </Button>
        </section>
        <ul className="flex flex-col divide-y rounded-xl border bg-card" aria-label={t('members_peopleLabel')}>
          {state.members.map((member) => (
            <MemberRow key={member.id} presenter={presenter} member={member} />
          ))}
        </ul>
      </main>
      {state.editor && <MemberNameDialog presenter={presenter} editor={state.editor} />}
      {state.memberToRemoveName != null && <RemoveMemberDialog presenter={presenter} name={state.memberToRemoveName} />}
      {state.personalInvite && <PersonalInviteDialog presenter={presenter} invite={state.personalInvite} groupName={state.groupName} />}
    </div>
  )
}

/** Right after joining through the link, and "Proteja seu acesso" while the account has no Google or Apple. */
function JoinedNotice({ presenter, name, showsProtectAccess }: { presenter: MembersPresenter; name: string; showsProtectAccess: boolean }) {
  return (
    <>
      <section className="flex gap-3 rounded-xl border border-primary/30 bg-primary/10 p-4">
        <CircleCheck className="mt-0.5 size-5 shrink-0 text-primary" />
        <div>
          <p className="font-semibold">{t('members_joinedTitle', { name })}</p>
          <p className="text-sm text-muted-foreground">{t('members_joinedMessage')}</p>
        </div>
      </section>
      {showsProtectAccess && (
        <button type="button" onClick={() => presenter.protectAccess()} className="flex items-center gap-3 rounded-xl border bg-card p-4 text-left hover:bg-muted/60">
          <ShieldCheck className="size-5 shrink-0 text-primary" />
          <span className="flex-1">
            <span className="block font-semibold">{t('members_protectTitle')}</span>
            <span className="block text-sm text-muted-foreground">{t('members_protectMessage')}</span>
          </span>
          <ChevronRight className="size-5 text-muted-foreground" />
        </button>
      )}
    </>
  )
}

function MemberRow({ presenter, member }: { presenter: MembersPresenter; member: JsMembersMember }) {
  return (
    <li className="flex items-center gap-3 py-2.5 pr-2 pl-4">
      <Avatar initial={member.initials} color={member.color} className="size-10 text-sm" />
      <div className="min-w-0 flex-1">
        <p className="truncate font-medium">
          {member.name}
          {member.isMe && <span className="text-muted-foreground"> · {t('members_me')}</span>}
        </p>
        <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <span aria-hidden className={cn('size-2 rounded-full', member.hasJoined ? 'bg-primary' : 'border-[1.5px] border-muted-foreground')} />
          {member.isMe ? t('members_statusThisBrowser') : member.hasJoined ? t('members_statusJoined') : t('members_statusNotJoined')}
        </p>
      </div>
      {member.hasJoined && <Check aria-hidden className="size-4 text-primary" />}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" aria-label={t('members_options', { name: member.name })}>
            <EllipsisVertical />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-64">
          <DropdownMenuItem onSelect={() => presenter.rename(member.id)}>
            <Pencil />
            {t('members_rename')}
          </DropdownMenuItem>
          {member.canBeInvited && (
            <DropdownMenuItem disabled={!member.isPersonalInviteReady} onSelect={() => presenter.personalInvite(member.id)}>
              <Link2 />
              {t('members_personalInvite', { name: member.name })}
            </DropdownMenuItem>
          )}
          {!member.isMe && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" disabled={!member.canBeRemoved} onSelect={() => presenter.remove(member.id)} className="items-start">
                <Trash2 className="mt-0.5" />
                <span className="flex flex-col">
                  {t('members_remove')}
                  {member.hasJoined && <span className="text-xs text-muted-foreground">{t('members_removeJoined', { name: member.name })}</span>}
                </span>
              </DropdownMenuItem>
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    </li>
  )
}

/**
 * "Convite pessoal para Carla": a link that joins straight in as that member, a dialog on desktop and tablet and a
 * drawer on the phone, like "Convidar". Closing it any way is the ViewModel's dismissal.
 */
function PersonalInviteDialog({ presenter, invite, groupName }: { presenter: MembersPresenter; invite: JsPersonalInvite; groupName: string }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.dismissPersonalInvite()
  }
  const title = t('members_personalInvite', { name: invite.memberName })
  const description = t('members_personalMessage', { name: invite.memberName })
  const heading = (
    <div className="flex items-center gap-3 text-left">
      <Avatar initial={invite.memberInitials} color={invite.memberColor} className="size-10 shrink-0 text-sm" />
      <div className="min-w-0">
        {isDesktop ? <DialogTitle className="truncate">{title}</DialogTitle> : <DrawerTitle className="truncate">{title}</DrawerTitle>}
        <p className="truncate text-sm text-muted-foreground">{groupName}</p>
      </div>
    </div>
  )
  const body = (
    <div className="flex flex-col gap-2">
      <Label htmlFor={personalLinkId}>{t('members_personalLinkLabel')}</Label>
      <div className="flex gap-2">
        <Input id={personalLinkId} readOnly value={invite.displayLink} onFocus={(event) => event.currentTarget.select()} className="font-mono" />
        <Button variant="secondary" onClick={() => presenter.copyPersonalLink()} aria-live="polite" className="shrink-0">
          {invite.isLinkCopied ? <Check /> : <Copy />}
          {invite.isLinkCopied ? t('members_copied') : t('members_copy')}
        </Button>
      </div>
    </div>
  )
  const close = (
    <Button variant="ghost" onClick={() => presenter.dismissPersonalInvite()} className={cn(!isDesktop && 'h-11')}>
      {t('members_close')}
    </Button>
  )
  const share = (
    <Button onClick={() => presenter.sharePersonalLink()} className={cn(!isDesktop && 'h-11')}>
      <Share2 />
      {t('members_personalShare', { name: invite.memberName })}
    </Button>
  )

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[460px]">
        <DialogHeader className="gap-4">
          {heading}
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {body}
        <DialogFooter>
          {close}
          {share}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent>
        <DrawerHeader className="gap-4 group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          {heading}
          <DrawerDescription>{description}</DrawerDescription>
        </DrawerHeader>
        <div className="px-4">{body}</div>
        <DrawerFooter>
          {share}
          {close}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

/** "Remover do grupo" asks first, since a removal reaches every device on the next sync. */
function RemoveMemberDialog({ presenter, name }: { presenter: MembersPresenter; name: string }) {
  return (
    <AlertDialog
      open
      onOpenChange={(open) => {
        if (!open) presenter.dismissRemove()
      }}
    >
      <AlertDialogContent className="sm:max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>{t('members_removeTitle', { name })}</AlertDialogTitle>
          <AlertDialogDescription>{t('members_removeMessage', { name })}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>{t('members_cancel')}</AlertDialogCancel>
          {/* Not an AlertDialogAction, which would close it through onOpenChange as a dismissal */}
          <Button variant="destructive" onClick={() => presenter.confirmRemove()}>
            {t('members_removeConfirm')}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

/** "Adicionar pessoa" and "Renomear" ask the same thing: a name free in the group, with nicknames when it isn't. */
function MemberNameDialog({ presenter, editor }: { presenter: MembersPresenter; editor: JsMemberNameEditor }) {
  // Why: the ViewModel caps the draft as it handles it, so the field reads the capped text back.
  const [draft, setDraft] = useDraft(editor.draft, (value) => {
    presenter.changeDraft(value)
    return presenter.getDraft()
  })
  const title = editor.isAdding ? t('members_add') : t('members_rename')
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) presenter.dismissEditor()
      }}
    >
      <DialogContent className="sm:max-w-[420px]">
        <form
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            event.preventDefault()
            presenter.save()
          }}
        >
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription className="sr-only">{title}</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor={nameId}>{t('members_nameLabel')}</Label>
            <Input
              id={nameId}
              autoFocus
              autoComplete="off"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              aria-invalid={editor.takenName != null}
              aria-describedby={editor.takenName != null ? `${nameId}-taken` : undefined}
            />
            {editor.takenName != null && (
              <>
                <p id={`${nameId}-taken`} className="text-sm text-destructive">
                  {t('members_nameTaken', { name: editor.takenName })}
                </p>
                <div className="flex flex-wrap gap-2">
                  {editor.nicknameSuggestions.map((suggestion) => (
                    <Button key={suggestion} type="button" variant="outline" size="sm" onClick={() => presenter.useSuggestion(suggestion)}>
                      {t('members_useNickname', { name: suggestion })}
                    </Button>
                  ))}
                </div>
              </>
            )}
          </div>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => presenter.dismissEditor()}>
              {t('members_cancel')}
            </Button>
            <Button type="submit" disabled={!editor.canSave}>
              {editor.isAdding ? t('members_add') : t('members_save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// Why: the browser's share sheet where there is one (phones, Safari), otherwise the link goes to the clipboard, as in "Convidar".
function sharePersonalLink(link: string, memberName: string, groupName: string) {
  const text = t('members_personalShareMessage', { group: groupName, name: memberName, link })
  if (typeof navigator.share === 'function') {
    navigator.share({ text }).catch(() => {})
    return
  }
  navigator.clipboard
    .writeText(link)
    .then(() => toast(t('members_linkCopied')))
    .catch(() => toast(t('members_copyFailed')))
}
