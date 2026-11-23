# Contributing to Blitz Split

Thanks for wanting to help with Blitz Split (**Bora Rachar** in Portuguese)! Bug reports, ideas, translations and code
are all welcome.

By contributing, you agree to follow the [code of conduct](CODE_OF_CONDUCT.md) and that your contribution is used
under the terms of the [license](LICENSE): the source is public to read and to contribute to, but it is not open
source.

## Reporting a bug or asking for something

- **A bug**: open an issue with the **Bug report** template. Say which platform (Android, iPhone, iPad, Mac or the
  web) and which version, what you did, what you expected and what happened instead. Screenshots help a lot.
- **An idea**: open an issue with the **Feature request** template, starting from the problem it solves.
- **A security problem**: never in a public issue. Follow the [security policy](SECURITY.md).

Search the existing issues first: someone may have reported it already.

## Before writing code

Product choices that nobody wrote down yet are the maintainer's call, so for anything bigger than a small fix, open
an issue first and agree on the behavior before writing it. How each screen behaves on every platform is written in
the [product docs](docs/product/general.md), one doc per area of the app; read your area's before changing a screen,
and update it in the same pull request.

## How the code is organized

Kotlin Multiplatform is shared **up to the ViewModel**: every `ViewModel`, `UiState`, `UiEvent` and `UiAction` lives
in `:feature:*`, and three native UIs draw them: Jetpack Compose (`android/*`), SwiftUI (`iosApp/`) and React
(`webApp/`). Logic goes in the ViewModel or below it, never in a UI, so the three platforms walk the same flow. A change
to a screen usually touches all three UIs.

Start with:

- [Getting started](docs/getting-started.md): the JDK, Android SDK, Xcode, XcodeGen and Node setup, and how to run each
  platform.
- [The architecture guide](docs/ai_agents.md): state, use cases, dependency injection, navigation, data sources and
  sync, code style.
- [Testing](docs/testing/README.md): what gets unit tested and how.

## Making a change

1. Fork the repository and branch from `main`. Name the branch `<type>/<short-description>`, where the type is one of:

   | Type | For |
   | --- | --- |
   | `fix` | A bug or unintended behavior |
   | `feature` | Something the app couldn't do |
   | `enhancement` | Something it already did, now better (performance, UX, accessibility, another screen size) |
   | `refactor` | Restructuring with no change people notice |
   | `chore` | Build, CI, tooling or docs only |

2. Write code, comments, docs and commit messages in **English**. Only the text a person sees in the app is
   translated: add every new string in English and in each translated language (Portuguese of Brazil and Portugal,
   Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian, Russian, Ukrainian, Korean, Hindi, Arabic, Chinese (Simplified), Hebrew, Thai, Vietnamese, Chinese (Traditional), Swedish, Czech, Romanian, Bengali, Malay, Greek, Croatian, Hungarian, Urdu, Filipino, Serbian, Slovak, Persian, Tamil and Swahili), in each
   platform's own place for them. Never send UI copy from Kotlin.
3. Add or update unit tests for the logic you change in `core/*` and `feature/*`.
4. If people will notice the change, add a release notes fragment, as [Release notes](docs/release-notes.md) explains.
5. Run the checks:

   ```bash
   ./scripts/ktlint.sh --format
   ./scripts/cpd.sh
   ./gradlew build jvmTest :koverVerifyCi assertModuleGraph
   npm --prefix webApp run check:messages
   npm --prefix webApp run build
   node scripts/release/release-notes.mjs check
   node scripts/release/store-listing.mjs check
   ```

   and, if you touched the Apple app, build the `BoraRachar-iOS` and `BoraRachar-macOS` schemes after
   `./iosApp/generate-project.sh`.

## Opening the pull request

- Title it with the [Conventional Commits](https://www.conventionalcommits.org) type of the branch, in English:
  `fix: keep the split when the currency changes`.
- Fill in the pull request template: the problem, the expected behavior, the edge cases (offline, without an account,
  another platform), and which platforms you checked and how. A change to a shared ViewModel changes Android, Apple
  and the web at once, so it is checked on all three.
- Add screenshots for any visual change, on each platform it reaches, in light and dark.
- Keep it focused: one change per pull request is far easier to review.

The CI runs these checks, plus Android lint (which fails on a missing Android translation) and the tests on the JS
and Apple targets, and a maintainer reviews every pull request before it merges.
