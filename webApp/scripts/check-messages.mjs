// Fails the build when a language of src/i18n holds a message that doesn't match English's: a missing or an extra key,
// an empty value, or a {placeholder} that English doesn't have (or lacks), which would print "{name}" on screen.
//
// tsc already fails on a missing or extra key, since every language is typed by English's keys; this adds what the
// types can't see: the placeholders and the empty values. A regional file (esES.ts, a language and a region) holds only
// the words that region says differently, so it may lack keys, but never has an extra one.
//
// A language with two plurals where English has one (Polish's few and many) writes them as "few|many" in an `…Other`
// message with a {count}; `t()` picks one. A "|" anywhere else, in another language, or forms that aren't two, would
// print on screen or never be picked.
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

// Why: "few|many" only means something where the plural has both (Polish); anywhere else t() would show one form for
// every count.
const hasFewAndMany = (language) => {
  const categories = new Intl.PluralRules(language.slice(0, 2)).resolvedOptions().pluralCategories
  return categories.includes('few') && categories.includes('many')
}

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
      if (!hasFewAndMany(language)) {
        fail(language, key, 'has a "|", which only a language with few and many plurals (Polish) may hold')
        continue
      }
      if (!key.endsWith('Other') || !/\{count\}/.test(en[key])) {
        fail(language, key, 'has a "|", which only an …Other message with a {count} may hold')
        continue
      }
      if (forms.length !== 2 || forms.some((form) => form.trim() === '')) {
        fail(language, key, 'must hold exactly two plural forms, "few|many"')
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
