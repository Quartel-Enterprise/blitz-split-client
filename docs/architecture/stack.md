## Stack

Versions live in `gradle/libs.versions.toml`, `webApp/package.json` and the `packages:` of `iosApp/project.yml`; this
is what each piece is for. The `bump-versions` skill updates them all, with the rules on pre-releases and on the
versions that move together.

### Shared (Kotlin Multiplatform)

- **Kotlin 2.4.21** targeting `android`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `js` (browser, ES modules)
  and `jvm` (unit tests and coverage only), plus `watchosArm64` and `watchosSimulatorArm64` for the Apple Watch app
  (see [watchOS](#watchos) below)
- **JetBrains multiplatform lifecycle** (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`) for the shared
  `ViewModel`s
- **kotlinx.coroutines** and `StateFlow` / `SharedFlow` for state, exposed to Swift and JS through a hand-written
  bridge (`FlowWatcher`, `Watch`, `ViewModelHolder`) — no SKIE, see
  [state-management.md](state-management.md#why-not-skie)
- **Koin** (`koin-core`) for dependency injection; ViewModels are bound with `factoryOf`, not `koin-core-viewmodel`'s
  `viewModelOf`, which has no watchOS target (see [dependency-injection.md](dependency-injection.md))
- **Room 3** (`androidx.room3`, KMP) as the local source of truth on every platform, with the `androidx.sqlite`
  drivers: framework on Android, bundled on Apple and jvm, Web Worker on the web
- **supabase-kt** (Auth, Postgrest, Functions, Storage) over Ktor, for the backend in the `blitz-split-api` repository
- **kotlinx.serialization** for the DTOs, **BuildKonfig** for the Supabase settings
- **Quare connectivity** (`software.quare:connectivity`) for `NetworkConnectivityObserver`

### watchOS

The Apple Watch app ([issue 419](https://github.com/Quartel-Enterprise/blitz-split-client/issues/419)) runs the same shared code as the phone: its own Room database, Supabase session and
sync. Every `:core:*` and `:feature:*` module and `:shared` apply `blitzsplit.kotlin.multiplatform.watchos` on top of
`blitzsplit.kotlin.multiplatform`, which adds two targets:

- `watchosArm64`: arm64_32, the device slice every Apple Watch from Series 4 on runs, Series 9 and Ultra 2 included.
- `watchosSimulatorArm64`: the simulator on Apple silicon.

The default hierarchy puts both under `appleMain`, so the Apple code is shared with iOS and macOS; only what
`iosMain` and `macosMain` set apart needs a `watchosMain` actual (`core/profile`'s `ClientPlatform`, which says `IOS`
until the server knows a watch, and `core/in_app_update`, which has no update prompt on the watch).

`:shared` links a static `Shared` framework for both, apart from the iOS and macOS ones, whose API doesn't change:

```bash
./scripts/heavy-check.sh ./gradlew :shared:assembleSharedDebugXCFramework   # shared/build/XCFrameworks/debug/Shared.xcframework
```

`assembleSharedReleaseXCFramework` builds the release one. The Apple Watch app (`BoraRachar-watchOS` in
`iosApp/project.yml`, embedded in the iPhone app by `iosApp/watch-embed.yml`, which a project generated on a Mac leaves
out: [getting-started.md](../getting-started.md#build-and-run-the-apple-app)) calls
`embedAndSignAppleFrameworkForXcode` like the iOS app does, and
builds for `arm64_32` only on the device (`ARCHS[sdk=watchos*]`). Its Swift calls `BlitzSplitApple` to start Koin and
`BlitzSplitWatch` (in `shared/src/watchosMain`, so only the watch's framework has it) for its screens, its back stack
and what the iPhone and its pushes ask of it. On a pull request, CI's `apple-app (iOS)` builds the watch app with the
iPhone app that embeds it, for the simulator, and `apple-app (macOS)` compiles the device slice; `main`'s `apple-tests`
builds the debug XCFramework ([ci.md](../ci.md#speed-and-timeouts)).

Left out, and why:

- `watchosDeviceArm64`, the 64-bit slice of Series 9 and later: a Kotlin/Native **tier 3** target (built, not tested
  by Kotlin's CI), and `lifecycle-viewmodel` 2.11.0 and supabase-kt 3.9.0 don't publish it. The arm64_32 slice runs
  on those watches meanwhile; add the target to the plugin once both libraries publish it, knowing the watch tests
  still run on the simulator only. Running is not shipping: before the first watch release, check whether App Store
  Connect accepts a watch app without the arm64 slice. If it doesn't, the watch app waits for those libraries.
- `watchosArm32` (Series 3, stuck on watchOS 8) and `watchosX64` (Intel simulators): deprecated in Kotlin.

A library the shared code takes needs a watchOS target too: `software.quare:loadable` from 1.2.0 and
`software.quare:connectivity` from 1.3.0 publish one.

### Android

- **Jetpack Compose** from the AndroidX BOM, with **Material 3 Expressive** (`material3` 1.5:
  `MaterialExpressiveTheme`, `MotionScheme.expressive()`) and `androidx.graphics:graphics-shapes`
- **Navigation 3** (`NavDisplay`) drawing the shared back stack
- **Credential Manager** + `googleid` for the native Google sign in, Play services code scanner for the QR
- **Coil 3** (`coil-compose`, `coil-network-okhttp`) for the avatar photo, a URL or the data URI of a picked one

### Apple (iOS, iPadOS, macOS 26)

- **SwiftUI** with Liquid Glass (`glassEffect`, `.tabViewStyle(.sidebarAdaptable)`), Swift 6 with `MainActor`
  default isolation
- **XcodeGen** (`iosApp/project.yml`) generating one project with the `BoraRachar-iOS` and `BoraRachar-macOS`
  targets
- The static framework `Shared` built by `./gradlew :shared:embedAndSignAppleFrameworkForXcode`
- **AuthenticationServices** for Sign in with Apple

### Web

- **Vite**, **React 19**, **TypeScript**, **Tailwind CSS 4**, **shadcn/ui** (Radix, `lucide-react`, `sonner`,
  `vaul`)
- The JS library `blitzsplit-shared` (`shared/build/dist/js/productionLibrary`, with TypeScript definitions; `npm run
  dev:fast` serves its development build instead)
- **SQLite WASM** (`@sqlite.org/sqlite-wasm`) in a Web Worker, storing the database in OPFS

### Tooling

- **ktlint** with the Quare ruleset (`scripts/ktlint.sh`), see [code-style.md](code-style.md)
- **jscpd** for copied code in Kotlin, Swift and TypeScript (`scripts/cpd.sh`), see
  [code-style.md](code-style.md#copied-code)
- **Kover** (80% line coverage) and **modules-graph-assert**, configured in the root `build.gradle.kts`
