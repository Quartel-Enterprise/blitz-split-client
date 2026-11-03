import { ArrowRight, ImageMinus, ImagePlus, Link2, Pencil, Plus, Receipt, Tag } from 'lucide-react'
import type { JsBillHistoryEntry, JsBillHistoryDay } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { LoadingRegion, SkeletonCircle, SkeletonLine } from '@/components/ui/skeleton'
import { t } from '@/i18n'
import { formatDayLabel, formatTime } from '@/lib/dates'
import { historyActor, historyDiffs, historyNote, historyVerb, type HistoryDiff } from './billHistoryText'

type Money = (minor: number) => string

/**
 * "Histórico", by day, the most recent first: who did what to the bill and what changed, with "Desfazer" on the
 * latest edit while the bill is still as it left it. Hidden while there is nothing in it; null shimmers while the
 * server's history is on its way.
 */
export function BillHistory({ history, isUndoing, money, onUndo }: { history: JsBillHistoryDay[] | null | undefined; isUndoing: boolean; money: Money; onUndo: () => void }) {
  if (!history) return <HistorySkeleton />
  if (history.length === 0) return null
  return (
    <section aria-labelledby="bill-history-title" className="flex flex-col">
      <h3 id="bill-history-title" className="text-sm font-medium">
        {t('billDetail_history')}
      </h3>
      {history.map((day) => (
        <div key={day.dayStartMillis} className="flex flex-col">
          <h4 className="pt-2 pb-1.5 text-xs font-medium text-muted-foreground">{formatDayLabel(day.dayStartMillis, day.relativeDay)}</h4>
          <ol className="flex flex-col">
            {day.entries.map((entry, index) => (
              <HistoryLine key={entry.id} entry={entry} hasRail={index < day.entries.length - 1} isUndoing={isUndoing} money={money} onUndo={onUndo} />
            ))}
          </ol>
        </div>
      ))}
    </section>
  )
}

/** The section's title over two lines' places: the face, the line and when, under a day. */
function HistorySkeleton() {
  return (
    <section aria-labelledby="bill-history-title" className="flex flex-col gap-3">
      <h3 id="bill-history-title" className="text-sm font-medium">
        {t('billDetail_history')}
      </h3>
      <LoadingRegion isDelayed={false} className="flex flex-col gap-3">
        {[0, 1].map((index) => (
          <span key={index} className="flex gap-2.5">
            <SkeletonCircle className="size-7" />
            <span className="flex flex-1 flex-col gap-2 pt-1">
              <SkeletonLine className={index === 0 ? 'w-3/5' : 'w-1/2'} />
              <SkeletonLine className="h-2.5 w-1/4" />
            </span>
          </span>
        ))}
      </LoadingRegion>
    </section>
  )
}

function kindIcon(entry: JsBillHistoryEntry): ReactNode {
  switch (entry.kind) {
    case 'created':
      return <Plus />
    case 'attachments-added':
      return entry.isReceipt ? <Receipt /> : <ImagePlus />
    case 'attachment-removed':
      return <ImageMinus />
    case 'link-changed':
      return <Link2 />
    default:
      return entry.changes[0]?.field === 'category' ? <Tag /> : <Pencil />
  }
}

/** "Ana trocou a categoria · 9:12", what changed under it, and the orange dot while it waits to go up. */
function HistoryLine({ entry, hasRail, isUndoing, money, onUndo }: { entry: JsBillHistoryEntry; hasRail: boolean; isUndoing: boolean; money: Money; onUndo: () => void }) {
  const diffs = historyDiffs(entry, money)
  const note = historyNote(entry, money)
  return (
    <li className="relative flex gap-2.5">
      {/* Why: a line joins the faces of one day, as the canvas draws it. */}
      {hasRail && <span aria-hidden className="absolute top-8 bottom-0.5 left-[13.5px] w-px bg-border" />}
      <span className="relative size-7 shrink-0">
        <Avatar initial={entry.actorInitials} color={entry.actorColor} photoUrl={entry.actorPhotoUrl} className="size-7 text-[11px]" />
        <span aria-hidden className="absolute -right-1 -bottom-1 flex size-4 items-center justify-center rounded-full border-2 border-card bg-foreground text-background [&>svg]:size-2 [&>svg]:stroke-[3.2]">
          {kindIcon(entry)}
        </span>
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-1.5 pt-1 pb-3.5">
        <span className="flex items-baseline gap-2">
          <span className="min-w-0 flex-1 text-sm leading-5">
            <span className="font-medium">{historyActor(entry)}</span> <span className="text-muted-foreground">{historyVerb(entry)}</span>
          </span>
          <span className="flex shrink-0 items-center gap-1.5 text-xs text-muted-foreground tabular-nums">
            {entry.isPending && (
              <>
                <span aria-hidden className="size-1.5 rounded-full bg-orange-500" />
                <span className="sr-only">{t('billDetail_historyPending')}</span>
              </>
            )}
            {formatTime(entry.occurredAt)}
          </span>
        </span>
        {diffs.map((diff, index) => (
          <DiffLine key={index} diff={diff} />
        ))}
        {note && <span className="text-xs text-muted-foreground">{note}</span>}
        {entry.canUndo && (
          <Button variant="link" size="sm" disabled={isUndoing} className="h-auto self-start p-0 text-[13px]" onClick={onUndo}>
            {t('billDetail_undo')}
          </Button>
        )}
      </div>
    </li>
  )
}

/** "Carla [R$ 112,00] → [R$ 98,00]", the side before struck through. */
function DiffLine({ diff }: { diff: HistoryDiff }) {
  return (
    <span className="flex flex-wrap items-center gap-1.5 text-xs">
      {diff.label && <span className="min-w-10 text-muted-foreground">{diff.label}</span>}
      <s className="inline-flex h-5 items-center rounded-md border px-1.5 text-muted-foreground">{diff.from}</s>
      <ArrowRight role="img" aria-label={t('activity_to')} className="size-3 text-muted-foreground" />
      <span className="inline-flex h-5 items-center rounded-md border px-1.5 font-medium">{diff.to}</span>
    </span>
  )
}
