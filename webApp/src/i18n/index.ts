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
import { ar } from './ar'
import { zh } from './zh'
import { he } from './he'
import { th } from './th'
import { vi } from './vi'
import { zhHant } from './zhHant'
import { sv } from './sv'
import { cs } from './cs'
import { ro } from './ro'
import { bn } from './bn'
import { ms } from './ms'
import { el } from './el'
import { hr } from './hr'
import { hu } from './hu'
import { ur } from './ur'
import { fil } from './fil'
import { sr } from './sr'
import { sk } from './sk'
import { fa } from './fa'
import { ta } from './ta'
import { nb } from './nb'
import { te } from './te'
import { bg } from './bg'
import { mr } from './mr'
import { sw } from './sw'
import { ca } from './ca'
import { da } from './da'
import { fi } from './fi'

export type { MessageKey }
/**
 * `es` is the neutral Latin-American Spanish; `es-ES` is Spain's, which overrides only the words it says differently.
 * `pt` is Brazil's Portuguese; `pt-PT` is Portugal's, which overrides only the words it says differently.
 * `zh` is Simplified Chinese, mainland China's; `zh-Hant` is Traditional Chinese, Taiwan's.
 */
export type Locale = 'en' | 'pt' | 'pt-PT' | 'es' | 'es-ES' | 'fr' | 'de' | 'it' | 'pl' | 'nl' | 'ja' | 'tr' | 'id' | 'ru' | 'uk' | 'ko' | 'hi' | 'ar' | 'zh' | 'he' | 'th' | 'vi' | 'zh-Hant' | 'sv' | 'cs' | 'ro' | 'bn' | 'ms' | 'el' | 'hr' | 'hu' | 'ur' | 'fil' | 'sr' | 'sk' | 'fa' | 'ta' | 'nb' | 'te' | 'bg' | 'mr' | 'sw' | 'ca' | 'da' | 'fi'

const messages: Record<Locale, Record<MessageKey, string>> = { en, pt, 'pt-PT': { ...pt, ...ptPT }, es, 'es-ES': { ...es, ...esES }, fr, de, it, pl, nl, ja, tr, id, ru, uk, ko, hi, ar, zh, he, th, vi, 'zh-Hant': zhHant, sv, cs, ro, bn, ms, el, hr, hu, ur, fil, sr, sk, fa, ta, nb, te, bg, mr, sw, ca, da, fi }

// Why: the languages with one variant for every tag of theirs.
const bareLocales: readonly Locale[] = ['en', 'fr', 'de', 'it', 'pl', 'nl', 'ja', 'tr', 'id', 'ru', 'uk', 'ko', 'hi', 'ar', 'zh', 'he', 'th', 'vi', 'sv', 'cs', 'ro', 'bn', 'ms', 'el', 'hr', 'hu', 'ur', 'fil', 'sr', 'sk', 'fa', 'ta', 'nb', 'te', 'bg', 'mr', 'sw', 'ca', 'da', 'fi']

/**
 * A Chinese tag's subtags written in Traditional characters: its script says so when it has one (zh-Hant-CN is
 * Traditional, zh-Hans-HK Simplified); without one, a region that writes them does (Taiwan, Hong Kong, Macau). Both
 * are read before any extension, as AppLanguage's.
 */
function isTraditionalChinese(subtags: readonly string[]): boolean {
  const end = subtags.findIndex((subtag) => subtag.length < 2)
  const tagSubtags = end === -1 ? subtags : subtags.slice(0, end)
  const script = tagSubtags.find((subtag) => /^[a-z]{4}$/.test(subtag))
  if (script) return script === 'hant'
  const region = regionOf(subtags)
  return region === 'tw' || region === 'hk' || region === 'mo'
}

// Why: the first two-letter subtag before any extension ("-u-fw-mon", whose keys are no region), as AppLanguage.regionOf.
function regionOf(subtags: readonly string[]): string | undefined {
  const end = subtags.findIndex((subtag) => subtag.length < 2)
  return (end === -1 ? subtags : subtags.slice(0, end)).find((subtag) => subtag.length === 2)
}

