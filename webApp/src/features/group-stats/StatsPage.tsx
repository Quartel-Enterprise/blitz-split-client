import { ArrowLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { groupPageContentClass, HomeTabCrumb, type HomeTab } from '@/components/HomeShell'
import { PageShell, useOpenHomeTab } from '@/components/HomeSidebar'
import { Button } from '@/components/ui/button'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * A page of the group's statistics (Statistics, Expenses by category) in the home's frame: the phone's bar with back,
 * the title and what it is about, and from the tablet up the breadcrumb from the home tab to the page. overlays draw
 * after the page (the phone's period drawer).
 */
export function StatsPage({
  homeTab,
  groupId,
  backLabel,
  title,
  subtitle,
  onBack,
  crumbs,
  overlays,
  children,
}: {
  homeTab: HomeTab
  groupId: string
  backLabel: string
  title: string
  subtitle: string
  onBack: () => void
  // Why: the pages between the home tab and this one (the group, the statistics), each a CrumbButton.
  crumbs: ReactNode
  overlays?: ReactNode
  children: ReactNode
}) {
  const openHomeTab = useOpenHomeTab()
  return (
    <PageShell selected={homeTab} groups={{ selectedId: groupId }} phoneBars="none">
      <div className="flex min-h-dvh flex-col">
        <header className="sticky top-0 z-10 flex h-14 items-center gap-1 border-b bg-background/95 px-2 backdrop-blur md:hidden">
          <Button variant="ghost" size="icon" aria-label={backLabel} onClick={onBack}>
            <ArrowLeft />
          </Button>
          <span className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-semibold">{title}</span>
            <span className="truncate text-xs text-muted-foreground">{subtitle}</span>
          </span>
        </header>

        <main className={cn(groupPageContentClass, 'gap-4 sm:gap-5')}>
          <nav aria-label={t('groupDetail_breadcrumb')} className="-mb-1 hidden items-center gap-1.5 text-[13px] md:flex">
            <HomeTabCrumb tab={homeTab} onOpen={openHomeTab} />
            {crumbs}
            <span aria-current="page">{title}</span>
          </nav>
          {children}
        </main>
        {overlays}
      </div>
    </PageShell>
  )
}
