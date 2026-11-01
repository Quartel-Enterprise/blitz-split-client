import type { JsItemEditor, JsItemStatus, JsItemsRow, JsItemsStatus, JsItemDeletion, JsItemsPersonRow } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'
import { formatNames, formatPercent } from '@/lib/money'
import type { Money } from './splitText'

/** The first line of "Itens da conta"'s status: "2 itens sem ninguém", "Faltam R$ 15,00" or "Fechou: R$ 642,00". */
export function itemsStatusTitle(status: JsItemsStatus, money: Money): string {
  switch (status.kind) {
    case 'loose':
      return t(status.names.length === 1 ? 'split_looseItemsOne' : 'split_looseItemsOther', { count: status.names.length })
    case 'left':
      return t('split_amountLeft', { amount: money(status.amountMinor) })
    case 'over':
      return t('split_amountOver', { amount: money(status.amountMinor) })
    default:
      return t('split_addsUpAmount', { amount: money(status.totalMinor) })
  }
}

export function itemsStatusBody(status: JsItemsStatus, money: Money, signed: Money): string {
  switch (status.kind) {
    case 'loose':
      if (status.isEveryItem) return t('items_looseAll')
      return t(status.names.length === 1 ? 'items_looseSomeOne' : 'items_looseSomeOther', {
        names: formatNames(status.names),
        amount: money(status.amountMinor),
      })
    case 'left':
    case 'over':
      return t(status.isFromReceipt ? 'items_sumReceipt' : 'items_sum', { sum: money(status.sumMinor), total: money(status.totalMinor) })
    default: {
      const parts = { items: money(status.itemsMinor), service: money(status.serviceMinor), couvert: money(status.couvertMinor) }
      return status.extraMinor === 0 ? t('items_closed', parts) : t('items_closedExtra', { ...parts, extra: signed(status.extraMinor) })
    }
  }
}

const rowModes: Record<string, MessageKey> = {
  units: 'items_rowUnits',
  shares: 'items_rowShares',
  amount: 'items_rowAmount',
  percent: 'items_rowPercent',
}

/** "6 × R$ 14,00 · Por unidade", or "Sem ninguém" for an item nobody is marked in. */
export function itemRowSubtitle(row: JsItemsRow, money: Money): string {
  const how = row.isLoose
    ? t('items_rowNobody')
    : row.mode === 'equal'
      ? row.consumers.length === 1
        ? t('items_rowEqualOne')
        : t('items_rowEqual', { count: row.consumers.length })
      : t(rowModes[row.mode] ?? 'items_rowAmount')
  return t('items_rowSub', { quantity: row.quantity, price: money(row.unitPriceMinor), how })
}

/** "R$ 120,00 em itens + R$ 14,50 de serviço e couvert", or only the couvert before anything is marked. */
export function itemsPersonSubtitle(row: JsItemsPersonRow, money: Money): string {
  if (row.itemsMinor > 0) return t('items_personSub', { items: money(row.itemsMinor), fees: money(row.feesMinor) })
  return row.feesMinor > 0 ? t('items_personCouvertOnly') : t('items_personNothing')
}

export const itemModeLabels: Record<string, MessageKey> = {
  equal: 'item_modeEqual',
  units: 'item_modeUnits',
  shares: 'item_modeShares',
  amount: 'item_modeAmount',
  percent: 'item_modePercent',
}

export const itemListLabels: Record<string, MessageKey> = {
  equal: 'item_listEqual',
  units: 'item_listUnits',
  shares: 'item_listShares',
  amount: 'item_listAmount',
  percent: 'item_listPercent',
}

export const itemHints: Record<string, MessageKey> = {
  equal: 'item_hintEqual',
  units: 'item_hintUnits',
  shares: 'item_hintShares',
  amount: 'item_hintAmount',
  percent: 'item_hintPercent',
}

function among(total: number, people: number, money: Money): string {
  return t(people === 1 ? 'split_amongOne' : 'split_amongOther', { total: money(total), count: people })
}

/** The item's status, title and the line under it. */
export function itemStatusText(status: JsItemStatus, money: Money): { title: string; body: string } {
  const one = status.count === 1
  switch (status.kind) {
    case 'nobody':
      return { title: t('item_nobody'), body: t('item_nobodySub') }
    case 'units-add-up':
      return {
        title: t(status.quantity === 1 ? 'item_unitsClosedOne' : 'item_unitsClosedOther', { marked: status.quantity, quantity: status.quantity }),
        body: t('item_unitsPrice', { amount: money(status.unitPriceMinor) }),
      }
    case 'units-left':
      return {
        title: t(one ? 'item_unitsLeftOne' : 'item_unitsLeftOther', { count: status.count }),
        body: t(one ? 'item_unitsLeftSubOne' : 'item_unitsLeftSubOther', { marked: status.marked, quantity: status.quantity }),
      }
    case 'units-over':
      return {
        title: t(one ? 'item_unitsOverOne' : 'item_unitsOverOther', { count: status.count }),
        body: t('item_unitsMarked', { marked: status.marked, quantity: status.quantity }),
      }
    case 'each':
      return { title: t('split_equalEach', { amount: money(status.amountMinor) }), body: among(status.totalMinor, status.people, money) }
    case 'parts': {
      const several = status.severalParts.map((parts) =>
        parts.person.isMe
          ? t(parts.parts === 1 ? 'item_myPartsOne' : 'item_myPartsOther', { count: parts.parts })
          : t(parts.parts === 1 ? 'item_personPartsOne' : 'item_personPartsOther', { name: parts.person.name, count: parts.parts }),
      )
      return {
        title: t(one ? 'split_sharesOne' : 'split_sharesOther', { count: status.count, amount: money(status.amountMinor) }),
        body: several.length > 0 ? several.join(' ') : among(status.totalMinor, status.people, money),
      }
    }
    case 'amount-add-up':
      return { title: t('split_addsUpAmount', { amount: money(status.totalMinor) }), body: t('item_amountClosed') }
    case 'amount-left':
    case 'amount-over':
      return {
        title: t(status.kind === 'amount-left' ? 'split_amountLeft' : 'split_amountOver', { amount: money(status.amountMinor) }),
        body: t('split_amountSoFar', { assigned: money(status.assignedMinor), total: money(status.totalMinor) }),
      }
    case 'percent-add-up':
      return { title: t('split_addsUpPercent', { percent: formatPercent(100) }), body: among(status.totalMinor, status.people, money) }
    default:
      return {
        title: t(status.kind === 'percent-left' ? 'split_percentLeft' : 'split_percentOver', { percent: formatPercent(status.count) }),
        body: t('item_percentProgress', { sum: formatPercent(status.marked) }),
      }
  }
}

/** "“Cerveja long neck” (R$ 84,00) sai da conta e o serviço é refeito. Os itens deixam de fechar com o total: …" */
export function itemDeletionText(deletion: JsItemDeletion, money: Money): string {
  const params = { name: deletion.name, amount: money(deletion.totalMinor) }
  const first = t(deletion.redoesService ? 'item_deleteTextService' : 'item_deleteText', params)
  return deletion.leftMinor != null ? `${first} ${t('item_deleteLeft', { amount: money(deletion.leftMinor) })}` : first
}

/** Whether a mode's people take a field: Valor and %, and on the web Unidades and Partes too. */
export function takesField(editor: JsItemEditor): boolean {
  return editor.mode !== 'equal'
}
