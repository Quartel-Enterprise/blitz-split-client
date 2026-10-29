import { t } from '@/i18n'

/**
 * Downloads the shared code's export as a JSON file, named with the browser's local date:
 * "bora-rachar-dados-2026-10-29.json" in Portuguese, "blitz-split-data-2026-10-29.json" otherwise.
 */
export function downloadExport(json: string, now: Date = new Date()) {
  const url = URL.createObjectURL(new Blob([json], { type: 'application/json' }))
  const link = Object.assign(document.createElement('a'), { href: url, download: t('profile_exportFileName', { date: localDate(now) }) })
  link.click()
  URL.revokeObjectURL(url)
}

// Why: toISOString() is UTC, which names the file a day off late in the evening west of Greenwich.
function localDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
