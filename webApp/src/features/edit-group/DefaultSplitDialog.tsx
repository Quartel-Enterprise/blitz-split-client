import { ArrowLeft, CircleAlert, Loader2, Lock, Minus, Plus, UserPlus } from 'lucide-react'
import { DefaultSplitPresenter, type JsDefaultSplitRow, type JsDefaultSplitState, type JsRoute } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { avatarStyle } from '@/lib/avatar'
import { uiTag } from '@/lib/currency'
import { amountPlaceholder, currencySymbol, formatMoney, formatNames, formatPercent } from '@/lib/money'
import { cn } from '@/lib/utils'

const modes: Record<string, { label: MessageKey; hint: MessageKey; list: MessageKey }> = {
  equal: { label: 'defaultSplit_modeEqual', hint: 'defaultSplit_hintEqual', list: 'defaultSplit_listEqual' },
  shares: { label: 'defaultSplit_modeShares', hint: 'defaultSplit_hintShares', list: 'defaultSplit_listShares' },
  percent: { label: 'defaultSplit_modePercent', hint: 'defaultSplit_hintPercent', list: 'defaultSplit_listPercent' },
  income: { label: 'defaultSplit_modeIncome', hint: 'defaultSplit_hintIncome', list: 'defaultSplit_listIncome' },
}

/**
 * "Divisão padrão" (6.7), in Editar grupo's place: a dialog on desktop and tablet (two columns from 1024 px, the
 * ways to split on the left and the people with the example on the right), a drawer on the phone. Back, Cancel and
 * closing it any way go back to Editar grupo; Salvar saves and goes back too.
 */
export function DefaultSplitDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new DefaultSplitPresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsDefaultSplitState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const isWide = useMediaQuery('(min-width: 1024px)')
  if (!presenter || !state?.isLoaded) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  const subtitle = t('defaultSplit_subtitle', { group: state.groupName })
  const back = (
    <Button type="button" variant="ghost" size="icon" aria-label={t('defaultSplit_back')} disabled={state.isSaving} onClick={() => presenter.back()}>
      <ArrowLeft />
    </Button>
  )
  const save = (
    <Button type="button" disabled={!state.canSave} onClick={() => presenter.save()} className={cn(!isDesktop && 'h-11')}>
      {state.isSaving && <Loader2 className="animate-spin" />}
      {t('defaultSplit_save')}
    </Button>
  )
  const cancel = (
    <Button type="button" variant="outline" disabled={state.isSaving} onClick={() => presenter.back()} className={cn(!isDesktop && 'h-11')}>
      {t('common_cancel')}
    </Button>
  )
  const body = (
    <div className={cn('grid gap-5', isWide && 'grid-cols-2 gap-6')}>
      <div className="flex flex-col gap-5">
        <Notices state={state} />
        <ModePicker presenter={presenter} state={state} />
      </div>
      <div className="flex flex-col gap-4">
        <People presenter={presenter} state={state} />
        <Example state={state} />
        <p className="text-[13px] text-muted-foreground">{t(state.showsOfflineNotice ? 'defaultSplit_offlineNote' : 'defaultSplit_note')}</p>
      </div>
    </div>
  )

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className={cn('max-h-[calc(100dvh-2rem)] overflow-y-auto', isWide ? 'sm:max-w-[820px]' : 'sm:max-w-[560px]')}>
        <DialogHeader className="flex-row items-center gap-2 pr-8 text-left">
          {back}
          <span className="flex flex-col gap-0.5">
            <DialogTitle>{t('defaultSplit_title')}</DialogTitle>
            <DialogDescription>{subtitle}</DialogDescription>
          </span>
        </DialogHeader>
        {body}
        <DialogFooter>
          {cancel}
          {save}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="flex-row items-center gap-2 group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          {back}
          <span className="flex flex-col gap-0.5">
            <DrawerTitle>{t('defaultSplit_title')}</DrawerTitle>
            <DrawerDescription>{subtitle}</DrawerDescription>
          </span>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{body}</div>
        <DrawerFooter>
          {save}
          {cancel}
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

/** The save error and "Carla entrou no grupo. Começa com 1 parte; ajuste se precisar.", for one or more people. */
function Notices({ state }: { state: JsDefaultSplitState }) {
  const names = state.joinedPeople
  const isOne = names.length === 1
  const start =
    state.mode === 'shares'
      ? t(isOne ? 'defaultSplit_startSharesOne' : 'defaultSplit_startSharesOther')
      : state.mode === 'percent' || state.mode === 'income'
        ? t(isOne ? 'defaultSplit_startPercentOne' : 'defaultSplit_startPercentOther', { zero: formatPercent(0) })
        : t(isOne ? 'defaultSplit_startEqualOne' : 'defaultSplit_startEqualOther')
  return (
    <>
      {state.showsSaveError && (
        <p role="alert" className="flex items-center gap-1.5 text-sm text-destructive">
          <CircleAlert className="size-4 shrink-0" />
          {t('defaultSplit_saveError')}
        </p>
      )}
      {names.length > 0 && (
        <div role="status" className="flex items-start gap-3 rounded-lg bg-primary/10 px-4 py-3 text-sm">
          <UserPlus aria-hidden className="mt-0.5 size-4 shrink-0 text-primary" />
          <span>
            {t('defaultSplit_joinedSentence', {
              joined: t(isOne ? 'defaultSplit_joinedOne' : 'defaultSplit_joinedOther', { names: formatNames(Array.from(names)) }),
              start,
            })}
          </span>
        </div>
      )}
    </>
  )
}

