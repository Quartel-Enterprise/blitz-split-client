import { ArrowLeft, CloudOff, Ellipsis, FolderPlus, Pencil, Plus, Search, SearchX, Tag } from 'lucide-react'
import { useEffect } from 'react'
import { toast } from 'sonner'
import {
  CategoryMapPresenter,
  type JsCategory,
  type JsCategoryMapAction,
  type JsCategoryMapState,
  type JsMapRow,
  type JsMapSearchResult,
  type JsMapSection,
} from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { CategoryDeleteDialog } from '@/components/CategoryForm'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { LoadingRegion, Skeleton, SkeletonCard, SkeletonLine, SkeletonRows } from '@/components/ui/skeleton'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { CategoryBadge, categoryName, categoryPath } from '@/lib/categories'
import { cn } from '@/lib/utils'
import { CategoryFormDialog } from './CategoryFormDialog'
import { TermDialog } from './TermDialog'
import { PendingDot } from '@/components/common'

/** "7 termos", "Nenhum termo · categoria do Viagem Floripa". */
export function sectionCount(section: JsMapSection): string {
  const terms =
    section.termCount === 0 ? t('categoryMap_noTerms') : section.termCount === 1 ? t('categoryMap_oneTerm') : t('categoryMap_terms', { count: section.termCount })
  return section.groupName ? `${terms} · ${t('categoryMap_groupCategory', { group: section.groupName })}` : terms
}

function rowLabel(row: JsMapRow): string {
  return row.isOnlyMain ? t('categoryMap_onlyMain', { name: categoryName(row.category) }) : categoryName(row.category)
}

/**
 * "Mapa de categorias" (9.1) under the profile: the search, "Adicionar termo" and ⋯ with "Restaurar padrão"; from the
 * tablet up the categories on the left and a Card with the picked one's terms, on a phone the cards stacked. "Novo
 * termo" and "Editar termo" (9.2) open in a Dialog (a Drawer on a phone).
 */
