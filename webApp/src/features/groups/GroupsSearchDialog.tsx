import { ReceiptText, Search } from 'lucide-react'
import type { GroupsPresenter, JsGroupsState, JsSearchBillHit, JsSearchBillPayer, JsTextMatch } from 'blitzsplit-shared'
import { useState, type KeyboardEvent, type ReactNode } from 'react'
import { Avatar } from '@/components/common'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { balanceTone, nameOrFormer } from '@/features/balances/BalanceCard'
import { GroupAvatar } from '@/features/create-group/GroupAvatar'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { formatMoney, formatSignedMoney } from '@/lib/money'
import { payerNamesText } from '@/lib/payers'
import { isAppleDevice } from '@/lib/platform'
import { cn } from '@/lib/utils'

/** "⌘K" on a Mac, iPhone or iPad, "Ctrl K" anywhere else, as the sidebar's "Buscar…" shows it. */
export const searchShortcut = isAppleDevice() ? '⌘K' : 'Ctrl K'

const scopes: { scope: string; label: MessageKey }[] = [
  { scope: 'ALL', label: 'groups_searchScopeAll' },
  { scope: 'GROUPS', label: 'groups_searchScopeGroups' },
  { scope: 'PEOPLE', label: 'groups_searchScopePeople' },
  { scope: 'BILLS', label: 'groups_searchScopeBills' },
]

interface SearchItem {
  key: string
  section: MessageKey
  open: () => void
  content: ReactNode
}

/**
 * "Buscar" (⌘K): a command dialog over the home, near the top on desktop and tablet and across the top on a phone. It
 * looks in the groups, people and bills of this browser, the settled groups included, offline too, with Tudo, Grupos,
 * Pessoas and Despesas under the field. The arrows move through the results, Enter opens one, Esc closes. Picking a
 * result closes the search and opens the group, the person's members or the bill over its group.
 */