/** "Como dividir": a radio group of cards, one way for the whole group. */
function ModePicker({ presenter, state }: { presenter: DefaultSplitPresenter; state: JsDefaultSplitState }) {
  return (
    <div className="flex flex-col gap-2">
      <span id="default-split-modes" className="text-sm font-medium">
        {t('defaultSplit_modes')}
      </span>
      <div role="radiogroup" aria-labelledby="default-split-modes" className="flex flex-col gap-2">
        {state.modes.map((mode) => {
          const isSelected = mode === state.mode
          const copy = modes[mode] ?? modes.equal
          return (
            <button
              key={mode}
              type="button"
              role="radio"
              aria-checked={isSelected}
              onClick={() => presenter.selectMode(mode)}
              className={cn(
                'flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
                isSelected ? 'border-primary bg-primary/5' : 'hover:bg-accent/60',
              )}
            >
              <span aria-hidden className={cn('flex size-4 shrink-0 items-center justify-center rounded-full border', isSelected ? 'border-primary' : 'border-muted-foreground')}>
                {isSelected && <span className="size-2 rounded-full bg-primary" />}
              </span>
              <span className="flex flex-col gap-0.5">
                <span className="text-sm font-medium">{t(copy.label)}</span>
                <span className="text-[13px] text-muted-foreground">{t(copy.hint, { first: formatPercent(75), second: formatPercent(25) })}</span>
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}

function People({ presenter, state }: { presenter: DefaultSplitPresenter; state: JsDefaultSplitState }) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium">{t((modes[state.mode] ?? modes.equal).list)}</span>
      <ul className="flex flex-col divide-y overflow-hidden rounded-lg border">
        {state.rows.map((row) => (
          <PersonRow key={row.id} presenter={presenter} state={state} row={row} />
        ))}
      </ul>
      {state.mode === 'income' && (
        <p className="flex items-start gap-2 text-[13px] text-muted-foreground">
          <Lock aria-hidden className="mt-0.5 size-3.5 shrink-0" />
          {t('defaultSplit_incomeNote')}
        </p>
      )}
    </div>
  )
}

function PersonRow({ presenter, state, row }: { presenter: DefaultSplitPresenter; state: JsDefaultSplitState; row: JsDefaultSplitRow }) {
  const name = row.isMe ? t('defaultSplit_me', { name: row.name }) : row.name
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const share = row.basisPoints / 10000
  let line: string
  switch (state.mode) {
    case 'shares':
      line = t('defaultSplit_rowShares', { percent: formatPercent(Math.round(share * 100)) })
      break
    case 'percent':
      line = t('defaultSplit_rowPercent', { amount: money(row.exampleMinor ?? 0), total: money(state.exampleTotalMinor) })
      break
    case 'income':
      line = t('defaultSplit_rowIncome', { percent: formatPercent(row.weight) })
      break
    default:
      line =
        row.weight > 0
          ? t('defaultSplit_rowEqual', {
              count: state.includedCount,
              percent: new Intl.NumberFormat(uiTag, { style: 'percent', maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(share),
            })
          : t('defaultSplit_rowOut')
  }
  if (row.isNew) line = t('defaultSplit_rowNew', { line })
  const content = (
    <>
      {state.mode === 'equal' && (
        <span aria-hidden className={cn('flex size-4 shrink-0 items-center justify-center rounded-sm border', row.isIncluded ? 'border-primary bg-primary text-primary-foreground' : 'border-muted-foreground')}>
          {row.isIncluded && <span className="text-[10px] leading-none">✓</span>}
        </span>
      )}
      <Avatar initial={row.initials} color={row.color} className="size-8 text-xs" />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{name}</span>
        <span className="text-[13px] text-muted-foreground tabular-nums">{line}</span>
      </span>
    </>
  )
  if (state.mode === 'equal') {
    return (
      <li>
        <button
          type="button"
          role="checkbox"
          aria-checked={row.isIncluded}
          aria-label={t('defaultSplit_includes', { name })}
          onClick={() => presenter.togglePerson(row.id)}
          className="flex w-full items-center gap-3 px-3 py-2.5 text-left outline-none hover:bg-accent/60 focus-visible:bg-accent/60"
        >
          {content}
        </button>
      </li>
    )
  }
  return (
    <li className="flex items-center gap-3 px-3 py-2.5">
      {content}
      {state.mode === 'shares' && (
        <span role="group" aria-label={t('defaultSplit_partsOf', { name })} className="flex shrink-0 items-center gap-1">
          <Button variant="outline" size="icon" className="size-8" aria-label={t('defaultSplit_less')} disabled={row.parts <= 0} onClick={() => presenter.decreaseParts(row.id)}>
            <Minus />
          </Button>
          <span className="w-7 text-center font-semibold tabular-nums">{row.parts}</span>
          <Button variant="outline" size="icon" className="size-8" aria-label={t('defaultSplit_more')} onClick={() => presenter.increaseParts(row.id)}>
            <Plus />
          </Button>
        </span>
      )}
      {state.mode === 'percent' && (
        <NumberField
          value={row.percentText}
          label={t('defaultSplit_percentOf', { name })}
          suffix="%"
          placeholder="0"
          isInvalid={state.showsPercentError}
          onChange={(text) => {
            presenter.changePercent(row.id, text)
            return presenter.getPercentText(row.id)
          }}
          className="w-20"
        />
      )}
      {state.mode === 'income' && (
        <NumberField
          value={row.incomeText}
          label={t('defaultSplit_incomeOf', { name })}
          prefix={currencySymbol(state.currency)}
          placeholder={amountPlaceholder(state.fractionDigits)}
          isInvalid={false}
          onChange={(text) => {
            presenter.changeIncome(row.id, text)
            return presenter.getIncomeText(row.id)
          }}
          className="w-36"
        />
      )}
    </li>
  )
}

function NumberField({
  value,
  label,
  prefix,
  suffix,
  placeholder,
  isInvalid,
  onChange,
  className,
}: {
  value: string
  label: string
  prefix?: string
  suffix?: string
  placeholder: string
  isInvalid: boolean
  onChange: (value: string) => string
  className?: string
}) {
  const [draft, setDraft] = useDraft(value, onChange)
  return (
    <label
      className={cn(
        'flex h-9 shrink-0 items-center gap-1.5 rounded-md border bg-card px-2.5 text-sm focus-within:ring-[3px] dark:bg-input/30',
        isInvalid ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
        className,
      )}
    >
      {prefix && (
        <span aria-hidden className="text-muted-foreground">
          {prefix}
        </span>
      )}
      <input
        inputMode={prefix ? 'decimal' : 'numeric'}
        autoComplete="off"
        value={draft}
        placeholder={placeholder}
        aria-label={label}
        aria-invalid={isInvalid}
        onChange={(event) => setDraft(event.target.value)}
        className="min-w-0 flex-1 bg-transparent text-right font-semibold tabular-nums outline-none placeholder:text-muted-foreground/60"
      />
      {suffix && (
        <span aria-hidden className="text-muted-foreground">
          {suffix}
        </span>
      )}
    </label>
  )
}

/** The example bill with each one's part and the bar in their colors, or what is missing while it doesn't add up. */
function Example({ state }: { state: JsDefaultSplitState }) {
  const money = (minor: number) => formatMoney(minor, state.currency, state.fractionDigits)
  const isError = state.showsPercentError
  const total = money(state.exampleTotalMinor)
  const title = isError
    ? t(state.remainingPercent > 0 ? 'defaultSplit_sumLeft' : 'defaultSplit_sumOver', {
        sum: formatPercent(state.percentSum),
        gap: formatPercent(Math.abs(state.remainingPercent)),
      })
    : state.mode === 'equal' && state.includedCount > 0
      ? t('defaultSplit_exampleEqual', { total, each: money(state.exampleEachMinor) })
      : t('defaultSplit_example', { total })
  const detail = isError
    ? t('defaultSplit_sumHelp', { full: formatPercent(100) })
    : state.mode === 'equal'
      ? t('defaultSplit_exampleEqualHelp')
      : state.rows
          .filter((row) => row.weight > 0)
          .map((row) => `${row.name} ${money(row.exampleMinor ?? 0)}`)
          .join(' · ')
  const parts = state.rows.filter((row) => row.basisPoints > 0)
  const missing = isError && state.remainingPercent > 0 ? state.remainingPercent * 100 : 0
  return (
    <div role="status" className={cn('flex flex-col gap-2 rounded-lg border px-4 py-3', isError && 'border-destructive text-destructive')}>
      <strong className="flex items-center gap-1.5 font-semibold tabular-nums">
        {isError && <CircleAlert aria-hidden className="size-4 shrink-0" />}
        {title}
      </strong>
      <span aria-hidden className="flex h-2 gap-0.5 overflow-hidden rounded-full">
        {parts.map((row) => (
          <span key={row.id} className="avatar rounded-full" style={{ ...avatarStyle(row.color), flex: `${row.basisPoints} 1 0` }} />
        ))}
        {missing > 0 && <span className="rounded-full bg-muted" style={{ flex: `${missing} 1 0` }} />}
      </span>
      <span className="text-[13px] text-muted-foreground tabular-nums">{detail}</span>
    </div>
  )
}
