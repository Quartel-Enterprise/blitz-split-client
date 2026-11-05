import { Plus, Users } from 'lucide-react'
import { GroupsPresenter, type JsGroupsState } from 'blitzsplit-shared'
import { useEffect, useMemo } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { HomeShell, loadedProfile, pageContentClass, type HomeTab } from '@/components/HomeShell'
import { SummaryListPageSkeleton } from '@/components/PageSkeletons'
import { Button } from '@/components/ui/button'
import { useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { toastGroupAction } from '@/features/group-detail/GroupExitDialog'
import { GroupsActions, GroupsPage, SyncStatus } from '@/features/groups/GroupsPage'
import { SearchDialog, useHomeSearch, type SearchActions } from '@/features/groups/SearchDialog'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

interface DeletedGroupNoticeState {
  deletedGroupId?: string | null
  deletedGroupName?: string | null
  deletedGroupByName?: string | null
}

/**
 * "Carla apagou o grupo X." once someone else deleted a group, on Grupos and Início. State, not an action, so it waits
 * until the screen is on top; the next deleted group follows once this one shows.
 */
export function useDeletedGroupNotice(presenter: { deletedGroupNoticeShown(): void } | null, state: DeletedGroupNoticeState | null | undefined) {
  const deletedGroupId = state?.deletedGroupId ?? null
  const deletedGroupName = state?.deletedGroupName ?? null
  const deletedGroupByName = state?.deletedGroupByName ?? null
  useEffect(() => {
    if (!deletedGroupId || !deletedGroupName) return
    toast(
      deletedGroupByName
        ? t('groups_deletedBy', { name: deletedGroupByName, group: deletedGroupName })
        : t('groups_deleted', { group: deletedGroupName }),
    )
    presenter?.deletedGroupNoticeShown()
  }, [presenter, deletedGroupId, deletedGroupName, deletedGroupByName])
}

// Início, Saldos and Contatos take Grupos' place above Início.
const tabEvents: Partial<Record<HomeTab, (presenter: GroupsPresenter) => void>> = {
  home: (p) => p.openHome(),
  balances: (p) => p.openBalances(),
  contacts: (p) => p.openContacts(),
}

/**
 * The home: the empty card before any group exists, the groups page after, in the home's frame (HomeShell) with the
 * Grupos tab selected.
 */
export function GroupsScreen() {
  const presenter = usePresenter(() => new GroupsPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsGroupsState) => void) => p.observeState(onChange))
  useActions(
    presenter,
    (p, onAction: (action: string) => void) => p.observeActions(onAction),
    (action) => (action === 'coming-soon' ? toast(t('groups_comingSoon')) : toastGroupAction(action)),
  )
  useDeletedGroupNotice(presenter, state)
  // Why: Registrar pagamento, which a row's Acertar opened over the home, just closed.
  useRecordedPaymentNotice(presenter, state?.recordedPayment)
  const search = useHomeSearch(presenter, state?.canSearch ?? false)
  const searchActions = useMemo(() => (presenter ? searchActionsOf(presenter) : null), [presenter])
  if (!presenter || !state) return null
  // Why: the first download draws the page before its first group, with what is still coming in gray.
  const showsPage = state.groups.length > 0 || !!state.firstDownload

  return (
    <HomeShell
      selected="groups"
      profile={loadedProfile(state)}
      onTab={(tab) => tabEvents[tab]?.(presenter)}
      onProfile={() => presenter.openProfile()}
      onBrand={() => presenter.openBrand()}
      search={search}
      overlays={
        <>
          {state.isSearching && searchActions && <SearchDialog search={state.search} actions={searchActions} />}
          {state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
        </>
      }
    >
      <main className={cn(pageContentClass, 'gap-5')}>
        <div className="flex flex-wrap items-center gap-3 md:min-h-9">
          {/* Why: titled from the tablet up as the other tabs are; the phone's bottom bar already names the page. */}
          <h1 className="sr-only text-2xl leading-8 font-semibold tracking-tight md:not-sr-only">{t('groups_title')}</h1>
          <div className="mr-auto flex flex-wrap items-center gap-2">
            <SyncStatus state={state} presenter={presenter} />
          </div>
          {showsPage && <GroupsActions presenter={presenter} state={state} />}
        </div>
        {showsPage ? (
          <GroupsPage presenter={presenter} state={state} />
        ) : !state.hasLoadedGroups ? (
          <SummaryListPageSkeleton leading="square" />
        ) : (
          <>
            {state.showsEmptyState && (
              <section className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed p-8 text-center">
                <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
                  <Users className="size-6" />
                </span>
                <h2 className="mt-1 text-lg font-semibold">{t('groups_emptyTitle')}</h2>
                <p className="max-w-sm text-sm text-muted-foreground">{t('groups_emptyBody')}</p>
                <div className="flex w-full flex-col gap-2 pt-2 sm:w-auto sm:flex-row">
                  <Button onClick={() => presenter.createGroup()}>
                    <Plus /> {t('groups_create')}
                  </Button>
                  <Button variant="outline" onClick={() => presenter.joinWithLink()}>
                    {t('groups_joinWithLink')}
                  </Button>
                </div>
              </section>
            )}
          </>
        )}
      </main>
    </HomeShell>
  )
}

/** Grupos' own search: it closes first, with the home still on top, so what opens isn't refused nor popped by the close. */
function searchActionsOf(presenter: GroupsPresenter): SearchActions {
  const pick = (open: () => void) => {
    presenter.closeSearch()
    open()
  }
  return {
    closeSearch: () => presenter.closeSearch(),
    search: (query) => presenter.search(query),
    searchIn: (scope) => presenter.searchIn(scope),
    openSearchedGroup: (groupId) => pick(() => presenter.openSearchedGroup(groupId)),
    openSearchedPerson: (groupId) => pick(() => presenter.openSearchedPerson(groupId)),
    openSearchedContact: (personKey) => pick(() => presenter.openSearchedContact(personKey)),
    searchSuggestion: (suggestion) => presenter.searchSuggestion(suggestion),
    openSearchedBill: (groupId, expenseId) => pick(() => presenter.openSearchedBill(groupId, expenseId)),
  }
}
