import { Palette } from 'lucide-react'
import type { CategoryMapPresenter, JsCategory, JsCustomColors } from 'blitzsplit-shared'
import { useState } from 'react'
import { CategoryColorPicker } from '@/components/CategoryColorPicker'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { t } from '@/i18n'
import { categoryName } from '@/lib/categories'

/**
 * "Change color of Food" (7.7): the color of one of the app's main categories, on this device only, with the person's
 * own colors and "+" after the named ones. A group's own category changes its color in its form instead.
 */
export function ChangeColorPopover({ presenter, category, customColors }: { presenter: CategoryMapPresenter; category: JsCategory; customColors: JsCustomColors }) {
  const [isOpen, setOpen] = useState(false)
  if (category.appKey == null || category.parentId != null || category.isOther) return null
  const appKey = category.appKey
  const title = t('categoryMap_changeColor', { name: categoryName(category) })
  const titleId = `change-color-${appKey}`
  return (
    <Popover open={isOpen} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={title}>
          <Palette />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="end"
        className="flex w-[min(20rem,calc(100vw-2rem))] flex-col gap-4"
        // Why: "New color" and the questions about a color open over it; a click in them never closes it.
        onInteractOutside={(event) => {
          if (customColors.draft || customColors.deletion) event.preventDefault()
        }}
      >
        <span id={titleId} className="font-semibold">
          {title}
        </span>
        <CategoryColorPicker
          selected={category.color}
          onPick={(code) => presenter.pickAppCategoryColor(appKey, code)}
          customColors={customColors}
          actions={presenter.customColors}
          categoryKey={appKey}
          labelledBy={titleId}
        />
        <div className="flex justify-between gap-2">
          <Button variant="ghost" onClick={() => presenter.pickAppCategoryColor(appKey, null)}>
            {t('categoryMap_restoreColor')}
          </Button>
          <Button onClick={() => setOpen(false)}>{t('splitBill_done')}</Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}
