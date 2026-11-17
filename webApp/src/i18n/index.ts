import { browserLanguages } from '@/lib/platform'
import { en, type MessageKey } from './en'
import { es } from './es'
import { esES } from './esES'
import { ptPT } from './ptPT'
import { de } from './de'
import { fr } from './fr'
import { id } from './id'
import { it } from './it'
import { ja } from './ja'
import { nl } from './nl'
import { pl } from './pl'
import { pt } from './pt'
import { ru } from './ru'
import { tr } from './tr'
import { uk } from './uk'
import { ko } from './ko'
import { hi } from './hi'
import { zh } from './zh'

export type { MessageKey }
/**
 * `es` is the neutral Latin-American Spanish; `es-ES` is Spain's, which overrides only the words it says differently.
 * `pt` is Brazil's Portuguese; `pt-PT` is Portugal's, which overrides only the words it says differently.
 * `zh` is Simplified Chinese, mainland China's.
 */
export type Locale = 'en' | 'pt' | 'pt-PT' | 'es' | 'es-ES' | 'fr' | 'de' | 'it' | 'pl' | 'nl' | 'ja' | 'tr' | 'id' | 'ru' | 'uk' | 'ko' | 'hi' | 'zh'

const messages: Record<Locale, Record<MessageKey, string>> = { en, pt, 'pt-PT': { ...pt, ...ptPT }, es, 'es-ES': { ...es, ...esES }, fr, de, it, pl, nl, ja, tr, id, ru, uk, ko, hi, zh }

// Why: the languages with one variant for every tag of theirs.
const bareLocales: readonly Locale[] = ['en', 'fr', 'de', 'it', 'pl', 'nl', 'ja', 'tr', 'id', 'ru', 'uk', 'ko', 'hi', 'zh']

// Why: the first two-letter subtag before any extension ("-u-fw-mon", whose keys are no region), as AppLanguage.regionOf.
function regionOf(subtags: readonly string[]): string | undefined {
  const end = subtags.findIndex((subtag) => subtag.length < 2)
  return (end === -1 ? subtags : subtags.slice(0, end)).find((subtag) => subtag.length === 2)
}

/**
 * The first browser language the app has, in the browser's order: `pt-PT` (exactly Portugal) is Portugal's Portuguese,
 * every other `pt` tag (pt, pt-BR…) is Brazil's, `es-ES` (Spain) is Spain's Spanish, every other `es` tag
 * (es, es-MX, es-419, es-AR…) is the neutral Latin-American Spanish, any `fr` tag (fr, fr-FR, fr-CA…) is French, any
 * `de`, `it`, `pl`, `nl`, `ja`, `tr`, `id`, `ru`, `uk`, `ko` or `hi` tag (de-AT, it-CH, pl-PL, nl-BE, ja-JP, tr-CY,
 * id-ID, the legacy in-ID, ru-KZ, uk-UA, ko-KR, hi-IN…) is German, Italian, Polish, Dutch, Japanese, Turkish,
 * Indonesian, Russian, Ukrainian, Korean or Hindi, any `zh` tag but a Traditional one (zh-CN, zh-Hans, zh-SG…) is Simplified Chinese, any
 * `en` tag is English, and English when none matches.
 */
