import { Plus, Users } from 'lucide-react'
import { GroupsPresenter, type JsGroupsState } from 'blitzsplit-shared'
import { useEffect } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { HomeShell, type HomeTab } from '@/components/HomeShell'
import { Button } from '@/components/ui/button'
import { useRecordedPaymentNotice } from '@/features/balances/BalanceCard'
import { toastGroupAction } from '@/features/group-detail/GroupExitDialog'
import { GroupsPage, SyncStatus } from '@/features/groups/GroupsPage'
import { GroupsSearchDialog, searchShortcut } from '@/features/groups/GroupsSearchDialog'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { t } from '@/i18n'

interface DeletedGroupNoticeState {
  deletedGroupId: string | null
  deletedGroupName: string | null
  deletedGroupByName: string | null
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

// Contatos and Atividade are not built yet and answer "Coming soon".
const tabEvents: Partial<Record<HomeTab, (presenter: GroupsPresenter) => void>> = {
  home: (p) => p.openHome(),
  balances: (p) => p.openBalances(),
  contacts: (p) => p.openContacts(),
  activity: (p) => p.openActivity(),
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
  // Why: ⌘K (Ctrl+K off Apple devices) opens "Buscar" from anywhere on the home, once there is a group to look in.
  const canSearch = state?.canSearch ?? false
  useEffect(() => {
    if (!presenter || !canSearch) return
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && !event.altKey && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        presenter.openSearch()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [presenter, canSearch])
  if (!presenter || !state) return null

  return (
    <HomeShell
      selected="groups"
      profile={state}
      onTab={(tab) => tabEvents[tab]?.(presenter)}
      onProfile={() => presenter.openProfile()}
      search={
        state.canSearch
          ? { shortcut: searchShortcut, keyShortcuts: searchShortcut === '⌘K' ? 'Meta+K' : 'Control+K', onOpen: () => presenter.openSearch() }
          : undefined
      }
      overlays={
        <>
          {state.isSearching && <GroupsSearchDialog presenter={presenter} state={state} />}
          {state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
        </>
      }
    >
      <main className="flex flex-1 flex-col gap-5 p-4 sm:p-8 lg:px-10">
        {/* Why: the selected Groups tab already names the page; the heading stays for screen readers. */}
        <h1 className="sr-only">{t('groups_title')}</h1>
        {state.groups.length > 0 ? (
          <GroupsPage presenter={presenter} state={state} />
        ) : (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <SyncStatus state={state} presenter={presenter} />
            </div>
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
