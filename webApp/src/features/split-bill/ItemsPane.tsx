import { X } from 'lucide-react'
import type { JsFeeEditor, JsItemEditor, JsItemPersonLine, JsItemsEditor, JsSplitBillState, SplitItemsPresenter } from 'blitzsplit-shared'
import { Avatar } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { ResponsiveDialog, type PaneParts } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { t } from '@/i18n'
import { amountPlaceholder, currencySymbol, formatPercent } from '@/lib/money'
import { cn } from '@/lib/utils'
import {
  itemDeletionText,
  itemHints,
  itemListLabels,
  itemModeLabels,
  itemRowSubtitle,
  itemStatusText,
  itemsPersonSubtitle,
  itemsStatusBody,
  itemsStatusTitle,
} from './itemsText'
import { personName, type Money } from './splitText'

const faceCount = 3

/**
 * "Itens da conta" (7.5) in Dividir's place: a 920 px dialog with the items on the left and what each one pays on
 * the right, one column on a tablet, a drawer on a phone. "Voltar" goes back to Dividir, which keeps the items typed;
 * "Pronto" waits until every item has someone and it all closes with the total.
 */
export function itemsPane(presenter: SplitItemsPresenter, state: JsSplitBillState, items: JsItemsEditor, lead: string, money: Money, signed: Money): PaneParts {
  return {
    title: t('items_title'),
    description: lead,
    body: <ItemsBody presenter={presenter} state={state} items={items} money={money} signed={signed} />,
    footer: (
      <>
        <Button key="items-back" variant="outline" onClick={() => presenter.dismiss()}>
          {t('common_back')}
        </Button>
        <Button key="items-done" disabled={!items.addsUp} onClick={() => presenter.confirm()}>
          {t('splitBill_done')}
        </Button>
      </>
    ),
  }
}

