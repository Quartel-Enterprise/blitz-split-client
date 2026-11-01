import { ChevronDown, ChevronRight, QrCode, Search, SearchX, UserRound, Users, WifiOff, X } from 'lucide-react'
import { ContactsPresenter, type JsContactRow, type JsContactsState, type JsRoute } from 'blitzsplit-shared'
import { useEffect, type ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { HomeShell, type HomeTab } from '@/components/HomeShell'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { balanceTone } from '@/features/balances/BalanceCard'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { cn } from '@/lib/utils'
import { ContactProfileView, useContactProfile } from './ContactProfile'
import { netWord, rowMeta } from './contactWords'

// Grupos and Saldos are the other tabs; Atividade is not built yet and answers "Coming soon".
const tabEvents: Partial<Record<HomeTab, (presenter: ContactsPresenter) => void>> = {
  groups: (p) => p.openGroups(),
  balances: (p) => p.openBalances(),
  activity: (p) => p.openActivity(),
}

/**
 * The home's Contatos tab: everyone the user shares a group with, A to Z, with the net with each, in the home's frame.
 * The desktop (from 1024 px) draws the list with the person picked beside it; a tablet and a phone open the profile
 * (`profileRoute`, pushed over Contatos) in the list's place. The browser can't read the agenda, so the only way to
 * bring someone new is a group's link.
 */
export function ContactsScreen({ profileRoute }: { profileRoute?: JsRoute }) {
  const presenter = usePresenter(() => new ContactsPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsContactsState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onAction: (action: string) => void) => p.observeActions(onAction), (action) =>
    toast(t(action === 'coming-soon' ? 'groups_comingSoon' : 'contacts_changeError')),
  )
  const isWide = useMediaQuery('(min-width: 1024px)')
  // Why: beside the list a click picks the person; anywhere else it opens their profile over the list.
  useEffect(() => {
    presenter?.setDetailPaneShown(isWide)
  }, [presenter, isWide])
  if (!presenter || !state) return null

  const personKey = profileRoute?.personKey
  if (personKey) return <ProfileOverList key={personKey} contacts={presenter} state={state} personKey={personKey} />
  return (
    <HomeShell selected="contacts" profile={state} onTab={(tab) => tabEvents[tab]?.(presenter)} onProfile={() => presenter.openProfile()}>
      <main
        className={cn(
          'mx-auto w-full flex-1 p-4 sm:px-8 sm:py-7 lg:px-10 lg:py-8',
          isWide ? 'grid max-w-6xl grid-cols-[340px_minmax(0,1fr)] items-start gap-6 xl:grid-cols-[400px_minmax(0,1fr)]' : 'flex max-w-3xl flex-col',
        )}
      >
        <ContactsList presenter={presenter} state={state} isWide={isWide} />
        {isWide && <DetailPane state={state} />}
      </main>
    </HomeShell>
  )
}

/** Tablet and phone: the profile in the list's place, still in the home's frame with Contatos selected. */
function ProfileOverList({ contacts, state, personKey }: { contacts: ContactsPresenter; state: JsContactsState; personKey: string }) {
  const profile = useContactProfile(personKey)
  // Why: Contatos' tab events close the profile first, as a tab bar does (ContactsViewModel).
  return (
    <HomeShell
      selected="contacts"
      profile={state}
      onTab={(tab) => tabEvents[tab]?.(contacts)}
      onProfile={() => contacts.openProfile()}
    >
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col p-4 sm:px-8 sm:py-7">
        <h1 className="sr-only">{t('contacts_title')}</h1>
        {profile.presenter && profile.state?.contact && (
          <ContactProfileView presenter={profile.presenter} state={profile.state} contact={profile.state.contact} onBack={() => profile.presenter?.back()} />
        )}
      </main>
    </HomeShell>
  )
}