export function GroupsSearchDialog({ presenter, state }: { presenter: GroupsPresenter; state: JsGroupsState }) {
  const search = state.search
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const [active, setActive] = useState(0)
  const [draft, setDraft] = useDraft(search.query, (next) => {
    setActive(0)
    presenter.search(next)
  })
  // Why: the search closes first, with the home still on top, so what opens isn't refused nor popped by the close.
  const pick = (open: () => void) => {
    presenter.closeSearch()
    open()
  }
  const items = searchItemsOf(presenter, state, pick)
  const activeIndex = Math.min(active, Math.max(items.length - 1, 0))
  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (items.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((activeIndex + 1) % items.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((activeIndex - 1 + items.length) % items.length)
    } else if (event.key === 'Enter') {
      event.preventDefault()
      items[activeIndex]?.open()
    }
  }
  return (
    <Dialog open onOpenChange={(open) => !open && presenter.closeSearch()}>
      <DialogContent
        showCloseButton={false}
        className={cn(
          'gap-0 overflow-hidden p-0',
          isDesktop ? 'top-[120px] max-h-[calc(100dvh-160px)] translate-y-0 sm:max-w-[560px]' : 'top-0 left-0 max-h-dvh max-w-none translate-x-0 translate-y-0 rounded-none rounded-b-xl sm:max-w-none',
        )}
      >
        <DialogTitle className="sr-only">{t('groups_search')}</DialogTitle>
        <DialogDescription className="sr-only">{t('groups_searchHint')}</DialogDescription>
        <div className="flex h-12 items-center gap-2 border-b px-3">
          <Search aria-hidden className="size-4 shrink-0 text-muted-foreground" />
          <input
            autoFocus
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={onKeyDown}
            placeholder={t('groups_searchPlaceholder')}
            role="combobox"
            aria-expanded={items.length > 0}
            aria-controls="groups-search-results"
            aria-activedescendant={items.length > 0 ? `groups-search-${activeIndex}` : undefined}
            aria-label={t('groups_search')}
            className="min-w-0 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
          <kbd className="flex h-5 items-center rounded border bg-muted px-1.5 font-sans text-[11px] text-muted-foreground">Esc</kbd>
        </div>
        <div role="tablist" aria-label={t('groups_searchScopes')} className="flex gap-1 border-b px-3 py-2">
          {scopes.map(({ scope, label }) => (
            <button
              key={scope}
              role="tab"
              aria-selected={search.scope === scope}
              onClick={() => {
                setActive(0)
                presenter.searchIn(scope)
              }}
              className={cn(
                'h-7 rounded-md px-2.5 text-xs font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                search.scope === scope ? 'bg-muted text-foreground' : 'text-muted-foreground hover:text-foreground',
              )}
            >
              {t(label)}
            </button>
          ))}
        </div>
        <div className="overflow-y-auto">
          {!search.hasQuery && <p className="px-4 py-8 text-center text-sm text-muted-foreground">{t('groups_searchHint')}</p>}
          {search.showsNoResults && <p className="px-4 py-8 text-center text-sm text-muted-foreground">{t('groups_searchNone', { query: search.trimmedQuery })}</p>}
          {search.showsResults && (
            <div id="groups-search-results" role="listbox" aria-label={t('groups_search')} className="p-1">
              {items.map((item, index) => (
                <div key={item.key}>
                  {(index === 0 || items[index - 1].section !== item.section) && (
                    <>
                      {index > 0 && <div className="-mx-1 my-1 h-px bg-border" />}
                      <div className="px-2 pt-2 pb-1 text-xs font-medium text-muted-foreground">{t(item.section)}</div>
                    </>
                  )}
                  <div
                    id={`groups-search-${index}`}
                    role="option"
                    aria-selected={index === activeIndex}
                    onClick={item.open}
                    onMouseMove={() => index !== activeIndex && setActive(index)}
                    className={cn('flex h-10 cursor-pointer items-center gap-2.5 rounded-md px-2 text-sm', index === activeIndex && 'bg-muted')}
                  >
                    {item.content}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}

/** The results in the order the dialog lists them: the groups, the people, then the bills. */
function searchItemsOf(presenter: GroupsPresenter, state: JsGroupsState, pick: (open: () => void) => void): SearchItem[] {
  const search = state.search
  const groups: SearchItem[] = search.groups.map((hit) => ({
    key: `group:${hit.group.id}`,
    section: 'groups_searchSectionGroups',
    open: () => pick(() => presenter.openGroup(hit.group.id)),
    content: (
      <>
        <GroupAvatar initials={hit.group.initials} color={hit.group.color} className="size-6 shrink-0 rounded-md text-[10px]" />
        <span className="min-w-0 flex-1 truncate">
          <Marked text={hit.group.name} match={hit.nameMatch} />
          {hit.billDescription != null && (
            <span className="text-muted-foreground">
              {' · '}
              <Templated template={t('groups_searchBillInGroup')} value={<Marked text={hit.billDescription} match={hit.billMatch} className="text-foreground" />} />
            </span>
          )}
        </span>
        {hit.group.isSettled ? (
          <span className="inline-flex h-5 shrink-0 items-center rounded-md border px-1.5 text-[11px] text-muted-foreground">{t('groups_balanceEveryoneSettled')}</span>
        ) : (
          <span className={cn('shrink-0 font-semibold tabular-nums', balanceTone(hit.group.netMinor))}>
            {formatSignedMoney(hit.group.netMinor, hit.group.currency, hit.group.fractionDigits)}
          </span>
        )}
      </>
    ),
  }))
  const people: SearchItem[] = search.people.map((hit) => ({
    key: `person:${hit.groupId}:${hit.memberId}`,
    section: 'groups_searchSectionPeople',
    open: () => pick(() => presenter.openSearchedPerson(hit.groupId)),
    content: (
      <>
        <Avatar initial={hit.initial} color={hit.color} className="size-6 shrink-0 text-[10px]" />
        <span className="min-w-0 flex-1 truncate">
          <Marked text={hit.name} match={hit.nameMatch} />
          <span className="text-muted-foreground"> · {hit.groupName}</span>
        </span>
      </>
    ),
  }))
  const bills: SearchItem[] = search.bills.map((hit) => ({
    key: `bill:${hit.id}`,
    section: 'groups_searchSectionBills',
    open: () => pick(() => presenter.openSearchedBill(hit.groupId, hit.id)),
    content: (
      <>
        <ReceiptText aria-hidden className="mx-1 size-4 shrink-0 text-muted-foreground" />
        <span className="min-w-0 flex-1 truncate">
          <Marked text={hit.description} match={hit.descriptionMatch} />
          <span className="text-muted-foreground"> · {billMetaText(hit)}</span>
        </span>
        <span className="shrink-0 tabular-nums">{formatMoney(hit.totalMinor, hit.currency, hit.fractionDigits)}</span>
      </>
    ),
  }))
  return [...groups, ...people, ...bills]
}

/** "Churrasco de sábado · Bruno pagou", "· Você pagou", "· Ana e Bruno pagaram" or "· 3 pessoas pagaram". */
function billMetaText(hit: JsSearchBillHit): string {
  const paid = paidText(Array.from(hit.payers))
  return paid ? `${hit.groupName} · ${paid}` : hit.groupName
}

function paidText(payers: JsSearchBillPayer[]): string | null {
  if (payers.length > 1) {
    return t('groups_searchPaidBySeveral', { names: payerNamesText(payers.map((payer) => (payer.isMe ? t('splitBill_you') : nameOrFormer(payer.name)))) })
  }
  const payer = payers[0]
  if (!payer) return null
  if (payer.isMe) return t('groups_searchYouPaid')
  return payer.name != null ? t('groups_searchPaidBy', { name: payer.name }) : null
}

/** The text with what the search found in bold, as the canvas marks "Chur" in "Churrasco". */
function Marked({ text, match, className }: { text: string; match: JsTextMatch | null | undefined; className?: string }) {
  if (!match || match.end > text.length) return <span className={className}>{text}</span>
  return (
    <span className={className}>
      {text.slice(0, match.start)}
      <strong className="font-semibold">{text.slice(match.start, match.end)}</strong>
      {text.slice(match.end)}
    </span>
  )
}

/** A translated sentence around a marked value: "despesa “{bill}”". */
function Templated({ template, value }: { template: string; value: ReactNode }) {
  const [before, after = ''] = template.split('{bill}')
  return (
    <>
      {before}
      {value}
      {after}
    </>
  )
}
