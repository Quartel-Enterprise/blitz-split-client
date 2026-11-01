import { Check, Minus, Plus } from 'lucide-react'
import type { DefaultSplitPresenter, JsCategory, JsDefaultSplitState, JsExceptionEditor, JsSplitException } from 'blitzsplit-shared'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { useDraft } from '@/hooks/useDraft'
import { t, type MessageKey } from '@/i18n'
import { CategoryBadge, categoryName } from '@/lib/categories'
import { formatMoney } from '@/lib/money'
import { cn } from '@/lib/utils'

const exceptionModes: Record<string, MessageKey> = {
  equal: 'defaultSplit_exceptionModeEqual',
  shares: 'defaultSplit_exceptionModeShares',
  percent: 'defaultSplit_exceptionModePercent',
}

/** "Exceções por categoria" (6.7f): each category that splits its own way, or "Nenhuma", and "Adicionar exceção". */
export function ExceptionsSection({ presenter, state }: { presenter: DefaultSplitPresenter; state: JsDefaultSplitState }) {
  return (
    <section aria-labelledby="default-split-exceptions" className="flex flex-col gap-2">
      <h3 id="default-split-exceptions" className="text-sm font-semibold text-muted-foreground">
        {t('defaultSplit_exceptions')}
      </h3>
      {state.exceptions.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t('defaultSplit_exceptionsNone')}</p>
      ) : (
        <ul className="flex flex-col gap-0.5">
          {state.exceptions.map((exception, index) => (
            <li key={exception.categoryId}>
              <button
                type="button"
                onClick={() => presenter.openException(exception.categoryId)}
                className={cn(
                  'flex min-h-[60px] w-full items-center gap-3 bg-card px-4 py-2 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                  index === 0 ? 'rounded-t-2xl' : 'rounded-t-md',
                  index === state.exceptions.length - 1 ? 'rounded-b-2xl' : 'rounded-b-md',
                )}
              >
                <CategoryBadge category={exception.category} />
                <span className="flex min-w-0 flex-col">
                  <span className="font-medium">{categoryName(exception.category)}</span>
                  <span className="text-sm text-muted-foreground">{exceptionSummary(exception)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <Button type="button" variant="ghost" className="self-start" onClick={() => presenter.addException()}>
        <Plus aria-hidden />
        {t('defaultSplit_exceptionAdd')}
      </Button>
    </section>
  )
}

/** "Igual entre todos", "Igual entre 2", "Partes, 2 para 1", "Porcentagem, 60% · 40%". */
function exceptionSummary(exception: JsSplitException): string {
  if (exception.mode === 'shares') {
    const values =
      exception.values.length === 2
        ? t('defaultSplit_exceptionSharesTwo', { first: exception.values[0], second: exception.values[1] })
        : exception.values.join(' · ')
    return t('defaultSplit_exceptionShares', { values })
  }
  if (exception.mode === 'percent') return t('defaultSplit_exceptionPercent', { values: exception.values.map((value) => `${value}%`).join(' · ') })
  return exception.includedCount >= exception.totalCount ? t('defaultSplit_exceptionEqualAll') : t('defaultSplit_exceptionEqualAmong', { count: exception.includedCount })
}

/** "Exceção por categoria": one category, its own way and who is in it, saved only on "Salvar exceção". */
export function ExceptionBody({ presenter, state, editor }: { presenter: DefaultSplitPresenter; state: JsDefaultSplitState; editor: JsExceptionEditor }) {
  return (
    <div className="flex flex-col gap-4">
      <span id="exception-category" className="text-sm font-semibold text-muted-foreground">
        {t('defaultSplit_exceptionCategory')}
      </span>
      <CategoryChips presenter={presenter} categories={state.exceptionMains} selectedId={editor.categoryId} openId={editor.openMainId} />
      {state.exceptionSubcategories.length > 0 && (
        <CategoryChips presenter={presenter} categories={state.exceptionSubcategories} selectedId={editor.categoryId} openId={null} />
      )}
      <span id="exception-how" className="text-sm font-semibold text-muted-foreground">
        {t('defaultSplit_exceptionHow')}
      </span>
      <div role="radiogroup" aria-labelledby="exception-how" className="grid grid-cols-3 gap-1">
        {state.exceptionModes.map((mode) => (
          <button
            key={mode}
            type="button"
            role="radio"
            aria-checked={editor.mode === mode}
            onClick={() => presenter.selectExceptionMode(mode)}
            className={cn(
              'flex h-11 items-center justify-center gap-1 rounded-md text-sm outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              editor.mode === mode ? 'rounded-full bg-primary font-semibold text-primary-foreground' : 'bg-muted font-medium',
            )}
          >
            {editor.mode === mode && <Check aria-hidden className="size-4" />}
            {t(exceptionModes[mode] ?? 'defaultSplit_exceptionModeEqual')}
          </button>
        ))}
      </div>
      <ul className="flex flex-col gap-0.5">
        {editor.rows.map((row) => (
          <li key={row.id} className="flex min-h-14 items-center gap-3 rounded-md bg-card px-3 py-1.5">
            {editor.mode === 'equal' && (
              <input
                type="checkbox"
                className="size-4 accent-primary"
                checked={row.isIncluded}
                aria-label={row.isMe ? t('splitBill_you') : row.name}
                onChange={() => presenter.toggleExceptionPerson(row.id)}
              />
            )}
            <Avatar initial={row.initials} color={row.color} className="size-8 text-xs" />
            <span className="min-w-0 flex-1 truncate">{row.isMe ? t('splitBill_you') : row.name}</span>
            {editor.mode === 'shares' && (
              <span className="flex items-center gap-1">
                <Button type="button" variant="secondary" size="icon" className="size-8" disabled={row.parts <= 0} aria-label={t('defaultSplit_less')} onClick={() => presenter.decreaseExceptionParts(row.id)}>
                  <Minus aria-hidden />
                </Button>
                <span className="w-7 text-center font-bold tabular-nums">{row.parts}</span>
                <Button type="button" variant="secondary" size="icon" className="size-8" aria-label={t('defaultSplit_more')} onClick={() => presenter.increaseExceptionParts(row.id)}>
                  <Plus aria-hidden />
                </Button>
              </span>
            )}
            {editor.mode === 'percent' && <PercentInput presenter={presenter} memberId={row.id} value={row.percentText} isError={editor.showsPercentError} />}
          </li>
        ))}
      </ul>
      <p role="status" className={cn('text-[13px]', editor.showsPercentError || state.isExceptionCategoryTaken ? 'text-destructive' : 'text-muted-foreground')}>
        {exceptionStatus(state, editor)}
      </p>
    </div>
  )
}

function PercentInput({ presenter, memberId, value, isError }: { presenter: DefaultSplitPresenter; memberId: string; value: string; isError: boolean }) {
  const [text, setText] = useDraft(value, (next) => {
    presenter.changeExceptionPercent(memberId, next)
    return presenter.getExceptionPercentText(memberId)
  })
  return (
    <span className="flex items-center gap-1">
      <input
        inputMode="numeric"
        value={text}
        aria-invalid={isError}
        onChange={(event) => setText(event.target.value)}
        className="h-9 w-16 rounded-md border bg-transparent px-2 text-right font-semibold tabular-nums"
      />
      <span aria-hidden>%</span>
    </span>
  )
}

function CategoryChips({
  presenter,
  categories,
  selectedId,
  openId,
}: {
  presenter: DefaultSplitPresenter
  categories: JsCategory[]
  selectedId: string | null | undefined
  openId: string | null | undefined
}) {
  return (
    <div role="radiogroup" aria-labelledby="exception-category" className="flex flex-wrap gap-2">
      {categories.map((category) => {
        const isSelected = category.id === selectedId
        return (
          <button
            key={category.id}
            type="button"
            role="radio"
            aria-checked={isSelected}
            onClick={() => presenter.pickExceptionCategory(category.id)}
            className={cn(
              'flex min-h-8 items-center gap-1.5 rounded-lg border px-3 text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              isSelected || category.id === openId ? 'border-transparent bg-secondary text-secondary-foreground' : 'border-border',
              isSelected && 'pl-2',
            )}
          >
            {isSelected && <Check aria-hidden className="size-4" />}
            {categoryName(category)}
          </button>
        )
      })}
    </div>
  )
}

/** What is missing, else "Numa conta de R$ 100,00 no Mercado: Pierre R$ 50,00 · Ana R$ 50,00". */
function exceptionStatus(state: JsDefaultSplitState, editor: JsExceptionEditor): string {
  const category = state.exceptionCategory
  if (!category) return t('defaultSplit_exceptionPick')
  if (state.isExceptionCategoryTaken) return t('defaultSplit_exceptionTaken', { name: categoryName(category) })
  if (editor.showsPercentError && editor.remainingPercent > 0) return t('defaultSplit_exceptionPercentLeft', { sum: editor.percentSum, left: editor.remainingPercent })
  if (editor.showsPercentError) return t('defaultSplit_exceptionPercentOver', { sum: editor.percentSum, over: -editor.remainingPercent })
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const parts = editor.rows
    .filter((row) => (row.exampleMinor ?? 0) > 0)
    .map((row) => `${row.isMe ? t('splitBill_you') : row.name} ${money(row.exampleMinor ?? 0)}`)
    .join(' · ')
  return t('defaultSplit_exceptionExample', { total: money(editor.exampleTotalMinor), name: categoryName(category), parts })
}
