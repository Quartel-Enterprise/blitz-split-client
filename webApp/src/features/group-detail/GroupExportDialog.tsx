import { Braces, FileSpreadsheet, FileText, Sheet } from 'lucide-react'
import { type GroupDetailPresenter, JsGroupExportWords, type JsGroupExportFile } from 'blitzsplit-shared'
import type { ReactNode } from 'react'
import { ResponsiveDialog } from '@/components/ResponsiveDialog'
import { Button } from '@/components/ui/button'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t } from '@/i18n'
import { appCategoryName } from '@/lib/categories'
import { downloadBlob } from '@/lib/download'
import { payingMethodTypeName } from '@/lib/payingMethod'

const FORMATS: { format: string; icon: ReactNode; label: () => string }[] = [
  { format: 'csv', icon: <Sheet />, label: () => 'CSV' },
  { format: 'xlsx', icon: <FileSpreadsheet />, label: () => 'Excel (XLSX)' },
  { format: 'json', icon: <Braces />, label: () => 'JSON' },
  { format: 'txt', icon: <FileText />, label: () => t('groupExport_text') },
]

/**
 * "Export group data": which file to make. A format picked makes it from what the device has, offline too, with the
 * headers in the page's language, and the page downloads it (`downloadGroupExport`).
 */
export function GroupExportDialog({ presenter }: { presenter: GroupDetailPresenter }) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  return (
    <ResponsiveDialog
      isDesktop={isDesktop}
      onOpenChange={(open) => {
        if (!open) presenter.dismissExport()
      }}
      pane={{
        title: t('groupExport_title'),
        body: (
          <div className="flex flex-col gap-1">
            {FORMATS.map(({ format, icon, label }) => (
              <Button
                key={format}
                variant="ghost"
                className="h-12 justify-start gap-3 text-[15px] [&_svg]:size-5"
                onClick={() => presenter.exportAs(format, exportWordsOf(presenter))}
              >
                {icon}
                {label()}
              </Button>
            ))}
          </div>
        ),
        footer: (
          <Button variant="outline" onClick={() => presenter.dismissExport()}>
            {t('common_cancel')}
          </Button>
        ),
      }}
    />
  )
}

/** The file's headers and the app categories' names, in the page's language, as the shared code takes them. */
function exportWordsOf(presenter: GroupDetailPresenter): JsGroupExportWords {
  return new JsGroupExportWords(
    t('groupExport_bills'),
    t('groupExport_payments'),
    t('groupExport_balances'),
    t('groupExport_date'),
    t('groupExport_description'),
    t('groupExport_category'),
    t('groupExport_amount'),
    t('groupExport_currency'),
    t('groupExport_paid'),
    t('groupExport_share'),
    t('groupExport_from'),
    t('groupExport_to'),
    t('groupExport_member'),
    t('groupExport_balance'),
    t('balance_formerMember'),
    presenter.exportCategoryKeys.map(appCategoryName),
    t('groupExport_paymentMethod'),
    presenter.exportPayingMethodTypes.map(payingMethodTypeName),
  )
}

/** Saves the file "Export group data" made, under the name the shared code gave it. */
export function downloadGroupExport(file: JsGroupExportFile) {
  downloadBlob(new Blob([file.bytes as Int8Array<ArrayBuffer>], { type: file.mimeType }), file.fileName)
}