/**
 * The first browser language the app has, in the browser's order: `pt-PT` (exactly Portugal) is Portugal's Portuguese,
 * every other `pt` tag (pt, pt-BR…) is Brazil's, `es-ES` (Spain) is Spain's Spanish, every other `es` tag (es, es-MX,
 * es-419, es-AR…) is the neutral Latin-American Spanish, any `fr` tag (fr, fr-FR, fr-CA…) is French, any `de`, `it`,
 * `pl`, `nl`, `ja`, `tr`, `id`, `ru`, `uk`, `ko`, `hi`, `ar`, `th`, `vi`, `sv`, `cs`, `ro`, `bn`, `ms`, `el`, `hr`, `hu`, `ur`, `fil`, `sr`, `sk`, `fa`, `ta` , `nb` (or Norwegian's `no` and Nynorsk's `nn`) `te` , `bg`, `mr`, `sw`, `ca`, `da` or `fi` tag
 * (de-AT, it-CH, pl-PL, nl-BE, ja-JP, tr-CY, id-ID, the legacy in-ID, ru-KZ, uk-UA, ko-KR, hi-IN, ar-EG, th-TH, vi-VN,
 * sv-FI, cs-CZ, ro-MD, bn-IN, ms-SG, el-CY, hr-BA, hu-RO, ur-PK, fil-PH, sr-RS, sr-Latn-RS, sr-Cyrl-RS, sk-SK, fa-IR, fa-AF, ta-LK, nb-NO, nn-NO, te-IN, bg-BG, mr-IN, sw-KE, sw-TZ, ca-ES, ca-ES-valencia, da-DK, fi-FI…) is German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian,
 * Russian, Ukrainian, Korean, Hindi, Arabic, Thai, Vietnamese, Swedish, Czech, Romanian, Bengali, Malay, Greek, Croatian, Hungarian, Urdu, Filipino, Serbian (in Latin, whatever its script), Slovak, Persian (Iran's, fa-AF too), Tamil , Norwegian (Bokmål, whichever of the three) Telugu, Bulgarian, Marathi, Swahili, Catalan (Valencian too), Danish or Finnish, a Traditional `zh` tag (zh-Hant, zh-TW, zh-HK, zh-MO) is Traditional Chinese,
 * any other `zh` tag (zh-CN, zh-Hans, zh-SG…) is Simplified Chinese, any `en` tag is English, and English when none
 * matches.
 */
