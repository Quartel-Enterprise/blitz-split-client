import { useEffect } from 'react'
import { toast } from 'sonner'
import type { GroupDetailPresenter, JsCategoryTeach, JsTeachWord } from 'blitzsplit-shared'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { categoryName, categoryPath } from '@/lib/categories'
import { cn } from '@/lib/utils'

function offerText(teach: JsCategoryTeach): string {
  const saved = categoryPath(teach.category, teach.categoryParent)
  if (!teach.mapCategory) return t('groupDetail_teachOfferNoMap', { saved })
  return t('groupDetail_teachOffer', { saved, suggested: categoryPath(teach.mapCategory, teach.mapCategoryParent) })
}

function wordDetail(word: JsTeachWord, target: string): string {
  if (!word.currentCategory) return t('groupDetail_teachWordNew', { term: word.term, target })
  return t('groupDetail_teachWordMoves', { current: categoryName(word.currentCategory), term: word.term, target })
}

/**
 * "Ensinar ao mapa?" (9.3e): a toast with Ensinar and Agora não once a bill whose category was picked by hand is saved;
 * Ensinar opens the dialog that asks which word of the title (9.3f).
 */
export function useCategoryTeachNotice(presenter: GroupDetailPresenter | null, teach: JsCategoryTeach | null | undefined) {
  const isOffered = !!teach && !teach.isPickingWord
  const text = teach && isOffered ? offerText(teach) : null
  useEffect(() => {
    if (!presenter || !text) return
    // Why: sonner calls onDismiss for toast.dismiss(id) too, so only a swipe or close while it still asks says Agora não.
    let isCurrent = true
    const id = toast(text, {
      duration: Infinity,
      action: {
        label: t('groupDetail_teach'),
        onClick: () => {
          isCurrent = false
          presenter.teach()
        },
      },
      cancel: {
        label: t('groupDetail_teachNotNow'),
        onClick: () => {
          isCurrent = false
          presenter.dismissTeach()
        },
      },
      onDismiss: () => {
        if (isCurrent) presenter.dismissTeach()
      },
    })
    return () => {
      isCurrent = false
      toast.dismiss(id)
    }
  }, [presenter, text])
}

export function CategoryTeachDialog({ presenter, teach }: { presenter: GroupDetailPresenter; teach: JsCategoryTeach }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  const target = categoryPath(teach.category, teach.categoryParent)
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.dismissTeach()
      }}
      pane={{
        title: t('groupDetail_teachTitle'),
        description: t('groupDetail_teachQuestion', { title: teach.title, target }),
        body: (
          <div className="flex flex-col gap-3">
            <div role="radiogroup" aria-label={t('groupDetail_teachTitle')} className="flex flex-col divide-y rounded-lg border">
              {teach.words.map((word) => {
                const isSelected = word.key === teach.selectedKey
                return (
                  <button
                    key={word.key}
                    type="button"
                    role="radio"
                    aria-checked={isSelected}
                    onClick={() => presenter.pickTeachWord(word.key)}
                    className="flex items-center gap-3 p-3 text-left hover:bg-accent"
                  >
                    <span aria-hidden className={cn('flex size-4 shrink-0 items-center justify-center rounded-full border-2', isSelected ? 'border-primary' : 'border-muted-foreground')}>
                      {isSelected && <span className="size-2 rounded-full bg-primary" />}
                    </span>
                    <span className="flex flex-col">
                      <span className="font-medium">{word.term}</span>
                      <span className="text-xs text-muted-foreground">{wordDetail(word, target)}</span>
                    </span>
                  </button>
                )
              })}
            </div>
            <p className="text-xs text-muted-foreground">{t('groupDetail_teachFooter')}</p>
          </div>
        ),
        footer: (
          <>
            <Button disabled={!teach.selectedKey || teach.isSaving} onClick={() => presenter.confirmTeach()}>
              {t('groupDetail_teach')}
            </Button>
            <Button variant="outline" onClick={() => presenter.dismissTeach()}>
              {t('groupDetail_teachCancel')}
            </Button>
          </>
        ),
      }}
    />
  )
}