/** The desktop's pane: the person picked, else what will show there. */
function DetailPane({ state }: { state: JsContactsState }) {
  return (
    <div className="sticky top-8 flex max-h-[calc(100dvh-4rem)] min-h-[480px] min-w-0 flex-col overflow-y-auto rounded-xl border bg-card p-6">
      {state.selectedKey ? (
        <ContactPanel key={state.selectedKey} personKey={state.selectedKey} />
      ) : (
        <section aria-label={t('contacts_pane')} className="my-auto flex flex-col items-center gap-3 text-center">
          <span aria-hidden className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
            <UserRound className="size-6" />
          </span>
          <p className="max-w-xs text-sm text-muted-foreground">{t(state.isEmpty ? 'contacts_emptyPlaceholder' : 'contacts_pickPlaceholder')}</p>
        </section>
      )}
    </div>
  )
}

function ContactPanel({ personKey }: { personKey: string }) {
  const profile = useContactProfile(personKey)
  if (!profile.presenter || !profile.state?.contact) return null
  return <ContactProfileView presenter={profile.presenter} state={profile.state} contact={profile.state.contact} />
}

function ContactsList({ presenter, state, isWide }: { presenter: ContactsPresenter; state: JsContactsState; isWide: boolean }) {
  const showsPeople = !state.isLoading && !state.isEmpty && !state.isSearching
  const selectedKey = isWide ? state.selectedKey : null
  const open = (row: JsContactRow) => presenter.openContact(row.key)
  return (
    <div className="flex min-w-0 flex-col gap-3 sm:gap-4">
      {/* Why: the phone's top bar already names the app, so only a screen reader gets the title there. */}
      <h1 className="sr-only text-2xl font-semibold tracking-tight sm:not-sr-only">{t('contacts_title')}</h1>
      <SearchField presenter={presenter} query={state.query} />
      {state.showsOfflineNotice && (
        <span className="inline-flex items-center gap-1.5 self-start rounded-md border px-2 py-1 text-xs text-muted-foreground">
          <WifiOff aria-hidden className="size-3.5" />
          {t('contacts_offline')}
        </span>
      )}
      {state.showsSummary && <SummaryCard presenter={presenter} state={state} />}
      {state.isLoading && <SkeletonRows />}
      {showsPeople && state.contacts.length > 0 && (
        <section aria-label={t('contacts_peopleLabel')} className="overflow-hidden rounded-xl border bg-card">
          <h2 className="flex h-9 items-center border-b bg-muted px-4 text-xs font-medium text-muted-foreground">
            {t('contacts_people', { count: state.contacts.length })}
          </h2>
          {state.contacts.map((row) => (
            <ContactRowButton key={row.key} row={row} isSelected={row.key === selectedKey} onClick={() => open(row)} />
          ))}
        </section>
      )}
      {state.isSearching && !state.showsNoResults && (
        <>
          <section aria-label={t('contacts_results')} className="overflow-hidden rounded-xl border bg-card">
            <h2 className="flex h-9 items-center border-b bg-muted px-4 text-xs font-medium text-muted-foreground">
              {t('contacts_inYourGroups', { count: state.groupResults.length })}
            </h2>
            {state.groupResults.map((row) => (
              <ContactRowButton key={row.key} row={row} isSelected={row.key === selectedKey} onClick={() => open(row)} />
            ))}
          </section>
          {/* Why: where the phones list "Na agenda": the browser has no agenda to look in. */}
          <p className="text-[13px] text-muted-foreground">{t('contacts_noAgenda')}</p>
        </>
      )}
      {state.showsNoResults && (
        <EmptyCard
          label={t('contacts_results')}
          icon={<SearchX className="size-6" />}
          title={t('contacts_noResultsTitle', { query: state.query.trim() })}
          body={t('contacts_noResultsBody')}
          onInvite={() => presenter.inviteWithLink()}
        />
      )}
      {state.isEmpty && (
        <EmptyCard
          label={t('contacts_title')}
          icon={<Users className="size-6" />}
          title={t('contacts_emptyTitle')}
          body={t('contacts_emptyBody')}
          onInvite={() => presenter.inviteWithLink()}
        />
      )}
      {(showsPeople || (state.isSearching && !state.showsNoResults)) && <InviteRow onClick={() => presenter.inviteWithLink()} />}
      {showsPeople && state.hiddenContacts.length > 0 && <HiddenSection presenter={presenter} state={state} />}
    </div>
  )
}

