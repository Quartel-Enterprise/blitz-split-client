## Stack

Versions live in `gradle/libs.versions.toml` and `webApp/package.json`; this is what each piece is for.

### Shared (Kotlin Multiplatform)

- **Kotlin 2.4.21** targeting `android`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `js` (browser, ES modules)
  and `jvm` (unit tests and coverage only)
- **JetBrains multiplatform lifecycle** (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`) for the shared
  `ViewModel`s
- **kotlinx.coroutines** and `StateFlow` / `SharedFlow` for state, exposed to Swift and JS through a hand-written
  bridge (`FlowWatcher`, `Watch`, `ViewModelHolder`) — no SKIE, see
  [state-management.md](state-management.md#why-not-skie)
- **Koin** for dependency injection
- **Room 3** (`androidx.room3`, KMP) as the local source of truth on every platform, with the `androidx.sqlite`
  drivers: framework on Android, bundled on Apple and jvm, Web Worker on the web
- **supabase-kt** (Auth, Postgrest, Functions, Storage) over Ktor, for the backend in the `blitz-split-api` repository
- **kotlinx.serialization** for the DTOs, **BuildKonfig** for the Supabase settings
- **Quare connectivity** (`software.quare:connectivity`) for `NetworkConnectivityObserver`

### Android

- **Jetpack Compose** from the AndroidX BOM, with **Material 3 Expressive** (`material3` 1.5 pre-release:
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
- The JS library `blitzsplit-shared` (`shared/build/dist/js/productionLibrary`, with TypeScript definitions)
- **SQLite WASM** (`@sqlite.org/sqlite-wasm`) in a Web Worker, storing the database in OPFS

### Tooling

- **ktlint** with the Quare ruleset (`scripts/ktlint.sh`), see [code-style.md](code-style.md)
- **jscpd** for copied code in Kotlin, Swift and TypeScript (`scripts/cpd.sh`), see
  [code-style.md](code-style.md#copied-code)
- **Kover** (80% line coverage) and **modules-graph-assert**, configured in the root `build.gradle.kts`