export function CategoryMapScreen() {
  const presenter = usePresenter(() => new CategoryMapPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsCategoryMapState) => void) => p.observeState(onChange))
  const isWide = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsCategoryMapAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'category-save-error') toast.error(t('splitBill_categorySaveError'))
    else if (action.type === 'category-delete-error') toast.error(t('splitBill_categoryDeleteError'))
    else toast.error(t('categoryMap_saveError'))
  })
  const removed = state?.removedTerm ?? null
  // Why: only the latest removed term's toast acts, so an older one closing never drops a newer term's Desfazer.
  useEffect(() => {
    if (!presenter || !removed) return
    let isCurrent = true
    const settle = () => {
      if (isCurrent) presenter.dismissRemoved()
    }
    const id = toast(t('categoryMap_removed', { term: removed }), {
      action: {
        label: t('categoryMap_undo'),
        onClick: () => {
          if (isCurrent) presenter.undoRemove()
        },
      },
      onAutoClose: settle,
      onDismiss: settle,
    })
    return () => {
      isCurrent = false
      toast.dismiss(id)
    }
  }, [presenter, removed])
  const [query, setQuery] = useDraft(state?.query ?? '', (value) => {
    presenter?.changeQuery(value)
    return presenter?.getQuery()
  })
  if (!presenter || !state) return null
  const showsTwo = isWide && !state.isSearching && !state.isEmpty
  const selected = state.sections.find((section) => section.category.id === state.selectedCategoryId) ?? state.sections[0]

  return (
    <div className="min-h-dvh bg-muted/40">
      <header className="sticky top-0 z-10 flex h-14 items-center gap-2 border-b bg-background/95 px-2 backdrop-blur sm:px-4">
        <Button variant="ghost" size="icon" aria-label={t('common_back')} onClick={() => presenter.back()}>
          <ArrowLeft />
        </Button>
        <h1 className="mr-auto text-lg font-semibold">{t('categoryMap_title')}</h1>
        {/* Why: a term added or the map restored before the whole map was read could overwrite what is coming. */}
        <Button size="sm" disabled={!state.canEditMap} onClick={() => presenter.addTerm(null, '')}>
          <Plus /> <span className="hidden sm:inline">{t('categoryMap_add')}</span>
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label={t('categoryMap_more')} disabled={!state.canEditMap}>
              <Ellipsis />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {/* Why: a category goes in a group (9.4), so with none on the device there is nowhere to create one. */}
            {state.canCreateCategory && <DropdownMenuItem onSelect={() => presenter.createCategory(null)}>{t('splitBill_categoryNew')}</DropdownMenuItem>}
            <DropdownMenuItem onSelect={() => presenter.askToRestore()}>{t('categoryMap_restore')}</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </header>
      <main className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-4 sm:p-8">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('categoryMap_search')} aria-label={t('categoryMap_search')} className="pl-9" />
        </div>
        <MapNotices presenter={presenter} state={state} />
        {!state.isLoaded ? (
          <MapSkeleton isWide={isWide} />
        ) : state.isSearching ? (
          <SearchResults presenter={presenter} state={state} />
        ) : state.isEmpty ? (
          <EmptyMap presenter={presenter} />
        ) : showsTwo && selected ? (
          <div className="grid grid-cols-[minmax(0,18rem)_minmax(0,1fr)] gap-6">
            <nav aria-label={t('categoryMap_title')} className="flex flex-col gap-1">
              {state.sections.map((section) => (
                <button
                  key={section.category.id}
                  type="button"
                  aria-current={section.category.id === selected.category.id ? 'true' : undefined}
                  onClick={() => presenter.selectSection(section.category.id)}
                  className={cn(
                    'flex items-center gap-3 rounded-lg p-2 text-left hover:bg-accent',
                    section.category.id === selected.category.id && 'bg-secondary text-secondary-foreground',
                  )}
                >
                  <CategoryBadge category={section.category} className="size-9" />
                  <span className="flex min-w-0 flex-col">
                    <span className="truncate font-medium">{categoryName(section.category)}</span>
                    <span className="text-xs text-muted-foreground">{sectionCount(section)}</span>
                  </span>
                </button>
              ))}
            </nav>
            <SectionCard presenter={presenter} section={selected} canCreateCategory={state.canCreateCategory} isDetail />
          </div>
        ) : (
          state.sections.map((section) => (
            <SectionCard key={section.category.id} presenter={presenter} section={section} canCreateCategory={state.canCreateCategory} />
          ))
        )}
      </main>
      {state.form && <TermDialog presenter={presenter} state={state} form={state.form} isDesktop={isWide} />}
      {state.categoryForm && <CategoryFormDialog presenter={presenter} state={state} form={state.categoryForm} isDesktop={isWide} />}
      {state.categoryDeletion && state.categoryForm && (
        <CategoryDeleteDialog actions={presenter} deletion={state.categoryDeletion} groupName={state.categoryForm.groupName} />
      )}
      {state.isRestoreConfirmOpen && (
        <ConfirmDialog
          title={t('categoryMap_restoreTitle')}
          description={restoreMessage(state)}
          confirmLabel={t('categoryMap_restoreConfirm')}
          onConfirm={() => presenter.confirmRestore()}
          onDismiss={() => presenter.dismissRestore()}
        />
      )}
    </div>
  )
}

/** The map until its terms, categories and account were read: category cards with their terms, or beside the list. */
function MapSkeleton({ isWide }: { isWide: boolean }) {
  const card = (
    <SkeletonCard>
      <span className="flex items-center gap-3">
        <Skeleton className="size-10 rounded-xl" />
        <span className="flex flex-1 flex-col gap-2">
          <SkeletonLine className="h-4 w-1/3" />
          <SkeletonLine className="h-2.5 w-1/5" />
        </span>
      </span>
      <span className="flex flex-wrap gap-2">
        {[16, 20, 14, 24, 18].map((width) => (
          <Skeleton key={width} className="h-6 rounded-full" style={{ width: `${width * 4}px` }} />
        ))}
      </span>
    </SkeletonCard>
  )
  return (
    <LoadingRegion>
      {isWide ? (
        <div className="grid grid-cols-[minmax(0,18rem)_minmax(0,1fr)] gap-6">
          <SkeletonRows count={6} leading="square" hasTrailing={false} rowClassName="px-2" />
          {card}
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {card}
          {card}
        </div>
      )}
    </LoadingRegion>
  )
}

