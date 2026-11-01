import type { JsContactRow } from 'blitzsplit-shared'
import { t } from '@/i18n'

/** "te deve", "você deve" or "quitado", by the sign of what is open with the person. */
export function netWord(minor: number): string {
  if (minor > 0) return t('contacts_rowOwesYou')
  if (minor < 0) return t('contacts_rowYouOwe')
  return t('contacts_rowSettled')
}

/** "3 grupos em comum" or the one group's name, with "· convidado sem app" for a guest. */
export function rowMeta(row: JsContactRow): string {
  const groups = row.groupCount > 1 ? t('contacts_groupsInCommon', { count: row.groupCount }) : row.firstGroupName
  return row.isGuest ? t('contacts_guestMeta', { groups }) : groups
}
