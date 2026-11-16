import { toast } from 'sonner'
import { t } from '@/i18n'

/** `text` to the clipboard, toasting `copied` once it is there when given, and `failed` when it can't. */
export function copyText(text: string, failed: string, copied?: string) {
  // Why: a page outside a secure context has no navigator.clipboard, which would throw before the catch says so.
  const written = navigator.clipboard?.writeText(text) ?? Promise.reject(new Error('No clipboard'))
  written
    .then(() => copied && toast(copied))
    .catch(() => toast(failed))
}

/** The link to the clipboard, saying so when it fails, and `copied` once it is there when given. */
export function copyLink(link: string, copied?: string) {
  copyText(link, t('invite_copyFailed'), copied)
}

/** The link to the clipboard with a "Link copied" toast, for a button that can't say "Copied" itself. */
export function copyLinkConfirmed(link: string) {
  navigator.clipboard
    .writeText(link)
    .then(() => toast(t('invite_linkCopied')))
    .catch(() => toast(t('invite_copyFailed')))
}

/** The browser's share sheet with `text` where there is one (phones, Safari); otherwise `link` goes to the clipboard. */
export function shareOrCopy(text: string, link: string) {
  if (typeof navigator.share === 'function') {
    navigator.share({ text }).catch(() => {})
    return
  }
  copyLinkConfirmed(link)
}
