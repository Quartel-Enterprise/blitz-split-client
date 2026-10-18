import { CloudCheck, CloudOff, Plus, ReceiptText, RefreshCw, UserRound, Users, Wallet } from 'lucide-react'
import { GroupsPresenter, type JsGroupsState } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, BrandMark } from '@/components/common'
import { Button } from '@/components/ui/button'
import { RecoveryCodeDialog } from '@/features/recovery-code/RecoveryCodeDialog'
import { cn } from '@/lib/utils'

interface Tab {
  label: string
  icon: ReactNode
  open?: (presenter: GroupsPresenter) => void
}

// Only Grupos is built; the other tabs of the canvas answer "Em breve".
const tabs: Tab[] = [
  { label: 'Grupos', icon: <Users className="size-4" /> },
  { label: 'Saldos', icon: <Wallet className="size-4" />, open: (p) => p.openBalances() },
  { label: 'Contatos', icon: <UserRound className="size-4" />, open: (p) => p.openContacts() },
  { label: 'Atividade', icon: <ReceiptText className="size-4" />, open: (p) => p.openActivity() },
]

const syncLabels: Record<string, { label: string; icon: ReactNode }> = {
  OFFLINE: { label: 'Offline · tudo fica salvo neste navegador', icon: <CloudOff className="size-3.5" /> },
  SYNCING: { label: 'Sincronizando…', icon: <RefreshCw className="size-3.5" /> },
  SYNCED: { label: 'Sincronizado agora', icon: <CloudCheck className="size-3.5" /> },
}

/**
 * The first home, before any group exists. Desktop (1024+) has the full sidebar, tablet (768–1023) an
 * icon rail and the phone a top bar with a bottom navigation, as the canvas draws them.
 */
export function GroupsScreen() {
  const presenter = usePresenter(() => new GroupsPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsGroupsState) => void) => p.observeState(onChange))
  useActions(presenter, (p, onShow: (show: boolean) => void) => p.observeComingSoon(onShow), () => toast('Em breve'))
  if (!presenter || !state) return null

  const sync = syncLabels[state.syncBadge] ?? syncLabels.SYNCING
  return (
    <div className="flex min-h-dvh">
      <aside className="sticky top-0 hidden h-dvh w-16 shrink-0 flex-col border-r bg-sidebar p-3 md:flex lg:w-60">
        <BrandMark showsName={false} className="h-10 px-1 lg:hidden" />
        <BrandMark className="hidden h-10 px-1.5 lg:flex" />
        <nav aria-label="Navegação principal" className="mt-4 flex flex-col gap-0.5">
          {tabs.map((tab, index) => (
            <button
              key={tab.label}
              aria-label={tab.label}
              aria-current={index === 0 ? 'page' : undefined}
              onClick={() => tab.open?.(presenter)}
              className={cn(
                'flex h-9 items-center justify-center gap-2.5 rounded-md px-2.5 text-sm font-medium outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start',
                index === 0 ? 'bg-sidebar-accent text-sidebar-accent-foreground' : 'text-muted-foreground',
              )}
            >
              {tab.icon}
              <span className="hidden lg:inline">{tab.label}</span>
            </button>
          ))}
        </nav>
        <button
          onClick={() => presenter.openProfile()}
          aria-label="Perfil"
          className="mt-auto flex h-12 items-center justify-center gap-2.5 rounded-lg px-1.5 text-left outline-none hover:bg-sidebar-accent focus-visible:ring-[3px] focus-visible:ring-ring lg:justify-start"
        >
          <Avatar initial={state.avatarInitial} color={state.avatarColor} className="size-7 text-xs" />
          <span className="hidden flex-col lg:flex">
            <span className="text-sm font-medium">Perfil</span>
            <span className="text-xs text-muted-foreground">Sem conta</span>
          </span>
        </button>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-2.5 border-b px-4 md:hidden">
          <BrandMark className="mr-auto" />
          <button aria-label="Perfil" onClick={() => presenter.openProfile()} className="rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring">
            <Avatar initial={state.avatarInitial} color={state.avatarColor} className="size-9 text-sm" />
          </button>
        </header>

        <main className="flex flex-1 flex-col gap-6 p-4 sm:p-8">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
            <h1 className="text-2xl font-bold tracking-tight">Grupos</h1>
            <span className="inline-flex h-6 items-center gap-1.5 rounded-md border px-2 text-xs font-medium text-muted-foreground">
              {sync.icon}
              {sync.label}
            </span>
          </div>
          <section className="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl border border-dashed p-8 text-center">
            <span aria-hidden className="flex size-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
              <Users className="size-6" />
            </span>
            <h2 className="mt-1 text-lg font-semibold">Nenhum grupo ainda</h2>
            <p className="max-w-sm text-sm text-muted-foreground">Crie um grupo para a viagem, a casa ou o churrasco. Dá para começar mesmo sem internet.</p>
            <div className="flex w-full flex-col gap-2 pt-2 sm:w-auto sm:flex-row">
              <Button onClick={() => presenter.createGroup()}>
                <Plus /> Criar grupo
              </Button>
              <Button variant="outline" onClick={() => presenter.joinWithLink()}>
                Entrar com link ou QR
              </Button>
            </div>
          </section>
        </main>

        <nav aria-label="Navegação principal" className="sticky bottom-0 grid h-16 grid-cols-4 border-t bg-card md:hidden">
          {tabs.map((tab, index) => (
            <button
              key={tab.label}
              aria-current={index === 0 ? 'page' : undefined}
              onClick={() => tab.open?.(presenter)}
              className={cn('flex flex-col items-center justify-center gap-1 text-[11px] font-medium', index === 0 ? 'text-foreground' : 'text-muted-foreground')}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {state.showsRecoveryCodePrompt && <RecoveryCodeDialog />}
    </div>
  )
}