export function pickLocale(languages: readonly string[]): Locale {
  for (const tag of languages) {
    const [subtag, ...rest] = tag.toLowerCase().split(/[-_]/)
    // Why: "in" is Indonesian's legacy code and "tl" Tagalog's (Filipino), which older Android WebViews still send, and
    // Norwegian's "no" and Nynorsk's "nn" read as Bokmål; AppLanguage.fromTag maps them too.
    const language = subtag === 'in' ? 'id' : subtag === 'tl' ? 'fil' : subtag === 'no' || subtag === 'nn' ? 'nb' : subtag
    if (language === 'zh' && isTraditionalChinese(rest)) return 'zh-Hant'
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
const isUnspaced = locale === 'ja' || locale === 'zh' || locale === 'zh-Hant'

/**
 * What goes between the pieces of a sentence the code joins ("Ana" and "changed the category", two sentences of a
 * detail): a space, but nothing in Japanese and Chinese, which write "Anaがカテゴリを変更しました". Same rule as
 * Android's `wordGapUi`.
 */
export const wordGap = isUnspaced ? '' : ' '

/**
 * The end of a sentence the code finishes: Japanese's and Chinese's "。", Hindi's and Bengali's danda "।", Urdu's "۔", nothing
 * in Thai, which ends a sentence with none (the word gap's space parts two), the others' ".". Same rule as Android's
 * `fullStopUi`.
 */
const ownFullStops: Partial<Record<Locale, string>> = { hi: '।', bn: '।', ur: '۔', th: '' }
export const fullStop = isUnspaced ? '。' : (ownFullStops[locale] ?? '.')

/**
 * Between two clauses of one sentence the code joins ("…pays you $64, and Ana no longer owes you"): Japanese ends the
 * first sentence instead ("…払います。…なくなりました"), Chinese takes its own comma ("…，Ana不再欠你"), Urdu and Persian their "،",
 * as their Android strings write it, and Thai, which has no comma, a space.
 */
const ownClauseBreaks: Partial<Record<Locale, string>> = { ja: '。', zh: '，', 'zh-Hant': '，', ur: '، ', fa: '، ', th: ' ' }
export const clauseBreak = ownClauseBreaks[locale] ?? ', '

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
  ar: 'ar',
  zh: 'zh-Hans',
  he: 'he',
  th: 'th',
  vi: 'vi',
  'zh-Hant': 'zh-Hant',
  sv: 'sv',
  cs: 'cs',
  ro: 'ro',
  bn: 'bn',
  ms: 'ms',
  el: 'el',
  hr: 'hr',
  hu: 'hu',
  ur: 'ur',
  fil: 'fil',
  sr: 'sr-Latn',
  sk: 'sk',
  fa: 'fa',
  ta: 'ta',
  nb: 'nb',
  te: 'te',
  bg: 'bg',
  mr: 'mr',
  sw: 'sw',
  ca: 'ca',
  da: 'da',
  fi: 'fi',
}

const documentLanguage = documentLanguages[locale]

// Why: the languages written right to left; "iw" is Hebrew's legacy code, which older browsers still send.
const rightToLeftLanguages: ReadonlySet<string> = new Set(['ar', 'fa', 'he', 'iw', 'ur'])

/** Whether `tag` ("ar", "he-IL", "ar_EG", "ur-PK", "fa-IR") is a language written right to left. */
export function isRightToLeft(tag: string): boolean {
  return rightToLeftLanguages.has(tag.toLowerCase().split(/[-_]/)[0])
}

/** The page's direction, from the app's language: `rtl` for Arabic, Hebrew, Urdu and Persian, `ltr` for the others. */
export const direction: 'ltr' | 'rtl' = isRightToLeft(documentLanguage) ? 'rtl' : 'ltr'

/** "→", the arrow that points the way a line reads: "←" in a right-to-left language, where arrows don't mirror. */
export const forwardArrow = direction === 'rtl' ? '←' : '→'

/**
 * `text` kept in one piece inside a right-to-left sentence: wrapped in FIRST STRONG ISOLATE … POP DIRECTIONAL
 * ISOLATE, so "−R$ 118,50" or a symbol's "US$" keeps its order among Arabic, Hebrew, Urdu or Persian words. A left-to-right language
 * gets `text` unchanged.
 */
export function isolated(text: string): string {
  return direction === 'rtl' ? `\u2068${text}\u2069` : text
}

/**
 * The Unicode extension keys a language's `Intl` formatting needs: Latin digits for Arabic, Thai, Bengali, Urdu, Persian, Tamil, Telugu and Marathi,
 * whose own ("١٢٫٥٠", "๑๒.๕๐", "১২.৫০", "۱۲٫۵۰", "௧௨.௫௦") the amount fields can't parse, and the Gregorian calendar for Thai,
 * whose default is the Buddhist era (2569 for 2026), and Persian, whose default is the Solar Hijri one (1405 for 2026),
 * so the web writes the year Android writes.
 */
const formattingKeys: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  ar: { nu: 'latn' },
  bn: { nu: 'latn' },
  ur: { nu: 'latn' },
  fa: { nu: 'latn', ca: 'gregory' },
  ta: { nu: 'latn' },
  te: { nu: 'latn' },
  mr: { nu: 'latn' },
  th: { nu: 'latn', ca: 'gregory' },
}

/**
 * The tag to hand `Intl` for numbers, amounts and dates: `tag` itself, but with Latin digits (`-u-nu-latn`) for
 * Arabic, Thai, Bengali, Urdu and Persian, so an amount reads "12.50" as the amount fields type it, and the Gregorian
 * calendar (`-u-ca-gregory`) for Thai and Persian. Every other language gets `tag` unchanged.
 */
