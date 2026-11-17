// Fails the build when a language of src/i18n holds a message that doesn't match English's: a missing or an extra key,
// an empty value, or a {placeholder} that English doesn't have (or lacks), which would print "{name}" on screen.
//
// tsc already fails on a missing or extra key, since every language is typed by English's keys; this adds what the
// types can't see: the placeholders and the empty values. A regional file (esES.ts, a language and a region) holds only
// the words that region says differently, so it may lack keys, but never has an extra one.
//
// A language with two plurals where English has one (Polish's few and many, Czech's few and other, Romanian's few and
// other) writes them as "few|many" (or "few|other") in an `…Other` message with a {count}; one whose singular also takes counts past 1 (Russian's and Ukrainian's 21) writes three,
// "one|few|many". A language with a dual (Arabic) writes every category it has, in CLDR's order:
// "zero|one|two|few|many|other". `t()` picks one. A "|" anywhere else, in another language, or another number of forms, would print
// on screen or never be picked.
//
// Runs on the TypeScript sources through Node's own type stripping, with no build:  node scripts/check-messages.mjs
import { readdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const directory = new URL('../src/i18n/', import.meta.url)
const isRegional = (name) => /^[a-z]{2}[A-Z]{2}$/.test(name)
// The names, once each: a language may say a name twice where English says it once
const placeholdersOf = (message) => [...new Set([...message.matchAll(/\{(\w+)\}/g)].map((match) => match[1]))].sort().join(',')

const { en } = await import(new URL('en.ts', directory))
const languages = readdirSync(fileURLToPath(directory))
  .filter((file) => file.endsWith('.ts') && !['index.ts', 'en.ts'].includes(file))
  .map((file) => file.slice(0, -'.ts'.length))

// Why: a second form only means something where the plural has a few (Polish, Czech, Romanian); anywhere else t()
// would show one form for every count.
const categoriesOf = (language) => new Intl.PluralRules(language.slice(0, 2)).resolvedOptions().pluralCategories
const hasFew = (language) => categoriesOf(language).includes('few')
// Why: a language with a dual holds every category it has, in this order (Arabic's six).
const everyForm = (language) => {
  const categories = categoriesOf(language)
  if (!categories.includes('two')) return undefined
  return ['zero', 'one', 'two', 'few', 'many', 'other'].filter((category) => categories.includes(category))
}

// Why: Russian's and Ukrainian's 21 takes the singular, which an …Other message must then hold too: one|few|many.
const pluralFormCount = (language) => (new Intl.PluralRules(language.slice(0, 2)).select(21) === 'one' ? 3 : 2)

let failures = 0
const fail = (language, key, problem) => {
  console.error(`webApp/src/i18n/${language}.ts: error: "${key}" ${problem}`)
  failures += 1
}

for (const language of languages) {
  const messages = (await import(new URL(`${language}.ts`, directory)))[language]
  if (!messages) {
    console.error(`webApp/src/i18n/${language}.ts: error: it must export its messages as \`${language}\``)
    failures += 1
    continue
  }
  for (const key of Object.keys(en)) {
    if (!(key in messages)) {
      if (!isRegional(language)) fail(language, key, 'is missing')
    } else if (typeof messages[key] !== 'string' || messages[key].trim() === '') {
      fail(language, key, 'is empty')
    } else if (messages[key].includes('|')) {
      const forms = messages[key].split('|')
      if (!hasFew(language) && !everyForm(language)) {
        fail(language, key, 'has a "|", which only a language with a few plural (Polish, Russian, Ukrainian, Czech, Romanian) may hold')
        continue
      }
      // Why: a …WordOther message is the word beside a count drawn apart ("7 payments" with a big 7), whose caller
      // passes the count with no {count} to print.
      if (!key.endsWith('Other') || !(/\{count\}/.test(en[key]) || key.endsWith('WordOther'))) {
        fail(language, key, 'has a "|", which only an …Other message with a {count}, or a …WordOther one, may hold')
        continue
      }
      const every = everyForm(language)
      const formCount = every ? every.length : pluralFormCount(language)
      if (forms.length !== formCount || forms.some((form) => form.trim() === '')) {
        const expected = every ? `"${every.join('|')}"` : formCount === 3 ? '"one|few|many"' : '"few|many"'
        fail(language, key, `must hold exactly ${formCount} plural forms, ${expected}`)
        continue
      }
      for (const form of forms) {
        if (placeholdersOf(form) !== placeholdersOf(en[key])) {
          fail(language, key, `has a form with the placeholders {${placeholdersOf(form)}} instead of English's {${placeholdersOf(en[key])}}`)
        }
      }
    } else if (placeholdersOf(messages[key]) !== placeholdersOf(en[key])) {
      fail(language, key, `has the placeholders {${placeholdersOf(messages[key])}} instead of English's {${placeholdersOf(en[key])}}`)
    }
  }
  for (const key of Object.keys(messages)) {
    if (!(key in en)) fail(language, key, "is not in English's")
  }
}

if (failures > 0) process.exit(1)
console.log(`Checked ${languages.length} languages against English: ${Object.keys(en).length} messages each.`)
