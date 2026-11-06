// Fails the build when a language of src/i18n holds a message that doesn't match English's: a missing or an extra key,
// an empty value, or a {placeholder} that English doesn't have (or lacks), which would print "{name}" on screen.
//
// tsc already fails on a missing or extra key, since every language is typed by English's keys; this adds what the
// types can't see: the placeholders and the empty values. A regional file (esES.ts, a language and a region) holds only
// the words that region says differently, so it may lack keys, but never has an extra one.
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