function restoreMessage(state: JsCategoryMapState): string {
  const body = t('categoryMap_restoreBody')
  if (state.ownTerms.length === 0) return body
  if (state.ownTerms.length === 1) return `${body} ${t('categoryMap_restoreOwnOne', { term: state.ownTerms[0] })}`
  return `${body} ${t('categoryMap_restoreOwnMany', { count: state.ownTerms.length })}`
}

function MapNotices({ presenter, state }: { presenter: CategoryMapPresenter; state: JsCategoryMapState }) {
  return (
    <>
      {state.showsOfflineNotice && (
        <div role="status" className="flex items-center gap-3 rounded-lg border bg-card p-3 text-sm">
          <CloudOff className="size-4 shrink-0 text-muted-foreground" />
          {state.pendingCount === 1 ? t('categoryMap_offlineOne') : t('categoryMap_offlineMany', { count: state.pendingCount })}
        </div>
      )}
      {state.showsDeviceOnlyNotice && !state.isSearching && (
        <div className="flex flex-wrap items-center gap-3 rounded-lg border bg-card p-3 text-sm">
          <p className="min-w-0 flex-1">{t('categoryMap_deviceOnly')}</p>
          <Button variant="outline" size="sm" onClick={() => presenter.openProfile()}>
            {t('categoryMap_openProfile')}
          </Button>
        </div>
      )}
      {!state.isSearching && !state.isEmpty && (
        <p className="text-sm text-muted-foreground">{state.isOnline ? t('categoryMap_intro') : t('categoryMap_introOffline')}</p>
      )}
    </>
  )
}

/**
 * A main category's terms in lines by subcategory, each term a Badge that opens "Editar termo"; a group's category
 * opens "Editar categoria" from its name, and every main one but Outros takes a new subcategory (9.4).
 */
