import { Check, ChevronDown, CircleAlert, Loader2, Plus, X } from 'lucide-react'
import { CreateGroupPresenter, type JsCreateGroupState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { currencyLabel } from '@/lib/currency'
import { cn } from '@/lib/utils'

const formId = 'create-group-form'
const nameId = 'create-group-name'
const currencyId = 'create-group-currency'
const typeLabelId = 'create-group-type'

const typeLabels: Record<string, MessageKey> = {
  trip: 'createGroup_typeTrip',
  home: 'createGroup_typeHome',
  event: 'createGroup_typeEvent',
  other: 'createGroup_typeOther',
}

/**
 * "New group", over the home: a dialog on desktop and tablet, a drawer with stacked buttons on the phone.
 * Closing it any way (Esc, the overlay, Cancel) is the ViewModel's close.
 */
export function CreateGroupDialog() {
  const presenter = usePresenter(() => new CreateGroupPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsCreateGroupState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!presenter || !state) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.close()
  }
  const create = (
    <Button type="submit" form={formId} disabled={!state.canCreate} className={cn(!isDesktop && 'h-11')}>
      {state.isCreating && <Loader2 className="animate-spin" />}
      {t('createGroup_create')}
    </Button>
  )
  const cancel = (
    <Button
      type="button"
      variant="outline"
      disabled={state.isCreating}
      onClick={() => presenter.close()}
      className={cn(!isDesktop && 'h-11')}
    >
      {t('common_cancel')}
    </Button>
  )
  const form = <CreateGroupForm presenter={presenter} state={state} />

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent aria-describedby={undefined} className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle>{t('createGroup_title')}</DialogTitle>
        </DialogHeader>
        {form}
        <DialogFooter>
          {cancel}
          {create}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent aria-describedby={undefined} className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{t('createGroup_title')}</DrawerTitle>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{form}</div>
        <DrawerFooter>
          {create}
          {cancel}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

function CreateGroupForm({ presenter, state }: { presenter: CreateGroupPresenter; state: JsCreateGroupState }) {
  // Why: the ViewModel caps the name and the draft as it handles them, so the fields read the capped text back.
  const [name, setName] = useDraft(state.name, (value) => {
    presenter.changeName(value)
    return presenter.getName()
  })
  const [draft, setDraft] = useDraft(state.draft, (value) => {
    presenter.changeDraft(value)
    return presenter.getDraft()
  })
  const members = state.members.length
  return (
    <form
      id={formId}
      onSubmit={(event) => {
        event.preventDefault()
        presenter.create()
      }}
      className="flex flex-col gap-5"
    >
      {state.showsCreateError && (
        <p role="alert" className="flex items-center gap-1.5 text-sm text-destructive">
          <CircleAlert className="size-4 shrink-0" />
          {t('createGroup_saveError')}
        </p>
      )}
      <div className="flex items-end gap-4">
        <button
          type="button"
          aria-label={t('createGroup_changeColor')}
          onClick={() => presenter.changeColor()}
          className="rounded-2xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
        >
          <GroupAvatar initials={state.initials} color={state.color} className="size-16 text-xl" />
        </button>
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <Label htmlFor={nameId}>{t('createGroup_nameLabel')}</Label>
          <Input id={nameId} value={name} onChange={(event) => setName(event.target.value)} autoComplete="off" />
        </div>
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
        <div className="flex flex-1 flex-col gap-2">
          <span id={typeLabelId} className="text-sm font-medium">
            {t('createGroup_typeLabel')}
          </span>
          <div role="radiogroup" aria-labelledby={typeLabelId} className="flex flex-wrap gap-1.5">
            {state.types.map((type) => (
              <Button
                key={type}
                type="button"
                role="radio"
                aria-checked={type === state.type}
                size="sm"
                variant={type === state.type ? 'secondary' : 'outline'}
                onClick={() => presenter.selectType(type)}
              >
                {type === state.type && <Check />}
                {t(typeLabels[type] ?? 'createGroup_typeOther')}
              </Button>
            ))}
          </div>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor={currencyId}>{t('createGroup_currency')}</Label>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button id={currencyId} type="button" variant="outline" className="justify-between">
                {currencyLabel(state.currency)}
                <ChevronDown className="text-muted-foreground" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="max-h-72 overflow-y-auto">
              {state.currencies.map((currency) => (
                <DropdownMenuItem key={currency} onSelect={() => presenter.selectCurrency(currency)}>
                  <span className="flex-1">{currencyLabel(currency)}</span>
                  {currency === state.currency && <Check />}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      <section className="flex flex-col gap-2" aria-labelledby="create-group-members">
        <div className="flex items-baseline justify-between text-sm">
          <h3 id="create-group-members" className="font-medium">
            {t('createGroup_membersTitle')}
          </h3>
          <span className="text-muted-foreground">
            {t(members === 1 ? 'createGroup_memberCountOne' : 'createGroup_memberCountOther', { count: members })}
          </span>
        </div>
        <ul className="flex flex-col divide-y rounded-lg border">
          {state.members.map((member) => (
            <li key={member.id} className="flex items-center gap-3 px-3 py-2">
              <Avatar initial={member.initials} color={member.avatarColor} className="size-8 text-xs" />
              <span className="min-w-0 flex-1 truncate text-sm font-medium">{member.name}</span>
              {member.isMe ? (
                <span className="text-xs text-muted-foreground">{t('createGroup_me')}</span>
              ) : (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-8 text-muted-foreground"
                  aria-label={t('createGroup_removeMember', { name: member.name })}
                  onClick={() => presenter.removeMember(member.id)}
                >
                  <X />
                </Button>
              )}
            </li>
          ))}
          <li className="flex items-center gap-2 px-3 py-2">
            <Plus aria-hidden className="size-4 shrink-0 text-muted-foreground" />
            <Input
              value={draft}
              placeholder={t('createGroup_addPlaceholder')}
              aria-label={t('createGroup_addPlaceholder')}
              aria-invalid={state.takenName != null}
              autoComplete="off"
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                // Why: Enter adds the name, not the group; the field stays focused for the next one.
                if (event.key === 'Enter') {
                  event.preventDefault()
                  presenter.addMember()
                }
              }}
              className="h-8 border-0 px-1 shadow-none focus-visible:ring-0 aria-invalid:ring-0"
            />
            <Button type="button" variant="secondary" size="sm" disabled={!state.canAddMember} onClick={() => presenter.addMember()}>
              {t('createGroup_add')}
            </Button>
          </li>
        </ul>
        {state.takenName != null && (
          <div role="alert" className="flex flex-col gap-2">
            <p className="flex items-center gap-1.5 text-sm text-destructive">
              <CircleAlert className="size-4 shrink-0" />
              {t('createGroup_nameTaken', { name: state.takenName })}
            </p>
            <div className="flex flex-wrap gap-2">
              {state.nicknameSuggestions.map((suggestion) => (
                <Button key={suggestion} type="button" variant="outline" size="sm" onClick={() => presenter.useSuggestion(suggestion)}>
                  {t('createGroup_useNickname', { name: suggestion })}
                </Button>
              ))}
            </div>
          </div>
        )}
        {state.showsNoAppNotice && <p className="text-sm text-muted-foreground">{t('createGroup_noAppNotice')}</p>}
      </section>
    </form>
  )
}
