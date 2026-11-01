import { toast } from 'sonner'
import { t } from '@/i18n'

/** The link to the clipboard, saying so only when it fails. */
export function copyLink(link: string) {
  navigator.clipboard.writeText(link).catch(() => toast(t('invite_copyFailed')))
}

/** The browser's share sheet with `text` where there is one (phones, Safari); otherwise `link` goes to the clipboard. */
export function shareOrCopy(text: string, link: string) {
  if (typeof navigator.share === 'function') {
    navigator.share({ text }).catch(() => {})
    return
  }
  navigator.clipboard
    .writeText(link)
    .then(() => toast(t('invite_linkCopied')))
    .catch(() => toast(t('invite_copyFailed')))
}
