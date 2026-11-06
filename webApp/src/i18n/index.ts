import { browserLanguages } from '@/lib/platform'
import { en, type MessageKey } from './en'
import { es } from './es'
import { esES } from './esES'
import { pt } from './pt'

export type { MessageKey }
/** `es` is the neutral Latin-American Spanish; `es-ES` is Spain's, which overrides only the words it says differently. */
export type Locale = 'en' | 'pt' | 'es' | 'es-ES'

const messages: Record<Locale, Record<MessageKey, string>> = { en, pt, es, 'es-ES': { ...es, ...esES } }

/**
 * The first browser language the app has, in the browser's order: any `pt` tag (pt, pt-BR, pt-PT…) is Portuguese,
 * `es-ES` (exactly Spain) is Spain's Spanish, every other `es` tag (es, es-MX, es-419, es-AR…) is the neutral
 * Latin-American Spanish, any `en` tag is English, and English when none matches.
 */
export function pickLocale(languages: readonly string[]): Locale {
  for (const tag of languages) {
    const [language, ...rest] = tag.toLowerCase().split('-')
    if (language === 'pt' || language === 'en') return language
    if (language === 'es') return rest.length === 1 && rest[0] === 'es' ? 'es-ES' : 'es'
  }
  return 'en'
}

// Why: Profile › Language's pick, which the shared code keeps under this key (WebAppLanguageLocalDataSource).
const storageKey = 'language'

function storedLanguage(): string | null {
  try {
    return localStorage.getItem(storageKey)
  } catch {
    return null
  }
}

/**
 * Picked once at startup: Profile › Language's pick, else the browser's languages. Picking another reloads the page,
 * so the app never switches languages while it runs.
 */
export const locale: Locale = pickLocale([storedLanguage(), ...browserLanguages()].filter((tag) => tag !== null))

/** Both Spanishes, for the few places where the language rather than the wording decides the grammar. */
export const isSpanish = locale === 'es' || locale === 'es-ES'

/** The message for `key` in the app's locale, with each `{name}` replaced by `params.name`. */
export function t(key: MessageKey, params?: Record<string, string | number>): string {
  const message = messages[locale][key]
  if (!params) return message
  return message.replace(/\{(\w+)\}/g, (match, name: string) => (name in params ? String(params[name]) : match))
}

// Why: "Ana dividió", but "Tú dividiste": Spanish conjugates the verb for the person, and one message serves them all.
const irregularSecondPerson: Record<string, string> = {
  deshizo: 'deshiciste',
  cambió: 'cambiaste',
  // Why: an -iar verb ends its preterite in "-ió" but takes "-aste", unlike the -er and -ir verbs.
  envió: 'enviaste',
  inició: 'iniciaste',
  copió: 'copiaste',
  vació: 'vaciaste',
  anunció: 'anunciaste',
}

/**
 * An activity verb ("dividió", "se unió a", "cambió la división de") said to the user, who is "Tú" in Spanish:
 * "dividiste", "te uniste a". Other languages keep the verb as it is.
 */
export function verbForYou(verb: string): string {
  if (!isSpanish) return verb
  const [first, ...rest] = verb.split(' ')
  if (first === 'se' && rest.length > 0) return ['te', verbForYou(rest[0]), ...rest.slice(1)].join(' ')
  const known = irregularSecondPerson[first]
  if (known) return [known, ...rest].join(' ')
  if (first.endsWith('ió')) return [`${first.slice(0, -2)}iste`, ...rest].join(' ')
  if (first.endsWith('ó')) return [`${first.slice(0, -1)}aste`, ...rest].join(' ')
  return verb
}

const documentLanguages: Record<Locale, string> = { en: 'en', pt: 'pt-BR', es: 'es-419', 'es-ES': 'es-ES' }

/** The page's language and title: Bora Rachar only in Portuguese, Blitz Split everywhere else. */
export function applyDocumentLocale() {
  document.documentElement.lang = documentLanguages[locale]
  document.title = t('appName')
}
