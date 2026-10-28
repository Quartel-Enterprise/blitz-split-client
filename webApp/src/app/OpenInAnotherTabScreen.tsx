import { AppWindow } from 'lucide-react'
import { BrandMark } from '@/components/common'
import { t } from '@/i18n'

/** Shown by a second tab of the app while the first one holds the database; it opens on its own after. */
export function OpenInAnotherTabScreen() {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-muted/40 p-4 sm:p-8">
      <div className="flex w-full max-w-md flex-col gap-6 rounded-xl border bg-card p-6 shadow-sm sm:p-8">
        <BrandMark />
        <div className="flex flex-col gap-1.5">
          <AppWindow aria-hidden className="mb-2 size-8 text-muted-foreground" />
          <h1 className="text-2xl font-semibold tracking-tight">{t('otherTab_title')}</h1>
          <p className="text-sm text-muted-foreground">{t('otherTab_body')}</p>
        </div>
      </div>
    </main>
  )
}