export function pickLocale(languages: readonly string[]): Locale {
  for (const tag of languages) {
    const [subtag, ...rest] = tag.toLowerCase().split(/[-_]/)
    // Why: "in" is Indonesian's legacy code, which older Android WebViews still send; AppLanguage.fromTag maps it too.
    const language = subtag === 'in' ? 'id' : subtag
    // Why: no Traditional Chinese yet, so a zh-Hant, zh-TW, zh-HK or zh-MO tag is skipped, as AppLanguage.fromTag does.
    if (language === 'zh' && rest.some((subtag) => ['hant', 'tw', 'hk', 'mo'].includes(subtag))) continue
    if (bareLocales.includes(language as Locale)) return language as Locale
    if (language === 'pt') return regionOf(rest) === 'pt' ? 'pt-PT' : 'pt'
    if (language === 'es') return regionOf(rest) === 'es' ? 'es-ES' : 'es'
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

// Why: Japanese and Chinese write no space between words, nor between two sentences.
const isUnspaced = locale === 'ja' || locale === 'zh'

/**
 * What goes between the pieces of a sentence the code joins ("Ana" and "changed the category", two sentences of a
 * detail): a space, but nothing in Japanese and Chinese, which write "Anaがカテゴリを変更しました". Same rule as
 * Android's `wordGapUi`.
 */
export const wordGap = isUnspaced ? '' : ' '

/**
 * The end of a sentence the code finishes: Japanese's and Chinese's "。", Hindi's danda "।", the others' ".". Same rule
 * as Android's `fullStopUi`.
 */
export const fullStop = isUnspaced ? '。' : locale === 'hi' ? '।' : '.'

/**
 * Between two clauses of one sentence the code joins ("…pays you $64, and Ana no longer owes you"): Japanese ends the
 * first sentence instead ("…払います。…なくなりました"), and Chinese takes its own comma ("…，Ana不再欠你").
 */
export const clauseBreak = locale === 'ja' ? '。' : locale === 'zh' ? '，' : ', '

const documentLanguages: Record<Locale, string> = {
  en: 'en',
  pt: 'pt-BR',
  'pt-PT': 'pt-PT',
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
  ru: 'ru',
  uk: 'uk',
  ko: 'ko',
  hi: 'hi',
  zh: 'zh-Hans',
}

const documentLanguage = documentLanguages[locale]

const pluralRules = new Intl.PluralRules(documentLanguage)

/**
 * Polish says "2 grupy" but "5 grup": where the other languages have one plural, it has few (2–4, 22–24…) and many
 * (0, 5–21…). Its message for a `…Other` key with a `{count}` holds both, "{count} grupy|{count} grup", and this picks
 * the one `count` takes (a fraction taking few's, as Polish does). Russian and Ukrainian also say 21 in the singular
 * ("21 группа"), so theirs hold three, one|few|many: "{count} группа|{count} группы|{count} групп". A fraction takes
 * few's, which is Russian's own; Ukrainian has a form of its own for it ("1,5 платежу"), which no count the app
 * passes needs. Any other message has one form. `count` must be the number itself, not a formatted string: one that
 * isn't a number gets the many form, the one most counts take.
 */
function pluralFormOf(message: string, count: string | number | undefined): string {
  if (!message.includes('|')) return message
  const forms = message.split('|')
  const category = typeof count === 'number' ? pluralRules.select(count) : 'many'
  if (forms.length === 3) return forms[category === 'one' ? 0 : category === 'many' ? 2 : 1]
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
  leyó: 'leíste',
}

// Why: the preterite of the verbs an activity row says, which Portugal's Portuguese conjugates for "tu" ("dividiu" → "dividiste").
const portugueseIrregularSecondPerson: Record<string, string> = {
  fez: 'fizeste',
  desfez: 'desfizeste',
  saiu: 'saíste',
  pôs: 'puseste',
}

// Why: the object pronoun Spanish puts before the verb ("lo adjuntó"), which stays as the verb changes ("lo adjuntaste").
const spanishObjectPronouns = new Set(['lo', 'la', 'los', 'las', 'le', 'les'])

// Why: each language's perfect auxiliary in the third person, with its second person.
const secondPersonAuxiliaries: Partial<Record<Locale, Record<string, string>>> = {
  fr: { a: 'as', 'l’a': 'l’as', "l'a": "l'as" },
  de: { hat: 'hast', ist: 'bist' },
  it: { ha: 'hai', 'l’ha': 'l’hai', "l'ha": "l'hai" },
  nl: { heeft: 'hebt', is: 'bent' },
}

/**
 * An activity verb ("dividió", "se unió a", "cambió la división de") said to the user, who is "Tú" in Spanish:
 * "dividiste", "te uniste a". French, German, Italian and Dutch verb phrases all start with their perfect's auxiliary
 * ("a divisé", "hat geteilt", "ist beigetreten", "ha diviso", "heeft verdeeld", "is lid geworden"), which becomes the
 * user's ("as divisé", "hast geteilt", "bist beigetreten", "hai diviso", "hebt verdeeld", "bent lid geworden"). Polish
 * ones start with a past in "ł(a)", the app not knowing the gender: "podzielił(a)" becomes "podzieliłeś(-aś)". Turkish
 * ones end with their simple past, which takes an "n": "böldü" becomes "böldün". Other languages keep the verb as it
 * is, Brazil's Portuguese too ("Você dividiu"), and Russian and Ukrainian, whose past says the gender but not the
 * person ("Ты разделил(а)"). Korean writes someone else as "Ana 님이" and the user as "내가", whose subject word holds its
 * own particle, so its verb phrases all start with "님이", which the user's drops: "님이 나눴어요" becomes "나눴어요".
 * Hindi does the same with its ergative "ने", the user's "आपने" holding it: "ने खर्च बांटा" becomes "खर्च बांटा".
 * Portugal's Portuguese ("tu") ends the preterite in "-aste", "-este" or "-iste": "dividiu" becomes "dividiste".
 * Same rules as Android's `toSecondPerson` and the Apple app's `SecondPerson`.
 */
export function verbForYou(verb: string): string {
  if (locale === 'pt-PT') return portugueseVerbForYou(verb)
  const auxiliary = secondPersonAuxiliaries[locale]
  if (auxiliary) {
    const [first, ...rest] = verb.split(' ')
    return Object.hasOwn(auxiliary, first) ? [auxiliary[first], ...rest].join(' ') : verb
  }
  if (locale === 'tr') return turkishVerbForYou(verb)
  if (locale === 'ko') return verb.replace(/^님이(?: |$)/, '')
  if (locale === 'hi') return verb.replace(/^ने(?: |$)/, '')
  if (locale === 'pl') return verb.replace(/^(\S*)ł\(a\)(?= |$)/, '$1łeś(-aś)')
  if (!isSpanish) return verb
  const [first, ...rest] = verb.split(' ')
  if (first === 'se' && rest.length > 0) return ['te', verbForYou(rest[0]), ...rest.slice(1)].join(' ')
  if (spanishObjectPronouns.has(first) && rest.length > 0) return [first, verbForYou(rest[0]), ...rest.slice(1)].join(' ')
  const known = irregularSecondPerson[first]
  if (known) return [known, ...rest].join(' ')
  if (first.endsWith('ió')) return [`${first.slice(0, -2)}iste`, ...rest].join(' ')
  if (first.endsWith('ó')) return [`${first.slice(0, -1)}aste`, ...rest].join(' ')
  return verb
}

function portugueseVerbForYou(verb: string): string {
  const [first, ...rest] = verb.split(' ')
  const known = portugueseIrregularSecondPerson[first]
  if (known) return [known, ...rest].join(' ')
  const ending = /(ou|eu|iu)$/.exec(first)
  if (!ending) return verb
  const replacement = { ou: 'aste', eu: 'este', iu: 'iste' }[ending[1] as 'ou' | 'eu' | 'iu']
  return [`${first.slice(0, -2)}${replacement}`, ...rest].join(' ')
}

/**
 * A sentence already in the user's language with their `name` first, its verb said to them: "Tú lo adjuntaste", "Du
 * hast es angehängt". A line with no verb right after the name ("Sen · bugün, 21:10 eklendi") stays as it is.
 */
export function sayToActor(text: string, name: string): string {
  if (!text.startsWith(`${name} `)) return text
  const rest = text.slice(name.length + 1)
  return rest.startsWith('·') ? text : `${name} ${verbForYou(rest)}`
}

/**
 * Turkish puts the verb last, so every Turkish verb phrase ends with its simple past, before the colon that leads to the
 * object if there is one ("bir harcamayı böldü:"), and the second person adds an "n" to it whatever the vowel ("böldü"
 * → "böldün", "katıldı" → "katıldın"). A phrase whose clause doesn't end in that past stays as it is.
 */
function turkishVerbForYou(verb: string): string {
  return verb.replace(/^([^:]*?[dt][ıiuü])(\s*(?::|$))/u, '$1n$2')
}

/** The page's language and title: Bora Rachar only in Brazil's Portuguese, Blitz Split everywhere else. */
export function applyDocumentLocale() {
  document.documentElement.lang = documentLanguage
  document.title = t('appName')
}