export function formattingLocale(tag: string): string {
  const wanted = formattingKeys[tag.toLowerCase().split(/[-_]/)[0]]
  if (wanted === undefined) return tag
  const [base, extension] = tag.split(/-u-/i)
  const kept = Object.keys(wanted).reduce(
    (keys, key) => keys.replace(new RegExp(`(^|-)${key}-[a-z0-9]{3,8}`, 'i'), ''),
    extension ?? '',
  ).replace(/^-/, '')
  const added = Object.entries(wanted).map(([key, value]) => `${key}-${value}`).join('-')
  return `${base}-u-${kept ? `${kept}-` : ''}${added}`
}

const pluralRules = new Intl.PluralRules(documentLanguage)

// Why: the plural categories of a language with a dual (Arabic's zero, one, two, few, many, other), in the order its
// "|" forms are written; undefined for the others, whose forms pluralFormOf picks by their count.
const everyFormOrder: readonly Intl.LDMLPluralRule[] | undefined = (() => {
  const categories = pluralRules.resolvedOptions().pluralCategories
  if (!categories.includes('two')) return undefined
  return (['zero', 'one', 'two', 'few', 'many', 'other'] as const).filter((category) => categories.includes(category))
})()

// Why: Czech's, Slovak's and Romanian's second form is the one their `other` takes ("5 skupin", "20 de grupuri"), their `many`
// being Czech's fractions, which say the few form ("1,5 skupiny"), where Polish's is the one its `many` takes.
const secondFormIsOther = locale === 'cs' || locale === 'sk' || locale === 'ro'

// Why: Croatian's third form is the one its other takes ("5 grupa"), its plural having no many.
const thirdFormIsOther = !pluralRules.resolvedOptions().pluralCategories.includes('many')

/**
 * Polish says "2 grupy" but "5 grup": where the other languages have one plural, it has few (2–4, 22–24…) and many (0,
 * 5–21…). Its message for a `…Other` key with a `{count}` holds both, "{count} grupy|{count} grup", and this picks the
 * one `count` takes (a fraction taking few's, as Polish does). Russian and Ukrainian also say 21 in the singular ("21
 * группа"), so theirs hold three, one|few|many: "{count} группа|{count} группы|{count} групп". A fraction takes few's,
 * which is Russian's own; Ukrainian has a form of its own for it ("1,5 платежу"), which no count the app passes needs.
 * Czech ("{count} skupiny|{count} skupin") and Romanian ("{count} grupuri|{count} de grupuri") hold two forms too,
 * their second one being the one their other takes (`secondFormIsOther`). Croatian says 21 in the singular too, and
 * holds three, one|few|other ("{count} grupa|{count} grupe|{count} grupa"), the third being its other's
 * (`thirdFormIsOther`), a fraction taking the form CLDR gives it. Arabic has a dual, so its messages hold every
 * category it has, in CLDR's order, "zero|one|two|few|many|other" (`everyFormOrder`), and so does any language whose
 * plural has a two. Any other message has one form. `count` must be the number itself, not a formatted string: one that
 * isn't a number gets the many form, the one most counts take (other, in a language that holds every form).
 */