function SectionCard({
  presenter,
  section,
  canCreateCategory,
  isDetail = false,
}: {
  presenter: CategoryMapPresenter
  section: JsMapSection
  canCreateCategory: boolean
  isDetail?: boolean
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center gap-3">
        <CategoryBadge category={section.category} />
        <div className="mr-auto flex flex-col">
          <CardTitle>{categoryName(section.category)}</CardTitle>
          <span className="text-sm text-muted-foreground">
            {sectionCount(section)}
            {isDetail && section.subcategoryCount > 0 && ` · ${t('categoryMap_subcategories', { count: section.subcategoryCount })}`}
          </span>
        </div>
        {canCreateCategory && section.category.isGroupCategory && (
          <Button variant="ghost" size="icon" aria-label={t('splitBill_categoryEdit', { name: categoryName(section.category) })} onClick={() => presenter.editCategory(section.category.id)}>
            <Pencil />
          </Button>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {section.rows.length === 0 && <p className="text-sm text-muted-foreground">{t('categoryMap_noTermsYet')}</p>}
        {section.rows.map((row) => (
          <div key={row.category.id} className="flex gap-3">
            <div className="flex w-32 shrink-0 flex-col pt-1 text-xs text-muted-foreground">
              {canCreateCategory && row.category.isGroupCategory && !row.isOnlyMain ? (
                <button
                  type="button"
                  className="self-start text-left underline-offset-2 hover:underline"
                  aria-label={t('splitBill_categoryEdit', { name: rowLabel(row) })}
                  onClick={() => presenter.editCategory(row.category.id)}
                >
                  {rowLabel(row)}
                </button>
              ) : (
                <span>{rowLabel(row)}</span>
              )}
              {row.groupName && <span className="text-primary">{t('categoryMap_onlyInGroup', { group: row.groupName })}</span>}
            </div>
            <div className="flex flex-1 flex-wrap gap-2">
              {row.terms.map((term) => (
                <button
                  key={term.key}
                  type="button"
                  onClick={() => presenter.openTerm(term.key)}
                  aria-label={termLabel(term.term, categoryPath(row.category, row.parent), term.isPending)}
                >
                  <Badge variant="outline" className="h-8 gap-1.5 px-3 text-sm font-medium">
                    {term.term}
                    {term.isPending && <PendingDot className="size-2" />}
                  </Badge>
                </button>
              ))}
            </div>
          </div>
        ))}
        <div className="flex flex-wrap gap-2">
          <Button variant="ghost" onClick={() => presenter.addTerm(section.category.id, '')}>
            <Plus /> {t('categoryMap_addIn', { name: categoryName(section.category) })}
          </Button>
          {canCreateCategory && !section.category.isOther && (
            <Button variant="ghost" onClick={() => presenter.createCategory(section.category.id)}>
              <FolderPlus /> {t('categoryMap_newSubcategoryIn', { name: categoryName(section.category) })}
            </Button>
          )}
        </div>
        {isDetail && <p className="text-xs text-muted-foreground">{t('categoryMap_tip')}</p>}
      </CardContent>
    </Card>
  )
}

function termLabel(term: string, path: string, isPending: boolean): string {
  const label = t('categoryMap_termAction', { term, path })
  return isPending ? `${label}, ${t('categoryMap_queued')}` : label
}

function SearchResults({ presenter, state }: { presenter: CategoryMapPresenter; state: JsCategoryMapState }) {
  const query = state.query.trim()
  if (state.showsNoResult) {
    return (
      <div className="flex flex-col items-center gap-2 py-10 text-center">
        <SearchX className="size-8 text-muted-foreground" />
        <p className="font-medium">{t('categoryMap_noResult', { query })}</p>
        <p className="max-w-sm text-sm text-muted-foreground">{t('categoryMap_noResultBody')}</p>
        <Button className="mt-2" onClick={() => presenter.addTerm(null, query)}>
          {t('categoryMap_addQuery', { query })}
        </Button>
      </div>
    )
  }
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-sm font-medium text-muted-foreground">
        {state.results.length === 1 ? t('categoryMap_resultsOne', { query }) : t('categoryMap_resultsMany', { count: state.results.length, query })}
      </h2>
      <div className="flex flex-col divide-y rounded-lg border bg-card">
        {state.results.map((result) => (
          <ResultRow key={result.term.key} presenter={presenter} result={result} />
        ))}
      </div>
      {state.results.length > 1 && <p className="text-xs text-muted-foreground">{t('categoryMap_longestWins')}</p>}
    </section>
  )
}

function ResultRow({ presenter, result }: { presenter: CategoryMapPresenter; result: JsMapSearchResult }) {
  const term = result.term.term
  return (
    <button type="button" onClick={() => presenter.openTerm(result.term.key)} className="flex items-center gap-3 p-3 text-left hover:bg-accent">
      <CategoryBadge category={(result.parent ?? result.category) as JsCategory} className="size-9" />
      <span className="flex flex-col">
        <span>
          {term.slice(0, result.matchStart)}
          <strong>{term.slice(result.matchStart, result.matchEnd)}</strong>
          {term.slice(result.matchEnd)}
        </span>
        <span className="text-xs text-muted-foreground">{categoryPath(result.category, result.parent)}</span>
      </span>
    </button>
  )
}

function EmptyMap({ presenter }: { presenter: CategoryMapPresenter }) {
  return (
    <div className="flex flex-col items-center gap-2 py-10 text-center">
      <Tag className="size-8 text-muted-foreground" />
      <p className="font-medium">{t('categoryMap_emptyTitle')}</p>
      <p className="max-w-sm text-sm text-muted-foreground">{t('categoryMap_emptyBody')}</p>
      <div className="mt-2 flex gap-2">
        <Button onClick={() => presenter.addTerm(null, '')}>{t('categoryMap_add')}</Button>
        <Button variant="outline" onClick={() => presenter.askToRestore()}>
          {t('categoryMap_restore')}
        </Button>
      </div>
    </div>
  )
}
