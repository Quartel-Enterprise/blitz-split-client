---
name: add-language
description: "Add a language to Bora Rachar on every platform: the AppLanguage entry and the pickers, the Android values-xx strings, the Apple Localizable.xcstrings entries, the web dictionary, the second person of the activity sentences, the docs and the translation checks. Use whenever someone asks to support, add or translate the app into another language (\"adiciona alemão\", \"suporta italiano\")."
---

# Add a language

French (PR #249) was the pilot, Spanish (PR #222) the model before it: their diffs show every place a language
touches. This skill is the order of the work and the decisions to take, not the rules: the product rules are
in [docs/product/general.md](../../../docs/product/general.md) and [profile.md](../../../docs/product/profile.md).

Start it with `worktree-start-task` (type `feature`, `add-<language>`). One language per branch: two branches adding a
language both edit `Localizable.xcstrings`, `webApp/src/i18n/index.ts` and the docs, so run them one at a time or
merge the first before starting the next.

## 1. Ask Pierre what the product leaves open

Product choices nobody wrote down are Pierre's call. Ask, in one question, only what the language needs:

- **The address**: informal or formal ("tu" or "vous", "du" or "Sie", "tú" or "usted"). Spanish and French are informal:
  friends splitting a bill.
- **Variants**: one for every tag of the language, unless Pierre wants a regional one (Spanish has `es` and a partial
  `es-ES`; `pt-PT` is not separate). A regional variant holds only the words the region says differently.
- **The name**: Blitz Split in every language but Portuguese, so nothing to ask unless the language needs another one.
- **A language that reads right to left** (Arabic, Hebrew) or whose plural has more than `one`/`other` (Russian, Polish,
  Arabic): say so, it is more than a translation (RTL layouts on three UIs; more plural categories in every `plurals`
  and plural variation).

Write the decisions in `docs/product/general.md`, next to the Spanish and French ones.

## 2. Write the translation rules once

Create a glossary and style file in the scratchpad, as `fr-glossary.md` was: the tone, the typography of the language
(French's no-break spaces before `: ; ! ?`, its apostrophe), the placeholders to keep (`%s` `%d` `%1$s` `%lld` `%@`
`{name}`, the private-use stand-ins of the Apple activity sentences), the plural categories, the product's words
(group, bill, balance, settle up, member, recovery code…) in the language, and the activity sentence rule of step 4.
Every translator, human or agent, reads it first; without it each one picks its own words.

## 3. The code

- `core/model` `AppLanguage`: the new entry (its BCP-47 tag), the `fromTag` comment, and a test in `AppLanguageTest`
  for its tags and its `toTag`. `ProfileUiStateTest` lists every language the picker offers.
- The picker names each language in the app's language ("Inglês" in Portuguese, "English" in English), with the region
  when the language is written for one ("Português (BR)"): a new language adds its name **to every existing language**,
  and the existing languages' names to its own. Android: a `profile_language_<name>` string in
  `android/feature/profile`'s `values`, `values-pt`, `values-es`, `values-fr` and the new `values-xx`, read by
  `languageName` in `LanguageSheet.kt`. Apple: the English literal ("French") in `LanguageName` of `ProfileView.swift`
  and its entry in `Localizable.xcstrings`. Web: a `profile_language<Name>` message in every dictionary and the
  `languageNames` map of `ProfileScreen.tsx`. Decide with Pierre whether the language carries a region.
- Web: `webApp/src/i18n/index.ts` (`Locale`, `messages`, `pickLocale`, `documentLanguages`) and the `isSupportedTag`
  pattern of `webApp/src/lib/currency.ts`, which keeps the currency names in the UI's language.
- Android needs nothing else: `generateLocaleConfig` builds the per-app language list from the `values-xx` folders.
  Apple needs nothing else either: the languages are the ones `Localizable.xcstrings` has.

## 4. The activity sentences

An activity row is "<actor> <verb phrase> <object>", one sentence for every actor. A language that conjugates by person
needs the user's rows turned to the second person: `toSecondPerson` (Android `SecondPersonVerbs.kt`), `SecondPerson`
(Apple) and `verbForYou` (web `i18n/index.ts`) do it the same way on the three platforms. Spanish rewrites the verb's
ending; French requires every verb phrase to be a passé composé starting with "a" and turns it into "as". Pick the
simplest rule the language allows, put it in the glossary, add it to the three helpers and describe it in
[activity-and-push.md](../../../docs/product/activity-and-push.md). A language whose verb does not change with the
person needs none of this.

## 5. The strings

Translate from the English, never from another translation:

- **Android**: `res/values-xx/strings.xml` next to each `res/values/strings.xml` (about 28 modules, `androidApp` and
  `android/designsystem` included), same keys, same order, every plural category the language needs.
- **Apple**: an `xx` localization for every key of `iosApp/Resources/Localizable.xcstrings` and
  `InfoPlist.xcstrings`, with the same plural and device variations as `es`. Edit the JSON with a script that keeps
  Xcode's formatting, so the diff shows only the added entries.
- **Web**: `webApp/src/i18n/xx.ts`, typed by `MessageKey`, exporting `xx`.

This is a lot of text and the files are independent, so hand each platform (the Android modules in two halves) to a
subagent with the glossary, and tell it not to build or commit. Read what comes back: have it list the strings it was
unsure about.

## 6. Make the checks know it

- `iosApp/check-translations.py`: add the language to `REQUIRED`, so a key without it fails the build.
- Android's `lintDebug` already fails on a missing or extra `values-xx` string, and `npm --prefix webApp run
  check:messages` on a web key or `{placeholder}` that differs from English: they need no change.
- The docs that list the languages: `CLAUDE.md`, `README.md`, `docs/ai_agents.md`, `docs/architecture/code-style.md`,
  `docs/architecture/new-feature-checklist.md`, `docs/product/general.md`, `docs/product/profile.md`,
  `.claude/skills/review-conventions/SKILL.md`, the comments of `androidApp/build.gradle.kts` and `iosApp/project.yml`.
  `grep -rn "Spanish"` finds the places the last language reached.

## 7. Verify

Through `./scripts/heavy-check.sh`, one build at a time: `./gradlew jvmTest`, `./gradlew lintDebug`,
`./scripts/ktlint.sh`, `npm --prefix webApp run check:messages` and `npm --prefix webApp run build`,
`python3 iosApp/check-translations.py iosApp/Resources`, then the Apple build (it runs the check too). Then look at the
language in each app: Profile › Language picks it, a long sentence does not break a layout (German, Finnish), and an
Activity row of the user's reads right in the second person.

Open the pull request with `create-pr`. The push notifications need no extra step: each device words them in its own
language from a kind and the names, with the `push.*` keys of the same string files.