function pluralFormOf(message: string, count: string | number | undefined): string {
  if (!message.includes('|')) return message
  const forms = message.split('|')
  if (everyFormOrder) {
    const category = typeof count === 'number' ? pluralRules.select(count) : 'other'
    return forms[everyFormOrder.indexOf(category)] ?? forms[forms.length - 1]
  }
  const category = typeof count === 'number' ? pluralRules.select(count) : 'many'
  if (forms.length === 3) {
    const isLast = category === 'many' || (thirdFormIsOther && category === 'other')
    return forms[category === 'one' ? 0 : isLast ? 2 : 1]
  }
  if (secondFormIsOther) return category === 'other' || typeof count !== 'number' ? forms[1] : forms[0]
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

// Why: the indefinite pasts Hungarian's verb phrases start with, with their second person.
const hungarianIndefinitePast: Readonly<Record<string, string>> = {
  felosztott: 'felosztottál',
  szerkesztett: 'szerkesztettél',
  törölt: 'töröltél',
  rögzített: 'rögzítettél',
  visszavont: 'visszavontál',
  hozzáadott: 'hozzáadtál',
  csatlakozott: 'csatlakoztál',
  kilépett: 'kiléptél',
  eltávolított: 'eltávolítottál',
  átnevezett: 'átneveztél',
  hozzászólt: 'hozzászóltál',
  csatolt: 'csatoltál',
  kért: 'kértél',
  küldött: 'küldtél',
  létrehozott: 'létrehoztál',
  módosított: 'módosítottál',
  megváltoztatott: 'megváltoztattál',
  elolvasott: 'elolvastál',
  bekapcsolt: 'bekapcsoltál',
  kikapcsolt: 'kikapcsoltál',
  fizetett: 'fizettél',
}

// Why: the object pronoun Spanish puts before the verb ("lo adjuntó"), which stays as the verb changes ("lo adjuntaste").
const spanishObjectPronouns = new Set(['lo', 'la', 'los', 'las', 'le', 'les'])

// Why: each language's perfect auxiliary in the third person, with its second person.
// Why: Czech and Slovak put the second person's auxiliary before a past in "l(a)", which the third person says without one.
const pastAuxiliaries: Partial<Record<Locale, string>> = { cs: 'jsi', sk: 'si' }

const secondPersonAuxiliaries: Partial<Record<Locale, Record<string, string>>> = {
  fr: { a: 'as', 'l’a': 'l’as', "l'a": "l'as" },
  de: { hat: 'hast', ist: 'bist' },
  it: { ha: 'hai', 'l’ha': 'l’hai', "l'ha": "l'hai" },
  nl: { heeft: 'hebt', is: 'bent' },
  ro: { a: 'ai' },
  hr: { je: 'si' },
  sr: { je: 'si' },
  fi: { on: 'olet' },
  ca: { ha: 'has', 'l’ha': 'l’has', "l'ha": "l'has" },
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
 * own particle, so its verb phrases all start with "님이", which the user's drops: "님이 나눴어요" becomes "나눴어요". Hindi does
 * the same with its ergative "ने", the user's "आपने" holding it: "ने खर्च बांटा" becomes "खर्च बांटा". Romanian swaps
 * its perfect's auxiliary as French does ("a împărțit" becomes "ai împărțit"), Catalan its "ha" ("ha dividit" becomes "has dividit"), Croatian its "je" ("je podijelio/la"
 * becomes "si podijelio/la"), Finnish its "on" ("on jakanut" becomes "olet jakanut"), and Czech puts the auxiliary "jsi"
 * before a past in "l(a)", which the third person says without one ("rozdělil(a)" becomes "jsi rozdělil(a)"), as Slovak does with "si"
 * ("rozdelil(a)" becomes "si rozdelil(a)"). Greek
 * ends its aorist in "-ες" for the user, an object pronoun staying before it ("μοίρασε" becomes "μοίρασες", "το
 * επισύναψε" "το επισύναψες"). Hungarian starts each with a past: a definite one in "-ta"/"-te" takes a "d" ("létrehozta"
 * becomes "létrehoztad"), an indefinite one is one of a short list a translation may start with ("felosztott" becomes
 * "felosztottál"). Bengali's respectful verb is the same for a name and for "আপনি", and Malay's never changes.
 * Urdu writes its ergative "نے" apart after a name and after the user's "آپ" alike, so its verb stays as it is.
 * Filipino's "ay" and actor-focus verb are the same for "Ikaw" and for a name, so its verb stays as it is too, and so
 * does Bulgarian's aorist, the same for "Ти" and for a name ("раздели").
 * Persian puts the verb last as Turkish does, so its verb phrases end with a simple past, which takes the respectful
 * "ید" of "شما": "تقسیم کرد" becomes "تقسیم کردید".
 * Tamil ones end, before the colon that leads to the object, with a respectful past in "-ார்", which becomes the user's
 * "-ீர்கள்": "பிரித்தார்" becomes "பிரித்தீர்கள்".
 * Telugu's respectful past "-ారు" is the same for a name and for "మీరు", so its verb stays as it is. Marathi does as
 * Hindi with its respectful agent "यांनी", the user's "तुम्ही" taking none: "यांनी खर्च वाटला" becomes "खर्च वाटला".
 * Swahili's verb carries the subject's prefix: every verb phrase starts with a perfect "a-me-" or a past "a-li-", whose
 * "a" becomes the user's "u" ("amegawanya" becomes "umegawanya").
 * Portugal's Portuguese ("tu") ends the preterite in "-aste", "-este" or "-iste": "dividiu" becomes "dividiste". Same
 * rules as Android's `toSecondPerson` and the Apple app's `SecondPerson`.
 */
export function verbForYou(verb: string): string {
  if (locale === 'pt-PT') return portugueseVerbForYou(verb)
  const auxiliary = secondPersonAuxiliaries[locale]
  if (auxiliary) {
    const [first, ...rest] = verb.split(' ')
    return Object.hasOwn(auxiliary, first) ? [auxiliary[first], ...rest].join(' ') : verb
  }
  if (locale === 'tr') return turkishVerbForYou(verb)
  if (locale === 'fa') return persianVerbForYou(verb)
  if (locale === 'ta') return tamilVerbForYou(verb)
  if (locale === 'ko') return verb.replace(/^님이(?: |$)/, '')
  if (locale === 'hi') return verb.replace(/^ने(?: |$)/, '')
  if (locale === 'mr') return verb.replace(/^यांनी(?: |$)/, '')
  if (locale === 'sw') return verb.replace(/^a(?=(?:me|li)\S)/, 'u')
  if (locale === 'pl') return verb.replace(/^(\S*)ł\(a\)(?= |$)/, '$1łeś(-aś)')
  const pastAuxiliary = pastAuxiliaries[locale]
  if (pastAuxiliary) return /^\S*l\(a\)(?=[\s,:]|$)/.test(verb) ? `${pastAuxiliary} ${verb}` : verb
  if (locale === 'hu') {
    return verb.replace(/^[^\s,:]+/, (past) => hungarianIndefinitePast[past] ?? (/t[ae]$/.test(past) ? `${past}d` : past))
  }
  if (locale === 'el') return verb.replace(/^(?:(?:το|τη|την|τον|τα|τους|τις) )?\S*ε(?=[\s,:]|$)/, '$&ς')
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
  return verbLastForYou(verb, '[dt][ıiuü]', 'n')
}

/**
 * A language that puts its verb last (Turkish, Persian): the clause before the colon that leads to the object, if any,
 * ends with a simple past matching `past`, which takes `ending` for the user. Same rule as Android's
 * `toVerbLastSecondPerson` and the Apple app's `verbLastForYou`.
 */
function verbLastForYou(verb: string, past: string, ending: string, dropsPast = false): string {
  const kept = dropsPast ? `([^:]*?)(?:${past})` : `([^:]*?${past})`
  return verb.replace(new RegExp(`^${kept}(\\s*(?::|$))`, 'u'), `$1${ending}$2`)
}

/**
 * Tamil puts the verb last too, so every Tamil verb phrase ends, before the colon that leads to the object, with a
 * respectful past in "-ார்", which the second person drops for "-ீர்கள்" ("பிரித்தார்" → "பிரித்தீர்கள்").
 */
function tamilVerbForYou(verb: string): string {
  return verbLastForYou(verb, 'ார்', 'ீர்கள்', true)
}

/**
 * Persian puts the verb last, so every Persian verb phrase ends with its simple past in the third person singular, its
 * past stem, which ends in "د" or "ت", before the colon that leads to the object if there is one ("هزینه‌ای را تقسیم
 * کرد:"), and the user's respectful "شما" adds "ید" to it ("تقسیم کرد" → "تقسیم کردید", "پیوست" → "پیوستید"). A phrase
 * whose clause doesn't end in that past stays as it is.
 */
function persianVerbForYou(verb: string): string {
  return verbLastForYou(verb, '[دت]', 'ید')
}

/** The page's language, direction and title: Bora Rachar only in Brazil's Portuguese, Blitz Split everywhere else. */
export function applyDocumentLocale() {
  document.documentElement.lang = documentLanguage
  document.documentElement.dir = direction
  document.title = t('appName')
}