function ItemsBody({ presenter, state, items, money, signed }: { presenter: SplitItemsPresenter; state: JsSplitBillState; items: JsItemsEditor; money: Money; signed: Money }) {
  const status = items.status
  const isOk = items.addsUp
  const showsDifference = status.kind === 'left' || status.kind === 'over'
  return (
    <div className="flex flex-col gap-4">
      <div role="status" className={cn('flex flex-col gap-2 rounded-lg border px-4 py-3', !isOk && 'border-destructive text-destructive')}>
        <strong className="font-semibold">{itemsStatusTitle(status, money)}</strong>
        <span className="text-[13px] text-muted-foreground">{itemsStatusBody(status, money, signed)}</span>
        {showsDifference && (
          <span className="flex flex-wrap gap-2">
            {items.canAddDifferenceItem && (
              <Button size="sm" variant="destructive" onClick={() => presenter.addDifferenceItem()}>
                {t('items_addDifference', { amount: money(items.remainingMinor) })}
              </Button>
            )}
            <Button size="sm" variant="destructive" disabled={!items.canSplitDifference} onClick={() => presenter.splitDifference()}>
              {t('items_splitDifference')}
            </Button>
          </span>
        )}
      </div>
      {state.showsOfflineNotice && (
        <p role="status" className="flex items-start gap-2 text-[13px] text-muted-foreground">
          <span aria-hidden className="mt-1.5 size-2 shrink-0 rounded-full bg-amber-600" />
          <span>{t('items_offline')}</span>
        </p>
      )}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
        <div className="flex min-w-0 flex-col gap-2">
          <div className="flex min-h-8 items-center justify-between gap-2">
            <span className="text-sm font-medium">{t('items_count', { count: items.rows.length })}</span>
            <Button variant="link" size="sm" className="h-8 px-0" disabled={!items.canAddItem} onClick={() => presenter.addItem()}>
              {t('items_add')}
            </Button>
          </div>
          <div role="table" aria-label={t('items_table')} className="flex flex-col divide-y overflow-hidden rounded-lg border">
            {items.rows.map((row) => (
              <button
                key={row.id}
                type="button"
                role="row"
                onClick={() => presenter.openItem(row.id)}
                className={cn('flex min-h-12 items-center gap-3 px-3 py-1.5 text-left hover:bg-accent/40', row.isLoose && 'bg-destructive/10')}
              >
                <span role="cell" className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate font-medium">{row.name}</span>
                  <span className={cn('truncate text-xs', row.isLoose ? 'text-destructive' : 'text-muted-foreground')}>{itemRowSubtitle(row, money)}</span>
                </span>
                <span aria-hidden className="flex -space-x-2">
                  {row.consumers.slice(0, faceCount).map((person) => (
                    <Avatar key={person.id} initial={person.initials} color={person.color} className="size-6 border-2 border-card text-[9px]" />
                  ))}
                </span>
                <span role="cell" className="w-24 shrink-0 text-right font-mono text-[13px] tabular-nums">
                  {money(row.totalMinor)}
                </span>
              </button>
            ))}
            <FeeRow
              title={t('items_service', { percent: formatPercent(items.servicePercent) })}
              subtitle={t('items_serviceSub')}
              amount={money(items.serviceMinor)}
              onClick={() => presenter.openFee('service')}
            />
            <FeeRow
              title={t('items_couvert')}
              subtitle={t('items_couvertSub', { amount: money(items.couvertMinor) })}
              amount={money(items.couvertTotalMinor)}
              onClick={() => presenter.openFee('couvert')}
            />
            {items.showsExtra && (
              <div role="row" className="flex min-h-12 items-center gap-3 px-3 py-1.5">
                <span role="cell" className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate font-medium">{t('items_extra')}</span>
                  <span className="truncate text-xs text-muted-foreground">{t('items_serviceSub')}</span>
                </span>
                <span role="cell" className="font-mono text-[13px] tabular-nums">
                  {signed(items.extraMinor)}
                </span>
                <Button variant="ghost" size="icon-sm" aria-label={t('items_extraRemove')} onClick={() => presenter.removeDifference()}>
                  <X />
                </Button>
              </div>
            )}
          </div>
        </div>
        <div className="flex min-w-0 flex-col gap-2">
          <span className="flex min-h-8 items-center text-sm font-medium">{t('items_people')}</span>
          <ul className="flex flex-col divide-y overflow-hidden rounded-lg border">
            {items.people.map((row) => (
              <li key={row.person.id} className="flex min-h-12 items-center gap-2.5 px-3 py-1.5">
                <Avatar initial={row.person.initials} color={row.person.color} className="size-7 text-[11px]" />
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="truncate font-medium">{personName(row.person)}</span>
                  <span className="truncate text-xs text-muted-foreground">{itemsPersonSubtitle(row, money)}</span>
                </span>
                <span className="font-mono text-[13px] font-semibold tabular-nums">{money(row.totalMinor)}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}

function FeeRow({ title, subtitle, amount, onClick }: { title: string; subtitle: string; amount: string; onClick: () => void }) {
  return (
    <button type="button" role="row" onClick={onClick} className="flex min-h-12 items-center gap-3 bg-muted/40 px-3 py-1.5 text-left hover:bg-accent/40">
      <span role="cell" className="flex min-w-0 flex-1 flex-col">
        <span className="truncate font-medium">{title}</span>
        <span className="truncate text-xs text-muted-foreground">{subtitle}</span>
      </span>
      <span role="cell" className="w-24 shrink-0 text-right font-mono text-[13px] tabular-nums">
        {amount}
      </span>
    </button>
  )
}

/**
 * "Item" (7.6): a 560 px dialog over "Itens da conta", in its place on a phone's drawer. Name, quantity and price,
 * the item's total, its modes, whether it closes and who consumed it; "Pronto" waits until it does.
 */
export function itemPane(presenter: SplitItemsPresenter, state: JsSplitBillState, editor: JsItemEditor, money: Money): PaneParts {
  return {
    title: t('item_title'),
    description: state.description.trim() || undefined,
    body: <ItemBody presenter={presenter} state={state} editor={editor} money={money} />,
    footer: (
      <>
        {editor.itemId != null && (
          <Button key="item-delete" variant="ghost" className="text-destructive sm:mr-auto" onClick={() => presenter.deleteItem(editor.itemId ?? '')}>
            {t('item_delete')}
          </Button>
        )}
        <Button key="item-back" variant="outline" onClick={() => presenter.dismissItem()}>
          {t('common_back')}
        </Button>
        <Button key="item-done" disabled={!editor.canSave} onClick={() => presenter.confirmItem()}>
          {t('splitBill_done')}
        </Button>
      </>
    ),
  }
}

function ItemBody({ presenter, state, editor, money }: { presenter: SplitItemsPresenter; state: JsSplitBillState; editor: JsItemEditor; money: Money }) {
  const [name, setName] = useDraft(editor.name, (next) => {
    presenter.changeName(next)
    return presenter.getName()
  })
  const [quantity, setQuantity] = useDraft(String(editor.quantity), (next) => {
    presenter.changeQuantity(next)
    return presenter.getQuantity()
  })
  const [price, setPrice] = useDraft(editor.unitPriceText, (next) => {
    presenter.changePrice(next)
    return presenter.getPrice()
  })
  const status = itemStatusText(editor.status, money)
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-[minmax(0,1fr)_80px_120px] gap-3 max-sm:grid-cols-[80px_minmax(0,1fr)]">
        <div className="flex flex-col gap-1.5 max-sm:col-span-2">
          <Label htmlFor="item-name">{t('item_name')}</Label>
          <Input id="item-name" value={name} autoComplete="off" onChange={(event) => setName(event.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="item-quantity">{t('item_quantity')}</Label>
          <Input id="item-quantity" inputMode="numeric" value={quantity} autoComplete="off" onChange={(event) => setQuantity(event.target.value)} className="text-right tabular-nums" />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="item-price">{t('item_price')}</Label>
          <Input
            id="item-price"
            inputMode="decimal"
            value={price}
            placeholder={amountPlaceholder(state.fractionDigits)}
            autoComplete="off"
            onChange={(event) => setPrice(event.target.value)}
            className="text-right tabular-nums"
          />
        </div>
      </div>
      <span className="flex items-baseline justify-between text-sm">
        <span className="text-muted-foreground">{t('item_total')}</span>
        <strong className="font-mono tabular-nums">{money(editor.totalMinor)}</strong>
      </span>
      <div
        role="radiogroup"
        aria-label={t('item_modes')}
        className="grid h-9 rounded-lg bg-muted p-[3px] text-muted-foreground"
        style={{ gridTemplateColumns: `repeat(${editor.modes.length}, minmax(0, 1fr))` }}
      >
        {editor.modes.map((mode) => (
          <button
            key={mode}
            type="button"
            role="radio"
            aria-checked={editor.mode === mode}
            onClick={() => presenter.changeMode(mode)}
            className={cn(
              'flex items-center justify-center rounded-md text-sm font-medium outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
              editor.mode === mode ? 'bg-background text-foreground shadow-sm dark:bg-input/30' : 'hover:text-foreground',
            )}
          >
            {t(itemModeLabels[mode] ?? 'item_modeEqual')}
          </button>
        ))}
      </div>
      <div role="status" className={cn('flex flex-col gap-2 rounded-lg border px-4 py-3', editor.isStatusError && 'border-destructive text-destructive')}>
        <strong className="font-semibold">{status.title}</strong>
        {editor.progress != null && (
          <span aria-hidden className="flex h-1.5 overflow-hidden rounded-full bg-muted">
            <span className={cn('rounded-full', editor.isStatusError ? 'bg-destructive' : 'bg-primary')} style={{ width: `${editor.progress}%` }} />
          </span>
        )}
        <span className="text-[13px] text-muted-foreground">{status.body}</span>
      </div>
      <div className="flex flex-col gap-2">
        <span className="text-sm font-medium">{t(itemListLabels[editor.mode] ?? 'item_listEqual')}</span>
        <ul className="flex flex-col divide-y overflow-hidden rounded-lg border">
          {editor.lines.map((line) => (
            <ItemPersonRow key={line.person.id} presenter={presenter} editor={editor} line={line} state={state} money={money} />
          ))}
        </ul>
      </div>
      <p className="text-[13px] text-muted-foreground">{t(itemHints[editor.mode] ?? 'item_hintEqual', { zero: money(0) })}</p>
    </div>
  )
}

function ItemPersonRow({ presenter, editor, line, state, money }: { presenter: SplitItemsPresenter; editor: JsItemEditor; line: JsItemPersonLine; state: JsSplitBillState; money: Money }) {
  const person = line.person
  const isIn = line.shareMinor != null || line.isIncluded || line.count > 0 || line.valueText !== ''
  const content = (
    <>
      {editor.mode === 'equal' && (
        <input
          type="checkbox"
          checked={line.isIncluded}
          onChange={() => presenter.togglePerson(person.id)}
          aria-label={t('item_personHad', { name: person.name })}
          className={cn('size-4 shrink-0 accent-primary', editor.isStatusError && 'outline outline-destructive')}
        />
      )}
      <Avatar initial={person.initials} color={person.color} className={cn('size-7 text-[11px]', !isIn && 'opacity-50')} />
      <span className="flex min-w-0 flex-1 flex-col">
        <span className={cn('truncate font-medium', !isIn && 'text-muted-foreground')}>{personName(person)}</span>
        <span className={cn('truncate font-mono text-xs tabular-nums', line.isError ? 'text-destructive' : 'text-muted-foreground')}>
          {line.shareMinor != null ? money(line.shareMinor) : '—'}
        </span>
      </span>
      {editor.mode !== 'equal' && <PersonField presenter={presenter} editor={editor} line={line} state={state} />}
    </>
  )
  return (
    <li>
      {editor.mode === 'equal' ? (
        <label className="flex min-h-12 cursor-pointer items-center gap-2.5 px-3 py-1.5 hover:bg-accent/40">{content}</label>
      ) : (
        <div className="flex min-h-12 items-center gap-2.5 px-3 py-1.5">{content}</div>
      )}
    </li>
  )
}

const fieldLabels = {
  units: 'item_personUnits',
  shares: 'item_personShares',
  amount: 'item_personAmount',
  percent: 'item_personPercent',
} as const

function PersonField({ presenter, editor, line, state }: { presenter: SplitItemsPresenter; editor: JsItemEditor; line: JsItemPersonLine; state: JsSplitBillState }) {
  const id = line.person.id
  const counted = editor.mode === 'units' || editor.mode === 'shares'
  const shown = counted ? (line.count > 0 ? String(line.count) : '') : line.valueText
  const [value, setValue] = useDraft(shown, (next) => {
    presenter.changePersonValue(id, next)
    return presenter.getPersonValue(id)
  })
  const label = t(fieldLabels[editor.mode as keyof typeof fieldLabels] ?? 'item_personAmount', { name: line.person.name })
  return (
    <span
      className={cn(
        'flex h-8 shrink-0 items-center gap-1 rounded-md border bg-card px-2 focus-within:ring-[3px] dark:bg-input/30',
        line.isError ? 'border-destructive focus-within:ring-destructive/20' : 'focus-within:border-ring focus-within:ring-ring/50',
        editor.mode === 'amount' ? 'w-28' : 'w-[72px]',
      )}
    >
      {editor.mode === 'amount' && (
        <span aria-hidden className="text-xs text-muted-foreground">
          {currencySymbol(state.currency)}
        </span>
      )}
      <input
        inputMode={editor.mode === 'amount' ? 'decimal' : 'numeric'}
        autoComplete="off"
        value={value}
        placeholder={editor.mode === 'amount' ? amountPlaceholder(state.fractionDigits) : '0'}
        aria-label={label}
        aria-invalid={line.isError}
        onChange={(event) => setValue(event.target.value)}
        className="w-full min-w-0 bg-transparent text-right font-mono text-[13px] tabular-nums outline-none placeholder:text-muted-foreground/60"
      />
      {editor.mode === 'percent' && (
        <span aria-hidden className="text-xs text-muted-foreground">
          %
        </span>
      )}
    </span>
  )
}

/** The service's percentage or the couvert per person, empty for none. */
export function feePane(presenter: SplitItemsPresenter, state: JsSplitBillState, editor: JsFeeEditor): PaneParts {
  const isService = editor.fee === 'service'
  return {
    title: t(isService ? 'fee_serviceTitle' : 'fee_couvertTitle'),
    body: <FeeBody presenter={presenter} state={state} editor={editor} />,
    footer: (
      <>
        <Button key="fee-cancel" variant="outline" onClick={() => presenter.dismissFee()}>
          {t('common_cancel')}
        </Button>
        <Button key="fee-done" disabled={!editor.canSave} onClick={() => presenter.confirmFee()}>
          {t('splitBill_done')}
        </Button>
      </>
    ),
  }
}

function FeeBody({ presenter, state, editor }: { presenter: SplitItemsPresenter; state: JsSplitBillState; editor: JsFeeEditor }) {
  const isService = editor.fee === 'service'
  const [value, setValue] = useDraft(editor.valueText, (next) => {
    presenter.changeFee(next)
    return presenter.getFee()
  })
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor="items-fee">{t(isService ? 'fee_serviceField' : 'fee_couvertField')}</Label>
      <div className="flex items-center gap-2">
        {!isService && <span className="text-sm text-muted-foreground">{currencySymbol(state.currency)}</span>}
        <Input
          id="items-fee"
          inputMode={isService ? 'numeric' : 'decimal'}
          value={value}
          placeholder={isService ? '0' : amountPlaceholder(state.fractionDigits)}
          aria-invalid={!editor.canSave}
          aria-describedby="items-fee-help"
          autoComplete="off"
          autoFocus
          onChange={(event) => setValue(event.target.value)}
          className="text-right tabular-nums"
        />
        {isService && <span className="text-sm text-muted-foreground">%</span>}
      </div>
      <p id="items-fee-help" className="text-[13px] text-muted-foreground">
        {t(isService ? 'fee_serviceHelp' : 'fee_couvertHelp')}
      </p>
    </div>
  )
}

/** "Item" or a fee over "Itens da conta" on desktop and tablet; their own dismissal leaves the items as they were. */
export function ItemsOverlayDialogs({ presenter, state, money }: { presenter: SplitItemsPresenter; state: JsSplitBillState; money: Money }) {
  const item = state.itemEditor
  const fee = state.feeEditor
  return (
    <>
      {item && (
        <ResponsiveDialog
          isDesktop
          pane={itemPane(presenter, state, item, money)}
          onOpenChange={(open) => {
            if (!open) presenter.dismissItem()
          }}
          className="sm:max-w-[560px]"
        />
      )}
      {fee && (
        <ResponsiveDialog
          isDesktop
          pane={feePane(presenter, state, fee)}
          onOpenChange={(open) => {
            if (!open) presenter.dismissFee()
          }}
          className="sm:max-w-[400px]"
        />
      )}
    </>
  )
}

/** "Apagar este item?" (7.6h), saying what the items fall short of once it goes. */
export function ItemDeleteDialog({ presenter, state, money }: { presenter: SplitItemsPresenter; state: JsSplitBillState; money: Money }) {
  const deletion = state.itemDeletion
  if (!deletion) return null
  return (
    <ConfirmDialog
      title={t('item_deleteTitle')}
      description={itemDeletionText(deletion, money)}
      confirmLabel={t('item_deleteConfirm')}
      onConfirm={() => presenter.confirmDelete()}
      onDismiss={() => presenter.dismissDelete()}
    />
  )
}
