# Release notes

What each version changed, told to the person using the app, is written once, by the pull request that makes the
change, and published by the release:

1. A pull request with a change people notice adds a **fragment**, `release-notes/unreleased/<description>.json`: its
   notes in every language. `create-pr` writes it.
2. A release picks the fragments its version tells, takes its version number from them, and publishes the notes: to
   [blitz-split-site](https://github.com/Quartel-Enterprise/blitz-split-site), whose "What's new" page and feed the
   apps' own "What's new" screen read, to the testers with the build, to the GitHub release and, for a stable version,
   to the stores.

[`scripts/release/release-notes.mjs`](../scripts/release/release-notes.mjs) does the reading, plain Node with no
dependencies, and [release-process.md](release-process.md) says how a release runs.

## A fragment

`release-notes/unreleased/export-group-data.json`:

```json
{
  "notes": [
    {
      "kind": "new",
      "platforms": ["all"],
      "text": {
        "en": "You can now export a group's data from its menu.",
        "pt": "Agora você pode exportar os dados de um grupo pelo menu dele.",
        "pt-PT": "Agora podes exportar os dados de um grupo a partir do menu dele.",
        "es": "…", "fr": "…", "de": "…", "it": "…", "pl": "…", "nl": "…", "ja": "…", "tr": "…", "id": "…",
        "ru": "…", "uk": "…", "ko": "…", "hi": "…", "ar": "…", "zh": "…", "he": "…", "th": "…", "vi": "…", "zh-Hant": "…",
        "sv": "…", "cs": "…", "ro": "…", "bn": "…", "ms": "…", "el": "…", "hr": "…", "hu": "…", "ur": "…", "fil": "…", "sr": "…", "sk": "…", "fa": "…", "ta": "…", "nb": "…", "sw": "…"
      }
    }
  ]
}
```

- **The file's name** is the branch's description (`feature/export-group-data` writes `export-group-data.json`), in
  lowercase letters, digits and hyphens. One file per pull request, so two pull requests never edit the same file.
- **`notes`**: one per change the person notices, at least one.
- **`kind`**: `new` (something the app couldn't do), `improved` (something it already did, now better) or `fixed`
  (something that was wrong). A version lists its notes in that order, and its number comes from them
  ([below](#the-version-a-release-gets)).
- **`platforms`**: `["all"]`, or some of `android`, `ios` (iPhone and iPad), `mac` and `web`, in that order; never all
  four, which is `["all"]`. The platforms whose UI the change reaches: a note for a change in the shared ViewModel is
  for all, one for a SwiftUI-only change is `["ios", "mac"]` or one of them.
- **`text`**: the note in each of the thirty-eight languages the fragments have (`en`, `pt`, `pt-PT`, `es`, `fr`, `de`,
  `it`, `pl`, `nl`, `ja`, `tr`, `id`, `ru`, `uk`, `ko`, `hi`, `ar`, `zh`, `he`, `th`, `vi`, `zh-Hant`, `sv`, `cs`, `ro`, `bn`, `ms`, `el`, `hr`, `hu`, `ur`, `fil`, `sr`, `sk`, `fa`, `ta`, `nb`, `sw`), one plain line each, with no Markdown and no HTML: the apps and the page draw it as
  it is. Portugal's Portuguese is written in full here, as on the site, not as the few words over Brazil's that the
  apps keep for it: nearly every note differs ("tu" for "você", "ecrã" for "tela", "estar a" for the gerund).
- **`betaOnly`**, optional: `true` for a note about something no stable version ever had, such as the fix of a bug
  another pull request introduced since the last stable version. The beta testers read it, and the stable version
  leaves it out, since nobody on a stable version saw the bug.

`node scripts/release/release-notes.mjs check` fails on a fragment a release couldn't read, as the `release-notes`
workflow does on the pull request.

### Which changes get one

A fragment is for what the person using the app would notice. A new feature, a screen that works better, a bug that
is gone, a new language. Nothing for a refactor, a test, a build or CI change, a doc, a skill, a dependency bump with
no visible effect, or code no screen reaches yet: a pull request with none of the first kind adds no fragment.

### How a note is written

- From the person's side: what they can do now, or what stopped going wrong. "You can now…", "Fixed a problem
  that…". Never what the code does: no class, no library, no "refactored".
- One sentence, with no jargon. Screens and buttons are named as the app shows them in that language (Profile ›
  Privacy and about is Perfil › Privacidade e sobre in Portuguese), as the site's legal pages do: take the words from
  the app's strings, not from a translation of the English note. The app is Bora Rachar in Brazil's Portuguese and
  Blitz Split in every other language, Portugal's included. A word Portugal's strings don't override
  (`values-pt-rPT`, the `pt-PT` localization, `ptPT.ts`) is Brazil's.
- No "On Android," at the start: `platforms` says where, and the page writes it. A platform's name stays only when it
  narrows the note further ("On iPhone in landscape,").
- An operating system's version is a floor ("on iOS 26 or later"), never one version: the notes stay readable for
  years.

## The version a release gets

A release takes its number from the fragments that came since the last stable version, leaving the `betaOnly` notes
out:

| The notes since the last stable version | The version |
| --- | --- |
| Only `fixed`, or none | A patch: 3.0.0 leads to 3.0.1 |
| At least one `new` or `improved` | A minor: 3.0.0 leads to 3.1.0 |

A major version is a product decision no note can make: set `versionName` in `version.properties` by hand, and the
release takes the higher of the file's and the notes'. The version typed when dispatching a release overrides both;
the merge-back writes the version a release used into the file when it is higher than the file's, so a version typed
to raise it holds for the betas after it too. Type one only to raise it: a beta of a version lower than one already
released would tell that release's notes again. Until the first stable version is tagged there is no version to
count from, and the file's `versionName` is the version.

The fragments pile up until a stable version, so the number only grows: a first beta with only fixes is
`3.0.1-beta_1`, and after a pull request with a `new` note the next one is `3.1.0-beta_1`. 3.0.1 then never comes out
as a stable version, and the site's page shows its beta under 3.0.1.

## Which notes a version tells

Read from the commit the release builds, never from `main`, which may have moved:

- **A beta** tells the notes the release before it didn't have: the ones that aren't in the commit of the highest
  version tagged below this one. A note is told by one beta, even though its file stays.
- **A stable version** tells every note the stable version before it didn't have, without the `betaOnly` ones:
  everything its betas told that still matters.
- **A note is known by its file and its English text.** So a pull request may add a note to a fragment a release
  already read, and only the new note is told. Renaming a fragment a release already read, or rewording the English
  of one of its notes, tells that note again in the next beta: leave a told fragment as it is, unless the note was
  wrong enough to be worth telling twice.
- **A version the site already has** keeps the notes written there. That is how a version's notes are written by
  hand, as 3.0.0's are, the version that replaces the legacy app: write `release-notes/<language>/<version>.json` in
  the site before releasing. The release leaves those files as they are, and `collect --site` reads them instead
  of the fragments for what it writes with `--out`, which is how the stores get the same text. `release-beta` picks
  its testers' text and its GitHub release without the site's checkout, so those two still tell the fragments' notes.

The fragments stay in `release-notes/unreleased/` through the betas and leave `main` in the merge-back of a stable
version ([`merge-back.sh`](../scripts/release/merge-back.sh)), which removes the ones its tag's commit had
(`git ls-tree --name-only <commit> release-notes/unreleased/`), not the ones merged since. A fragment a later pull
request added a note to stays, since removing it would lose that note: its other notes were told, and are not told
again. Which notes are told never depends on that cleaning: a note left behind is one the release before already
had.

## Where the notes go

| Where | What | When |
| --- | --- | --- |
| blitz-split-site | `release-notes/<language>/<version>.json` in its format (its `docs/release-notes.md`), through a pull request that `send-release-notes-to-site.sh` opens and merges. The site deploys on the merge | Every release with a note |
| Firebase App Distribution | The version and build number, then the notes of that platform in Portuguese (`TESTERS_LANG`), since it takes one text and the testers read Portuguese. Counted from the last beta that sent that platform's testers a build, not from the last beta (Pierre's call): after an iOS only beta, the Android testers read its notes with their next build. Which beta sent what is read from the `Sent to testers:` line `finalize` writes in each GitHub release (a release from before that line counts as having sent nothing) | Every beta |
| The GitHub release | "What's new", in English, each note naming its platforms unless it is for all | Every release |
| Google Play and the App Store | `stores/play/<language>.txt` (the notes for all and for `android`, as many as fit the 500 characters Play takes, the last ones left out first), `stores/app-store-ios/` and `stores/app-store-mac/` (4000 characters), written by `collect --out`. A store with no note for its platform gets "Bug fixes and performance improvements." in its language | A stable version |

The store files are named by the notes' language. `release-production` collects them once, with the site's
checkout, and the lanes that upload them (`play_upload` and `app_store_submit` in `fastlane/Fastfile`) ask each
store which locales its listing has and give each one its language's file: the file of its own name (`pt-PT`),
else its language's (`pt-BR` reads `pt`, `es-419`, `es-MX` and `es-ES` read `es`, `en-GB` reads `en`), else
English's. So no map of store locale codes is kept for the notes, and a language added to a listing gets its notes
with the next release ([release-process.md](release-process.md#what-the-stores-and-the-web-get)).

## Adding a language

The languages are `LANGS` in `release-notes.mjs`, and they are the site's: a language goes in both, with its "Bug
fixes and performance improvements." in `NOTHING_TO_SAY`, and every fragment in `release-notes/unreleased/` gets its
text, or the check fails. The `add-language` skill lists it.

The two repositories gain a language in pull requests of their own, and either may merge first. So a release writes
the languages the site's `release-notes/` has folders for, not `LANGS`: a language only the fragments have waits for
the site, whose earlier versions it then adds itself, and one only the site has stops `site-release-notes`, naming
it, since the site would refuse a version without it. A hand-written version read from a site that lacks a language
tells that language its base's notes (Portugal's reads Brazil's) or English's.
