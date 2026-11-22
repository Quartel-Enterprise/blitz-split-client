---
name: add-language
description: "Add a language to Bora Rachar on every platform: the AppLanguage entry and the pickers, the Android values-xx strings, the Apple Localizable.xcstrings entries, the web dictionary, the second person of the activity sentences, the docs and the translation checks. Use whenever someone asks to support, add or translate the app into another language (\"adiciona alemão\", \"suporta italiano\")."
---

# Add a language

French (PR #249) was the pilot, Spanish (PR #222) the model before it; Russian and Ukrainian (PR #396) and Korean
are the latest, with the store screenshots and a currency: their diffs show every place a language touches. This skill is the order of the work and the decisions to take, not the rules: the product rules are
in [docs/product/general.md](../../../docs/product/general.md) and [profile.md](../../../docs/product/profile.md).

Start it with `worktree-start-task` (type `feature`, `add-<language>`). One language per branch: two branches adding a
language both edit `Localizable.xcstrings`, `webApp/src/i18n/index.ts` and the docs, so run them one at a time or
merge the first before starting the next.

## 1. Ask Pierre what the product leaves open

Product choices nobody wrote down are Pierre's call. Ask, in one question, only what the language needs:

- **The address**: informal or formal ("tu" or "vous", "du" or "Sie", "tú" or "usted"). Spanish and French are informal:
  friends splitting a bill.
- **Variants**: one for every tag of the language, unless Pierre wants a regional one (Spanish has `es` and a partial
  `es-ES`, Portuguese `pt-BR` and a partial `pt-PT`, each its own row of the picker). A regional variant holds only the words the region says differently.
- **The name**: Blitz Split in every language but Portuguese, so nothing to ask unless the language needs another one.
- **A language that reads right to left** (Arabic, Hebrew) or whose plural has more than `one`/`other` (Russian, Polish,
  Arabic): say so, it is more than a translation (RTL layouts on three UIs; more plural categories in every `plurals`
  and plural variation).
- **A language whose particles or suffixes follow the sound of the word before them** (Turkish, Korean, Hungarian,
  Finnish): ask whether to never glue one to a value the app fills in (Turkish's and Korean's answer: "Ödeme: Ana",
  "Ana 님이") or to pick it by sound in code on three platforms.
- **Its currency**: when the app has none for the language's main country (`GroupCurrency.entries`), ask whether it
  comes with it, as the ruble and hryvnia came with Russian and Ukrainian, and the won with Korean. See step 3.

Write the decisions in `docs/product/general.md`, next to the Spanish and French ones.

## 2. Write the translation rules once

Create a glossary and style file in the scratchpad, as `fr-glossary.md` was: the tone, the typography of the language
(French's no-break spaces before `: ; ! ?`, its apostrophe), the placeholders to keep (`%s` `%d` `%1$s` `%lld` `%@`
`{name}`, the private-use stand-ins of the Apple activity sentences), the plural categories, the product's words
(group, bill, balance, settle up, member, recovery code…) in the language, and the activity sentence rule of step 4.
Every translator, human or agent, reads it first; without it each one picks its own words.

## 3. The code

- `core/model` `AppLanguage`: the new entry (its BCP-47 tag), the `fromTag` comment, and a test in `AppLanguageTest`
  for its tags and its chip. `ProfileUiStateTest` lists every language the picker offers.
- The picker names each language in its own language first ("Français", "日本語"), the same in every language of the
  app, with its name in the app's language as a smaller line under it ("French", "Francês"); a region is spelled out,
  never a code ("Português (Brasil)", "Portuguese (Brazil)"). So a new language adds its own name once and its
  translated name **to every existing language**, and the existing languages' names to its own. Own name: Android's
  non-translatable `profile_language_native_<name>` in `android/feature/profile`'s `values/strings.xml`, read by
  `languageName` in `LanguageSheet.kt`; Apple's `nativeName` in `LanguageName` of `ProfileView.swift`; the web's
  `nativeLanguageNames` map of `ProfileScreen.tsx`. Translated name: Android's `profile_language_<name>` in every
  `values-xx`, read by `translatedLanguageName`; Apple's English literal ("French") in `translatedName` and its entry in
  `Localizable.xcstrings`; the web's `profile_language<Name>` message in every dictionary and the `languageNames` map.
  Decide with Pierre whether the language carries a region.
- Web: `webApp/src/i18n/index.ts` (`Locale`, `messages`, `pickLocale`, `documentLanguages`) and the `isSupportedTag`
  pattern of `webApp/src/lib/currency.ts`, which keeps the currency names in the UI's language.
- Android needs nothing else: `generateLocaleConfig` builds the per-app language list from the `values-xx` folders.
  Apple needs nothing else either: the languages are the ones `Localizable.xcstrings` has.
- The tests that list every language: `AppLanguageTest` (its tags, the chips of every entry),
  `ObserveSpokenLanguageUseCaseTest` (the picks, the devices, the regional variants) and `ProfileUiStateTest` (both
  names, every option list). `./gradlew jvmTest` fails on the `when`s until each has it.
- A language written without spaces between words or phrases (Japanese, Chinese): add it to `unspacedLanguages`
  (`WordSpacing.kt`), `WordGap.swift` and the web's `wordGap`, `fullStop` and `clauseBreak` in `i18n/index.ts`. A
  language that parts its phrases with a space but has no full stop or comma (Thai) keeps the word gap and changes only
  `fullStopUi`, `WordGap.fullStop` and the web's `fullStop` and `clauseBreak`.
- A currency that comes with the language (Pierre's call, step 1): its `GroupCurrency` (code, regions, `fractionDigits`
  when it has none, as the yen, rupiah and won) after the last one in `entries`, a `GroupCurrencyTest` for its region,
  `ExpenseRules.maxAmountMinorFor` when 100 million of it is too little (the rupiah), and its paragraph in
  `general.md` next to the ruble's, saying what an older app version shows (a currency with no decimals shows 100
  times smaller there). The UIs name it and write its symbol from the platform, so nothing else changes in them.
