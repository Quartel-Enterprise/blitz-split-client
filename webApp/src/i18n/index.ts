import { browserLanguages } from '@/lib/platform'
import { en, type MessageKey } from './en'
import { pt } from './pt'

export type { MessageKey }
export type Locale = 'en' | 'pt'

const messages: Record<Locale, Record<MessageKey, string>> = { en, pt }

/**
 * The first browser language the app has, in the browser's order: any `pt` tag (pt, pt-BR, pt-PT…)
 * is Portuguese, any `en` tag English, and English when none matches.
 */
export function pickLocale(languages: readonly string[]): Locale {
  for (const tag of languages) {
    const language = tag.toLowerCase().split('-')[0]
    if (language === 'pt' || language === 'en') return language
  }
  return 'en'
}

/** Picked once at startup; the app doesn't switch languages while it runs. */
export const locale: Locale = pickLocale(browserLanguages())

/** The message for `key` in the app's locale, with each `{name}` replaced by `params.name`. */
export function t(key: MessageKey, params?: Record<string, string | number>): string {
  const message = messages[locale][key]
  if (!params) return message
  return message.replace(/\{(\w+)\}/g, (match, name: string) => (name in params ? String(params[name]) : match))
}

/** The page's language and title: Bora Rachar only in Portuguese, Blitz Split everywhere else. */
export function applyDocumentLocale() {
  document.documentElement.lang = locale === 'pt' ? 'pt-BR' : 'en'
  document.title = t('appName')
}
