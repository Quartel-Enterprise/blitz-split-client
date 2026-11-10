import { browserLanguages } from '@/lib/platform'
import { en, type MessageKey } from './en'
import { es } from './es'
import { esES } from './esES'
import { de } from './de'
import { fr } from './fr'
import { id } from './id'
import { it } from './it'
import { ja } from './ja'
import { nl } from './nl'
import { pl } from './pl'
import { pt } from './pt'
import { tr } from './tr'

export type { MessageKey }
/** `es` is the neutral Latin-American Spanish; `es-ES` is Spain's, which overrides only the words it says differently. */
export type Locale = 'en' | 'pt' | 'es' | 'es-ES' | 'fr' | 'de' | 'it' | 'pl' | 'nl' | 'ja' | 'tr' | 'id'

const messages: Record<Locale, Record<MessageKey, string>> = { en, pt, es, 'es-ES': { ...es, ...esES }, fr, de, it, pl, nl, ja, tr, id }

// Why: the languages with one variant for every tag of theirs.
const bareLocales: readonly Locale[] = ['pt', 'en', 'fr', 'de', 'it', 'pl', 'nl', 'ja', 'tr', 'id']

/**
 * The first browser language the app has, in the browser's order: any `pt` tag (pt, pt-BR, pt-PT…) is Portuguese,
 * `es-ES` (exactly Spain) is Spain's Spanish, every other `es` tag (es, es-MX, es-419, es-AR…) is the neutral
 * Latin-American Spanish, any `fr` tag (fr, fr-FR, fr-CA…) is French, any `de`, `it`, `pl`, `nl`, `ja`, `tr` or `id`
 * tag (de-AT, it-CH, pl-PL, nl-BE, ja-JP, tr-CY, id-ID, the legacy in-ID…) is German, Italian, Polish, Dutch,
 * Japanese, Turkish or Indonesian, any `en` tag is English, and English when none matches.
 */
export function pickLocale(languages: readonly string[]): Locale {
  for (const tag of languages) {
    const [subtag, ...rest] = tag.toLowerCase().split(/[-_]/)
    // Why: "in" is Indonesian's legacy code, which older Android WebViews still send; AppLanguage.fromTag maps it too.
    const language = subtag === 'in' ? 'id' : subtag
    if (bareLocales.includes(language as Locale)) return language as Locale
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

/**
 * What goes between the pieces of a sentence the code joins ("Ana" and "changed the category", two sentences of a
 * detail): a space, but nothing in Japanese, which writes "Anaがカテゴリを変更しました". Same rule as Android's
 * `wordGapUi`.
 */
export const wordGap = locale === 'ja' ? '' : ' '

/** The end of a sentence the code finishes: Japanese's "。", the others' ".". Same rule as Android's `fullStopUi`. */
export const fullStop = locale === 'ja' ? '。' : '.'

/**
 * Between two clauses of one sentence the code joins ("…pays you $64, and Ana no longer owes you"): Japanese ends the
 * first sentence instead ("…払います。…なくなりました").
 */
export const clauseBreak = locale === 'ja' ? '。' : ', '

const documentLanguages: Record<Locale, string> = {
  en: 'en',
  pt: 'pt-BR',
  es: 'es-419',
  'es-ES': 'es-ES',
  fr: 'fr',
  de: 'de',
  it: 'it',
  pl: 'pl',
  nl: 'nl',
  ja: 'ja',
  tr: 'tr',
  id: 'id',
}

const documentLanguage = documentLanguages[locale]

const pluralRules = new Intl.PluralRules(documentLanguage)

/**
 * Polish says "2 grupy" but "5 grup": where the other languages have one plural, it has few (2–4, 22–24…) and many
 * (0, 5–21…). Its message for a `…Other` key with a `{count}` holds both, "{count} grupy|{count} grup", and this picks
 * the one `count` takes (a fraction taking few's, as Polish does). Any other message has one form. `count` must be the
 * number itself, not a formatted string: one that isn't a number gets the many form, the one most counts take.
 */
function pluralFormOf(message: string, count: string | number | undefined): string {
  if (!message.includes('|')) return message
  const forms = message.split('|')
  const category = typeof count === 'number' ? pluralRules.select(count) : 'many'
  return category === 'many' ? forms[1] : forms[0]
}

/** The message for `key` in the app's locale, with each `{name}` replaced by `params.name`. */
export function t(key: MessageKey, params?: Record<string, string | number>): string {
  const message = pluralFormOf(messages[locale][key], params?.count)
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

// Why: each language's perfect auxiliary in the third person, with its second person.
const secondPersonAuxiliaries: Partial<Record<Locale, Record<string, string>>> = {
  fr: { a: 'as' },
  de: { hat: 'hast', ist: 'bist' },
  it: { ha: 'hai' },
  nl: { heeft: 'hebt', is: 'bent' },
}

/**
 * An activity verb ("dividió", "se unió a", "cambió la división de") said to the user, who is "Tú" in Spanish:
 * "dividiste", "te uniste a". French, German, Italian and Dutch verb phrases all start with their perfect's auxiliary
 * ("a divisé", "hat geteilt", "ist beigetreten", "ha diviso", "heeft verdeeld", "is lid geworden"), which becomes the
 * user's ("as divisé", "hast geteilt", "bist beigetreten", "hai diviso", "hebt verdeeld", "bent lid geworden"). Polish
 * ones start with a past in "ł(a)", the app not knowing the gender: "podzielił(a)" becomes "podzieliłeś(-aś)". Turkish
 * ones end with their simple past, which takes an "n": "böldü" becomes "böldün". Other languages keep the verb as it
 * is. Same rules as Android's `toSecondPerson` and the Apple app's `SecondPerson`.
 */
export function verbForYou(verb: string): string {
  const auxiliary = secondPersonAuxiliaries[locale]
  if (auxiliary) {
    const [first, ...rest] = verb.split(' ')
    return Object.hasOwn(auxiliary, first) ? [auxiliary[first], ...rest].join(' ') : verb
  }
  if (locale === 'tr') return turkishVerbForYou(verb)
  if (locale === 'pl') return verb.replace(/^(\S*)ł\(a\)(?= |$)/, '$1łeś(-aś)')
  if (!isSpanish) return verb
  const [first, ...rest] = verb.split(' ')
  if (first === 'se' && rest.length > 0) return ['te', verbForYou(rest[0]), ...rest.slice(1)].join(' ')
  const known = irregularSecondPerson[first]
  if (known) return [known, ...rest].join(' ')
  if (first.endsWith('ió')) return [`${first.slice(0, -2)}iste`, ...rest].join(' ')
  if (first.endsWith('ó')) return [`${first.slice(0, -1)}aste`, ...rest].join(' ')
  return verb
}

/**
 * Turkish puts the verb last, so every Turkish verb phrase ends with its simple past, before the colon that leads to the
 * object if there is one ("bir harcamayı böldü:"), and the second person adds an "n" to it whatever the vowel ("böldü"
 * → "böldün", "katıldı" → "katıldın"). A phrase whose clause doesn't end in that past stays as it is.
 */
function turkishVerbForYou(verb: string): string {
  return verb.replace(/^([^:]*?[dt][ıiuü])(\s*(?::|$))/u, '$1n$2')
}

/** The page's language and title: Bora Rachar only in Portuguese, Blitz Split everywhere else. */
export function applyDocumentLocale() {
  document.documentElement.lang = documentLanguage
  document.title = t('appName')
}