- The store screenshots ([docs/store-listing-screenshots.md](../../../docs/store-listing-screenshots.md)): the
  language's sample trip in `SampleTripCopies.kt` and `SampleTrips.kt` (`currencies`, and a round rate in `priceOf` for a new
  currency), its words in `scripts/store-screenshots/listing-art/copy.tsv` and `creative-assets/copy.tsv`, its `StoreLocale`, its entry in `androidQualifiers` of
  `wearApp/src/test/kotlin/com/quare/blitzsplit/wear/WearStoreScreenshots.kt` when the language's Android resource
  qualifiers aren't its tag's language code (the Wear OS watch's screenshots are drawn from the row of `locales.tsv` below), its
  listing locales in `scripts/store-screenshots/locales.tsv` (the stores' own codes are `PLAY_LOCALES` and
  `APP_STORE_LOCALES` of `scripts/release/store-listing.mjs`), and the count in the `Fastfile`'s description and the docs that say "in all
  N languages". The stores only get the new language's screenshots from a stable release dispatched with
  `screenshots=true`, which is off by default
  ([release-process.md](../../../docs/release-process.md#screenshots-with-a-release)), or, for Google Play alone,
  from the `play-store-screenshots` workflow
  ([release-process.md](../../../docs/release-process.md#google-plays-screenshots-alone)): say so when you hand the
  task back. Until then its listing shows the default language's. That input alone captures the iPhone and the
  iPad on the App Store: the iPhone Duo's and the Mac's are made only with `screenshots_iphone_duo=true` and
  `screenshots_mac=true`, whose captures fail on the runners today, or by hand on a Mac.

## 4. The activity sentences

An activity row is one whole sentence per kind with the actor and the object in it, placed where the language's word
order puts them: Android's `activity_kind_*` with `%1$s` and `%2$s`, Apple's `SecondPerson` stand-ins, the web's
`activity_*` messages with `{who}` and `{subject}` ("{who} hat {subject} aufgeteilt"). Write the web's as Android's, and
start each with the actor. A language that conjugates by person needs the user's rows turned to the second person, on
the words right after the actor (the verb phrase): `toSecondPerson` (Android `SecondPersonVerbs.kt`), `SecondPerson`
(Apple) and `verbForYou` (web `i18n/index.ts`) do it the same way on the three platforms. Spanish rewrites the verb's
ending. French, German and Italian require every verb phrase to start with their perfect's auxiliary and swap it
("a" → "as", "hat"/"ist" → "hast"/"bist", "ha" → "hai"): a language like them only adds its entry to the auxiliary maps
(`secondPersonAuxiliaries` on Android and the web, `auxiliaries` on Apple). Polish, whose past says the gender the app
doesn't know, starts every verb phrase with a past in "ł(a)" and turns it into "łeś(-aś)". Pick the simplest rule the
language allows, put it in the glossary, add it to the three helpers and describe it in
[activity-and-push.md](../../../docs/product/activity-and-push.md). A language whose verb does not change with the
person needs none of this, unless something after the actor depends on who it is.

Korean is that case: its particles follow the last sound of the name, so someone else is "Ana 님이" (님 always takes
이) and every Korean verb phrase starts with "님이", which the user's rows drop after the user's subject word "내가"
(the rule is in the three helpers). A language like it needs the subject word apart from the standalone "You": the
subject keys are Android's `activity_you` and `bill_detail_history_you`, the web's `activity_you` and Apple's "You,
who did it"; every other "You" (a list, a chip, a payment's "You → Ana", Android's `activity_person_you`) is the
standalone one. Tell the translators which is which in the glossary.

A language that conjugates also says the deleted-item notice with who deleted it first ("Tu as supprimé…"): add it to
`youSubjectLanguages` in `SecondPersonVerbs.kt`, so Android uses "You" and the verb rule instead of "by you". So does a
language that would decline the names after "by" (Russian, Ukrainian: "Ты удалил(а): …"); its Android notice must then
start with the deleter's `%2$s`. Apple's notice always starts with the actor, and the web has its own
`activity_deletedByMe` message: three places to check for each new language.

## 5. The strings

Translate from the English, never from another translation:

- **Android**: `res/values-xx/strings.xml` (Android's own code for the few languages it still names by a legacy one:
  `values-in` for Indonesian, `values-iw` for Hebrew; add each legacy code's mapping to `AppLanguage.fromTag` and the web's `pickLocale`, as Indonesian's "in" is) next to each `res/values/strings.xml` (about 28 modules, `androidApp` and
  `android/designsystem` included), same keys, same order, every plural category the language needs.
- **Apple**: an `xx` localization for every key of `iosApp/Resources/Localizable.xcstrings` and
  `InfoPlist.xcstrings`, with the same plural and device variations as `es` (only the plural categories the language
  has). Edit the JSON with a script that keeps Xcode's formatting, so the diff shows only the added entries: Python's
  `json.dumps(d, indent=2, separators=(',', ' : '), ensure_ascii=False) + '\n'` writes the file back byte for byte.
  The keys of `strings` are in insertion order, not sorted (a new key goes next to its siblings, as "Korean" after
  "Ukrainian"); the languages inside a key's `localizations` are sorted. Hand the translators slices of the keys
  (each key's English and a translated language's shape) and have each write its translations to a file of its own,
  then merge them in one script: two agents editing the 4 MB catalog at once overwrite each other.
- **Web**: `webApp/src/i18n/xx.ts`, typed by `MessageKey`, exporting `xx`.

This is a lot of text (about 2,400 Android strings, 1,950 Apple keys and 2,300 web lines), so hand it to subagents with
the glossary, one per independent file set, in parallel: the Android modules in three groups (each module's
`values-xx` is its own file), the Apple keys in two slices and the web dictionary in two halves (each writing a file of
its own in the scratchpad, merged by you), and the release notes with the store listing. Tell each not to build or
commit. Read what comes back: have it list the strings it was unsure about and the words it chose that the glossary
lacks, and make those agree across platforms before the checks.

## 6. Make the checks know it

- `iosApp/check-translations.py`: add the language to `REQUIRED`, so a key without it fails the build, and to
  `PLURAL_CATEGORIES` when its plural has more forms than one and other (Polish's few and many), so a plural
  variation without one of them fails too.
- Android's `lintDebug` already fails on a missing or extra `values-xx` string, and `npm --prefix webApp run
  check:messages` on a web key or `{placeholder}` that differs from English: they need no change.
- The web picks `…One` for 1 and `…Other` for any other count. A language with two plurals past one (Polish) writes
  an `…Other` message with a `{count}` as "few|many", and `pluralFormOf` in `i18n/index.ts` picks one with
  `Intl.PluralRules` (many takes the second form, any other category the first); `check:messages` rejects a "|"
  anywhere else. A language with more forms than that needs `pluralFormOf` extended first.
- The release notes: add the language to `LANGS` and its sentence to `NOTHING_TO_SAY` in
  `scripts/release/release-notes.mjs`, and its text to every fragment in `release-notes/unreleased/`
  (`node scripts/release/release-notes.mjs check` fails until each has it). This goes for a regional
  variant too when the site writes its release notes in full, as it does Portugal's Portuguese: the
  fragments then carry the whole note, not only the words that differ. The site gains the language in a
  pull request of its own and either may merge first: see
  [docs/release-notes.md](../../../docs/release-notes.md#adding-a-language).
- The stores' listing: write `store-listing/<language>.md` and add the language's store locales to
  `PLAY_LOCALES` and `APP_STORE_LOCALES` in `scripts/release/store-listing.mjs`
  (`node scripts/release/store-listing.mjs check` fails until the file is there). See
  [docs/store-listing.md](../../../docs/store-listing.md#adding-a-language).
- The docs that list the languages: `CLAUDE.md`, `README.md`, `CONTRIBUTING.md` (step 2 of "Making a change"),
  `ACCESSIBILITY.md` (the "Language" commitment), `docs/ai_agents.md`, `docs/architecture/code-style.md`,
  `docs/architecture/new-feature-checklist.md`, `docs/product/general.md`, `docs/product/profile.md`,
  `.claude/skills/review-conventions/SKILL.md`, the comments of `androidApp/build.gradle.kts` and `iosApp/project.yml`.
  `grep -rn "Spanish"` finds the places the last language reached.

## 7. The other repositories

A language isn't done until the site and the API speak it too: open a pull request in each, as part of this task,
once the client's is open (Pierre's call). Fetch each repository first and branch from its `origin/main`, since
their checkouts sit on stale branches. Link the three pull requests to each other in their descriptions. Either may
merge first.

- `blitz-split-site`: the language's release notes folder, which a release needs before it writes the language
  ([docs/release-notes.md](../../../docs/release-notes.md#adding-a-language)), and its legal pages
  (`public/legal/<page>.<language>.md`, translated from English), which the apps read in English until then
  ([profile.md](../../../docs/product/profile.md)). Plus whatever else the site lists per language: follow its own
  docs.
- `blitz-split-api`: the language of the invite and bill link previews (`invite_preview.ts` and the preview texts),
  picked by a currency of the language, as `JPY` gives Japanese
  ([joining-and-folds.md](../../../docs/product/joining-and-folds.md)); and a new currency's minor digits and cap
  (`supabase/functions/_shared/app_currencies.ts`, when they differ from ISO's or the usual cap). Update the
  client's `joining-and-folds.md` with the currency's language in the client's pull request.

## 8. Verify

Through `./scripts/heavy-check.sh`, one build at a time: `./gradlew jvmTest`, `./gradlew lintDebug`,
`./scripts/ktlint.sh`, `npm --prefix webApp run check:messages` and `npm --prefix webApp run build`,
`python3 iosApp/check-translations.py iosApp/Resources`, then the Apple build (it runs the check too). Then look at the
language in each app: Profile › Language picks it, a long sentence does not break a layout (German, Finnish), and an
Activity row of the user's reads right in the second person.

Open the pull request with `create-pr`. The push notifications need no extra step: each device words them in its own
language from a kind and the names, with the `push.*` keys of the same string files.
