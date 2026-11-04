import { Check, ChevronDown, ChevronRight, ChevronUp, MoreHorizontal, Plus, Search, SearchX, Tag, Users, X } from 'lucide-react'
import type {
  JsBillCategories,
  JsCategory,
  JsCategoryBranch,
  JsSplitBillState,
  SplitCategoriesPresenter,
} from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import type { PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { t, type MessageKey } from '@/i18n'
import { CategoryBadge, categoryName, categoryPath } from '@/lib/categories'
import { cn } from '@/lib/utils'

const searchId = 'category-search'

const reasons: Record<string, MessageKey | null> = {
  description: 'splitBill_categoryReasonDescription',
  'most-used': 'splitBill_categoryReasonMostUsed',
  group: 'splitBill_categoryReasonGroup',
  receipt: 'splitBill_categoryReasonReceipt',
  used: null,
}

/** The main category above a category of the bill's group, for "Alimentação › Mercado". */
function parentIn(categories: JsBillCategories, category: JsCategory): JsCategory | null {
  if (!category.parentId) return null
  return categories.branches.find((branch) => branch.category.id === category.parentId)?.category ?? null
}

/** The line under the description: what chose the bill's category (7.1, 7.1l to 7.1n). */
export function categoryHint(state: JsSplitBillState): string {
  const categories = state.categories
  const path = categoryPath(categories.category, categories.parent)
  const term = categories.matches[0]?.term ?? ''
  switch (categories.hint) {
    case 'by-term':
      return t('splitBill_categoryByTerm', { path, term })
    case 'by-receipt':
      return t('splitBill_categoryByReceipt', { path })
    case 'by-longest-term':
      return t('splitBill_categoryByLongest', { count: categories.matches.length, term, path })
    case 'no-term':
      return t('splitBill_categoryNoTerm')
    case 'changed':
      return t('splitBill_categoryChanged', {
        path,
        mapPath: categories.mapCategory ? categoryPath(categories.mapCategory, categories.mapParent) : '',
        term,
      })
    default:
      return t('splitBill_categoryPicked', { path })
  }
}

/**
 * "Categoria" (7.7) in the bill's place, as Dividir is: the search, "Sugeridas" and "Todas as categorias", each main
 * one opening its subcategories ("Só Alimentação", "Nova em Alimentação"). The group's own have a ⋯ to edit them.
 */
export function categoryPane(presenter: SplitCategoriesPresenter, state: JsSplitBillState, isWide: boolean): PaneParts {
  return {
    title: t('splitBill_categoryTitle'),
    body: <CategoryPicker presenter={presenter} state={state} isWide={isWide} />,
    footer: (
      <>
        <Button key="category-back" variant="outline" onClick={() => presenter.dismiss()}>
          {t('common_back')}
        </Button>
        <Button key="category-new" onClick={() => presenter.create(null, '')}>
          <Plus aria-hidden />
          {t('splitBill_categoryNew')}
        </Button>
      </>
    ),
  }
}

function CategoryPicker({ presenter, state, isWide }: { presenter: SplitCategoriesPresenter; state: JsSplitBillState; isWide: boolean }) {
  const categories = state.categories
  const [query, setQuery] = useDraft(categories.query, (text) => {
    presenter.changeQuery(text)
    return presenter.getQuery()
  })
  const open = categories.branches.find((branch) => branch.category.id === categories.openMainId) ?? null
  const trimmed = categories.query.trim()
  return (
    <div className={cn('grid gap-5', isWide && open && !categories.isSearching && 'md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] md:gap-8')}>
      <div className="flex min-w-0 flex-col gap-4">
        <div className="relative">
          <Label htmlFor={searchId} className="sr-only">
            {t('splitBill_categorySearch')}
          </Label>
          <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            id={searchId}
            role="searchbox"
            autoComplete="off"
            value={query}
            placeholder={t('splitBill_categorySearch')}
            className="rounded-full pr-10 pl-9"
            onChange={(event) => setQuery(event.target.value)}
          />
          {query && (
            <Button
              variant="ghost"
              size="icon"
              aria-label={t('splitBill_categoryClearSearch')}
              className="absolute top-1/2 right-1 size-8 -translate-y-1/2"
              onClick={() => setQuery('')}
            >
              <X aria-hidden />
            </Button>
          )}
        </div>
        {categories.isSearching ? (
          <SearchResults presenter={presenter} state={state} query={trimmed} />
        ) : (
          <>
            <Suggestions presenter={presenter} state={state} />
            <CategoryMapCard presenter={presenter} state={state} />
            <SectionLabel>{t('splitBill_categoryAll')}</SectionLabel>
            <ul className="flex flex-col gap-0.5">
              {categories.branches.map((branch, index) => (
                <BranchItem
                  key={branch.category.id}
                  presenter={presenter}
                  state={state}
                  branch={branch}
                  isFirst={index === 0}
                  isLast={index === categories.branches.length - 1}
                  showsInline={!isWide}
                />
              ))}
            </ul>
          </>
        )}
      </div>
      {isWide && open && !categories.isSearching && (
        <div className="hidden min-w-0 flex-col gap-4 rounded-3xl bg-muted/40 p-5 md:flex">
          <div className="flex items-center gap-3">
            <CategoryBadge category={open.category} />
            <div className="min-w-0">
              <p className="font-semibold">{categoryName(open.category)}</p>
              <p className="text-[13px] text-muted-foreground">{t('splitBill_categorySideHint', { name: categoryName(open.category) })}</p>
            </div>
          </div>
          <SubcategoryChips presenter={presenter} state={state} branch={open} />
          <p className="text-[13px] text-muted-foreground">{t('splitBill_categoryMenuHint')}</p>
        </div>
      )}
    </div>
  )
}

function SectionLabel({ children }: { children: ReactNode }) {
  return <h3 className="px-1 text-sm font-semibold text-muted-foreground">{children}</h3>
}

function Suggestions({ presenter, state }: { presenter: SplitCategoriesPresenter; state: JsSplitBillState }) {
  const categories = state.categories
  if (categories.suggestions.length === 0) return null
  return (
    <>
      <SectionLabel>{t('splitBill_categorySuggested')}</SectionLabel>
      <div role="listbox" aria-label={t('splitBill_categorySuggested')} className="flex flex-wrap gap-2">
        {categories.suggestions.map((suggestion) => {
          const reason = reasons[suggestion.reason]
          return (
            <Chip
              key={suggestion.category.id}
              isSelected={suggestion.isSelected}
              isGroupCategory={suggestion.category.isGroupCategory}
              onClick={() => presenter.pick(suggestion.category.id)}
            >
              <span className="flex flex-col items-start leading-tight">
                <span>{categoryName(suggestion.category)}</span>
                {reason && <span className="text-[11px] text-muted-foreground">{t(reason)}</span>}
              </span>
            </Chip>
          )
        })}
      </div>
      {categories.mostUsed && (
        <p className="text-[13px] text-muted-foreground">
          {t('splitBill_categoryMostUsedNote', { name: categoryName(categories.mostUsed), group: state.groupName })}
        </p>
      )}
    </>
  )
}

function Chip({
  isSelected,
  isGroupCategory,
  onClick,
  children,
}: {
  isSelected: boolean
  isGroupCategory: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      role="option"
      aria-selected={isSelected}
      onClick={onClick}
      className={cn(
        'flex min-h-8 items-center gap-1.5 rounded-lg border px-3 py-1 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
        isSelected ? 'border-transparent bg-secondary pl-2 text-secondary-foreground' : 'border-border',
      )}
    >
      {isSelected && <Check aria-hidden className="size-4" />}
      {children}
      {isGroupCategory && <Users aria-label={t('splitBill_categoryFromGroup')} className="size-3.5 text-muted-foreground" />}
    </button>
  )
}

function BranchItem({
  presenter,
  state,
  branch,
  isFirst,
  isLast,
  showsInline,
}: {
  presenter: SplitCategoriesPresenter
  state: JsSplitBillState
  branch: JsCategoryBranch
  isFirst: boolean
  isLast: boolean
  showsInline: boolean
}) {
  const categories = state.categories
  const category = branch.category
  const isOpen = categories.openMainId === category.id
  const selected = branch.subcategories.find((sub) => sub.id === categories.category.id)
  const creator = state.people.find((person) => person.id === category.createdByMemberId)
  const subtitle = selected
    ? categoryName(selected)
    : category.isGroupCategory
      ? creator
        ? t('splitBill_categoryCreatedByGroup', { name: creator.name })
        : t('splitBill_categoryFromGroup')
      : branch.subcategories.length === 0
        ? t('splitBill_categoryNoSubcategories')
        : branch.subcategories.length === 1
          ? t('splitBill_categorySubcategoriesOne')
          : t('splitBill_categorySubcategories', { count: branch.subcategories.length })
  return (
    <li
      className={cn(
        'flex flex-col bg-card',
        isFirst ? 'rounded-t-2xl' : 'rounded-t-md',
        isLast ? 'rounded-b-2xl' : 'rounded-b-md',
        isOpen && !showsInline && 'bg-secondary',
      )}
    >
      <div className="flex items-center gap-1 pr-2">
        <button
          type="button"
          aria-expanded={category.isOther ? undefined : isOpen}
          onClick={() => presenter.toggleMain(category.id)}
          className="flex min-h-[60px] min-w-0 flex-1 items-center gap-3 px-3 py-2 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
        >
          <CategoryBadge category={category} />
          <span className="min-w-0 flex-1">
            <span className={cn('flex items-center gap-1.5', isOpen && 'font-semibold')}>
              {categoryName(category)}
              {category.isGroupCategory && <Users aria-label={t('splitBill_categoryFromGroup')} className="size-3.5 text-muted-foreground" />}
            </span>
            <span className="block truncate text-[13px] text-muted-foreground">{subtitle}</span>
          </span>
          {!category.isOther && (isOpen ? <ChevronUp aria-hidden className="size-4" /> : <ChevronDown aria-hidden className="size-4" />)}
        </button>
        {category.isGroupCategory && <EditMenu presenter={presenter} category={category} />}
      </div>
      {isOpen && showsInline && <SubcategoryChips presenter={presenter} state={state} branch={branch} className="px-4 pb-3.5" />}
    </li>
  )
}

/** "⋯" on one of the group's categories: "Editar" (7.8e). */
function EditMenu({ presenter, category }: { presenter: SplitCategoriesPresenter; category: JsCategory }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={t('splitBill_categoryMoreOf', { name: categoryName(category) })}>
          <MoreHorizontal aria-hidden />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onSelect={() => presenter.edit(category.id)}>{t('splitBill_categoryEdit', { name: categoryName(category) })}</DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

function SubcategoryChips({
  presenter,
  state,
  branch,
  className,
}: {
  presenter: SplitCategoriesPresenter
  state: JsSplitBillState
  branch: JsCategoryBranch
  className?: string
}) {
  const selectedId = state.categories.category.id
  const mainName = categoryName(branch.category)
  return (
    <div role="listbox" aria-label={mainName} className={cn('flex flex-wrap gap-2', className)}>
      <Chip isSelected={selectedId === branch.category.id} isGroupCategory={false} onClick={() => presenter.pick(branch.category.id)}>
        {t('splitBill_categoryOnly', { name: mainName })}
      </Chip>
      {branch.subcategories.map((sub) => (
        <span key={sub.id} className="flex items-center">
          <Chip isSelected={selectedId === sub.id} isGroupCategory={sub.isGroupCategory} onClick={() => presenter.pick(sub.id)}>
            {categoryName(sub)}
          </Chip>
          {sub.isGroupCategory && <EditMenu presenter={presenter} category={sub} />}
        </span>
      ))}
      <button
        type="button"
        onClick={() => presenter.create(branch.category.id, '')}
        className="flex min-h-8 items-center gap-1.5 rounded-lg border border-dashed border-muted-foreground px-3 pl-2 text-sm font-medium text-primary outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
      >
        <Plus aria-hidden className="size-4" />
        {t('splitBill_categoryNewIn', { name: mainName })}
      </button>
    </div>
  )
}

function SearchResults({ presenter, state, query }: { presenter: SplitCategoriesPresenter; state: JsSplitBillState; query: string }) {
  const categories = state.categories
  if (categories.showsNoneFound) {
    return (
      <div role="status" className="flex flex-col items-center gap-2 py-6 text-center">
        <SearchX aria-hidden className="size-10 text-muted-foreground" />
        <p className="font-semibold">{t('splitBill_categoryNoneTitle', { query })}</p>
        <p className="text-sm text-muted-foreground">{t('splitBill_categoryNoneText', { group: state.groupName })}</p>
        <Button onClick={() => presenter.create(null, query)}>
          <Plus aria-hidden />
          {t('splitBill_categoryCreateShort', { query })}
        </Button>
      </div>
    )
  }
  const count = categories.results.length
  return (
    <>
      <SectionLabel>{count === 1 ? t('splitBill_categoryResultsOne') : t('splitBill_categoryResults', { count })}</SectionLabel>
      <ul role="listbox" aria-label={t('splitBill_categorySearch')} className="flex flex-col gap-0.5">
        {categories.results.map((category, index) => {
          const parent = parentIn(categories, category)
          return (
            <li
              key={category.id}
              className={cn('flex bg-card', index === 0 ? 'rounded-t-2xl' : 'rounded-t-md', index === count - 1 ? 'rounded-b-2xl' : 'rounded-b-md')}
            >
              <button
                type="button"
                role="option"
                aria-selected={categories.category.id === category.id}
                onClick={() => presenter.pick(category.id)}
                className="flex min-h-[60px] flex-1 items-center gap-3 px-3 py-2 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring"
              >
                <CategoryBadge category={category} />
                <span className="min-w-0">
                  <span className="block">{categoryName(category)}</span>
                  {parent && <span className="block text-[13px] text-muted-foreground">{t('splitBill_categoryIn', { name: categoryName(parent) })}</span>}
                </span>
              </button>
            </li>
          )
        })}
      </ul>
      <Button variant="ghost" className="self-start" onClick={() => presenter.create(null, query)}>
        <Plus aria-hidden />
        {t('splitBill_categoryCreateQuery', { query })}
      </Button>
    </>
  )
}

/** "Mapa de categorias" between Sugeridas and Todas as categorias (9.0): which word chose the category, and the way to the map. */
function CategoryMapCard({ presenter, state }: { presenter: SplitCategoriesPresenter; state: JsSplitBillState }) {
  const match = state.categories.matches[0]
  const mapCategory = state.categories.mapCategory
  return (
    <button type="button" onClick={() => presenter.openMap()} className="flex items-center gap-3 rounded-lg border bg-card p-3 text-left hover:bg-accent">
      <Tag aria-hidden className="size-5 shrink-0 text-primary" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="text-sm font-medium">{t('splitBill_categoryMap')}</span>
        <span className="text-xs text-muted-foreground">
          {match && mapCategory ? t('splitBill_categoryMapByTerm', { name: categoryName(mapCategory), term: match.term }) : t('splitBill_categoryMapDetail')}
        </span>
      </span>
      <ChevronRight aria-hidden className="size-4 text-muted-foreground" />
    </button>
  )
}