/** "Buscar contatos": typing searches the groups' people, hidden ones included; clearing it or Esc closes the search. */
function SearchField({ presenter, query }: { presenter: ContactsPresenter; query: string }) {
  const [draft, setDraft] = useDraft(query, (next) => presenter.search(next))
  return (
    <div className="relative">
      <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        type="search"
        aria-label={t('contacts_search')}
        placeholder={t('contacts_search')}
        value={draft}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Escape' && draft) {
            event.preventDefault()
            setDraft('')
          }
        }}
        className="bg-card pr-9 pl-9 [&::-webkit-search-cancel-button]:hidden"
      />
      {draft && (
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t('contacts_clearSearch')}
          onClick={() => setDraft('')}
          className="absolute top-1/2 right-0.5 -translate-y-1/2 text-muted-foreground"
        >
          <X />
        </Button>
      )}
    </div>
  )
}

/** "Você deve" and "Te devem", Saldos' totals: each opens Saldos on its filter, "Ver Saldos" on all of it. */
function SummaryCard({ presenter, state }: { presenter: ContactsPresenter; state: JsContactsState }) {
  const { total } = state
  const amount = (minor: number) => formatMoney(minor, total.currency, total.fractionDigits)
  const totals = [
    { label: t('contacts_youOwe'), minor: total.toPayMinor, tone: 'text-destructive', onClick: () => presenter.openYouOwe() },
    { label: t('contacts_owedToYou'), minor: total.toReceiveMinor, tone: 'text-primary', onClick: () => presenter.openOwedToYou() },
  ]
  return (
    <section aria-label={t('contacts_summary')} className="flex items-center gap-2 rounded-xl border bg-card p-2">
      <div className="grid flex-1 grid-cols-2 gap-1">
        {totals.map((item) => (
          <button
            key={item.label}
            type="button"
            disabled={item.minor === 0}
            onClick={item.onClick}
            className="flex flex-col gap-0.5 rounded-lg px-2.5 py-1.5 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none"
          >
            <span className="text-xs font-medium text-muted-foreground">{item.label}</span>
            <strong className={cn('text-lg font-bold tracking-tight whitespace-nowrap tabular-nums', item.minor === 0 ? 'text-muted-foreground' : item.tone)}>
              {amount(item.minor)}
            </strong>
          </button>
        ))}
      </div>
      <Button variant="ghost" size="sm" onClick={() => presenter.openBalancesCard()}>
        {t('contacts_seeBalances')}
        <ChevronRight />
      </Button>
    </section>
  )
}

/** A person, A to Z: the face, the name (with what the search found in bold), where they are, and the net with its word. */
function ContactRowButton({ row, isSelected, onClick }: { row: JsContactRow; isSelected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-current={isSelected || undefined}
      onClick={onClick}
      className={cn(
        'flex w-full items-center gap-3 border-b px-3.5 py-2.5 text-left outline-none last:border-b-0 hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:ring-inset sm:px-4',
        isSelected && 'bg-muted',
      )}
    >
      <Avatar initial={row.initials} color={row.color} className="size-8 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">
          <MatchedName row={row} />
        </span>
        <span className="truncate text-xs text-muted-foreground">{rowMeta(row)}</span>
      </span>
      <span className="flex shrink-0 flex-col items-end">
        <span className={cn('text-sm font-semibold whitespace-nowrap tabular-nums', balanceTone(row.netMinor))}>
          {formatSignedMoney(row.netMinor, row.currency, row.fractionDigits)}
        </span>
        <span className="text-xs text-muted-foreground">{netWord(row.netMinor)}</span>
      </span>
    </button>
  )
}

function MatchedName({ row }: { row: JsContactRow }) {
  if (row.matchStart < 0) return <>{row.name}</>
  return (
    <>
      {row.name.slice(0, row.matchStart)}
      <strong className="font-bold">{row.name.slice(row.matchStart, row.matchEnd)}</strong>
      {row.name.slice(row.matchEnd)}
    </>
  )
}

/** "Convidar com link ou QR": the web's only way to bring someone new, since the browser can't read the agenda. */
function InviteRow({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-3 rounded-xl border bg-card py-3 pr-3 pl-3.5 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50"
    >
      <span aria-hidden className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted">
        <QrCode className="size-[18px]" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-sm font-medium">{t('contacts_inviteWithLink')}</span>
        <span className="text-xs text-muted-foreground">{t('contacts_inviteWithLinkSub')}</span>
      </span>
      <ChevronRight aria-hidden className="size-4 text-muted-foreground" />
    </button>
  )
}

/** "Ocultos · 1", closed by default: each hidden person with "Mostrar", and why they are still around. */
function HiddenSection({ presenter, state }: { presenter: ContactsPresenter; state: JsContactsState }) {
  const count = state.hiddenContacts.length
  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        aria-expanded={state.isHiddenExpanded}
        onClick={() => presenter.toggleHidden()}
        className="-ml-2.5 self-start text-[13px] text-muted-foreground"
      >
        {state.isHiddenExpanded ? <ChevronDown /> : <ChevronRight />}
        {t('contacts_hidden', { count })}
      </Button>
      {state.isHiddenExpanded && (
        <>
          <section aria-label={t('contacts_hiddenLabel')} className="overflow-hidden rounded-xl border bg-card">
            {state.hiddenContacts.map((row) => (
              <div key={row.key} className="flex items-center gap-2 border-b py-1.5 pr-3 pl-1.5 last:border-b-0">
                <button
                  type="button"
                  onClick={() => presenter.openContact(row.key)}
                  className="flex min-w-0 flex-1 items-center gap-3 rounded-lg px-2.5 py-1.5 text-left outline-none hover:bg-accent/50 focus-visible:ring-[3px] focus-visible:ring-ring/50"
                >
                  <Avatar initial={row.initials} color={row.color} className="size-8 text-xs" />
                  <span className="flex min-w-0 flex-col">
                    <span className="truncate text-sm font-medium">{row.name}</span>
                    <span className="truncate text-xs text-muted-foreground">
                      {t('contacts_hiddenMeta', {
                        meta: rowMeta(row),
                        amount: formatSignedMoney(row.netMinor, row.currency, row.fractionDigits),
                        word: netWord(row.netMinor),
                      })}
                    </span>
                  </span>
                </button>
                <Button variant="outline" size="sm" aria-label={t('contacts_showPerson', { name: row.name })} onClick={() => presenter.showContact(row.key)}>
                  {t('contacts_show')}
                </Button>
              </div>
            ))}
          </section>
          <p className="text-[13px] text-muted-foreground">{t('contacts_hiddenNote')}</p>
        </>
      )}
    </>
  )
}

/** Only until the device's data is read, so the empty state never flashes. */
function SkeletonRows() {
  const widths = [
    ['w-24', 'w-36'],
    ['w-16', 'w-28'],
    ['w-28', 'w-24'],
    ['w-14', 'w-40'],
    ['w-20', 'w-28'],
    ['w-20', 'w-32'],
  ]
  return (
    <section aria-label={t('contacts_loading')} aria-busy className="overflow-hidden rounded-xl border bg-card">
      <h2 className="flex h-9 items-center border-b bg-muted px-4 text-xs font-medium text-muted-foreground">{t('contacts_peopleLabel')}</h2>
      {widths.map(([name, meta], index) => (
        <div key={index} style={{ opacity: 1 - index * 0.12 }} className="flex items-center gap-3 border-b px-4 py-2.5 last:border-b-0">
          <span className="size-8 shrink-0 rounded-full bg-muted" />
          <span className="flex flex-1 flex-col gap-1.5">
            <span className={cn('h-3 rounded bg-muted', name)} />
            <span className={cn('h-2.5 rounded bg-muted', meta)} />
          </span>
        </div>
      ))}
    </section>
  )
}

/** "Ninguém por aqui ainda" and "Ninguém com “Rodrigo”": both lead to a group's link. */
function EmptyCard({ label, icon, title, body, onInvite }: { label: string; icon: ReactNode; title: string; body: string; onInvite: () => void }) {
  return (
    <section aria-label={label} className="flex flex-col items-center gap-2 rounded-xl border bg-card px-6 py-10 text-center">
      <span aria-hidden className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        {icon}
      </span>
      <h2 className="mt-1 text-lg font-semibold">{title}</h2>
      <p className="max-w-xs text-sm text-muted-foreground">{body}</p>
      <Button onClick={onInvite} className="mt-1">
        {t('contacts_inviteWithLink')}
      </Button>
    </section>
  )
}
