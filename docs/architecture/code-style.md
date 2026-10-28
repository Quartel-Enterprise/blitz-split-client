# Code Style

## Running ktlint

Kotlin style is checked by ktlint with the custom rules of
[`Quartel-Enterprise/ktlint-rules`](https://github.com/Quartel-Enterprise/ktlint-rules), shared with the other Quare
projects:

```bash
./scripts/ktlint.sh            # check
./scripts/ktlint.sh --format   # autocorrect what can be autocorrected
```

The script is a wrapper that never changes here. It reads `quare-ktlint-rules` in `gradle/libs.versions.toml` and
downloads that release's runner, ruleset jar and ktlint CLI once into `~/.cache/ktlint`; there is no Gradle ktlint
task. Project settings live in `.editorconfig` (rules turned off for some paths, the test-only rules turned on for
the test source sets). A new or changed rule goes to ktlint-rules first; to try one before it is released, build its
jar there and run `KTLINT_RULESET_JAR=<path to the jar> ./scripts/ktlint.sh`.

Everything below marked with a `quare:` rule is enforced; the rest is review.

## Kotlin

### Return types and expression bodies

Every function declares its return type, except `Unit`, which is omitted. A `Unit` function always has a block
body; a non-`Unit` function whose body is a single expression uses `=` (`quare:unit-function-block-body`).

```kotlin
// Correct
override fun observeAccount(): Flow<AccountSnapshot> = local
    .observe()
    .map(::toSnapshot)
    .distinctUntilChanged()

override suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt) {
    local.update { account -> account.copy(recoveryCodePrompt = prompt.code) }
}

// Wrong
override fun observeAccount() = local.observe().map(::toSnapshot)
override suspend fun updateRecoveryCodePrompt(prompt: RecoveryCodePrompt) = local.update { ... }
```

Exempt: `@Test` functions and `runTest { }` bodies (on JS `TestResult` is a promise the function must return), and
bodies that never return (`= error("unused")`).

### Function naming

A function that returns a value is a verb phrase whose verb says what kind of operation it is
(`quare:value-returning-function-naming`):

| Prefix | Use for | Example |
| --- | --- | --- |
| *(none: a `val`)* | no parameters and no real computation | `val areAccountButtonsEnabled: Boolean` |
| `get…` | cheap read, or the current value of something | `getCurrentUserOrNull()` |
| `fetch…` / `load…` | network or disk I/O | `fetchRecoveryCredentials(code)` |
| `observe…` | returns a `Flow` | `observeSessionUser()` |
| `create…` / `build…` | a new instance | `createBlitzSplitSupabaseClient()`, `createRecoveryCode()` |
| `find…` | a lookup that may miss | pair with `…OrNull` |
| `to…` / `as…` | a conversion | `toRestoreError()`, `toRecoveryCodeFromQr()` |
| `is…` / `has…` / `can…` | `Boolean` | |

Exempt: value-returning `@Composable`s (`rememberDeviceLayout()`), `Modifier` extensions, `override` / `actual`,
`operator` functions, names ending in `…OrNull` / `…Of` / `…For` (`fromCodeOrNull`, `deviceLayoutOf`), `@Dao` and
`@Database` members (`profileDao()`), and test sources.

### Imports

No fully qualified names in code, no wildcard imports (`standard:no-wildcard-imports`, not autocorrected) and no
unused imports (`standard:no-unused-imports`, enabled in `.editorconfig`; commented-out code doesn't count as a
use).

### File-scoped constants

A top-level `private val` is `lowerCamelCase`, sits right after the imports, and consecutive ones form one block
with no blank lines (`quare:private-top-level-val-naming`, `quare:top-level-val-position`,
`quare:top-level-val-blank-line`):

```kotlin
private val singleColumnMaxWidth = 560.dp
private val twoColumnsMaxWidth = 1100.dp
private val tabletActionsMaxWidth = 400.dp

@Composable
fun WelcomeContent(...) { ... }
```

File scope is for values with no owner: Compose sizes above the composables they size, Koin modules, fixtures. A
value only one class reads goes in that class (`quare:top-level-val-ownership`).

### Where a `private val` lives

A class's own values are `private val`s in its body (`private val json = Json { ignoreUnknownKeys = true }`,
`private val writeLock = Mutex()`), declared before the property that reads them. A companion object holds only
`const val`s and public API (`quare:companion-object-constants`):

```kotlin
class SupabaseAccountRemoteDataSource(...) : AccountRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val RECOVER_FUNCTION = "recover"
        const val RECOVERY_CODE_FUNCTION = "recovery-code"
        const val HTTP_CONFLICT = 409
    }
}
```

A `Duration` is never a companion member (`quare:companion-object-duration`) and never a hand-made product of
time units like `30L * 60 * 1000` (`quare:manual-time-unit-conversion`): write `30.seconds` and convert with
`inWholeMilliseconds` only where an API needs a number.

### When-entry bodies

A `when` entry with a single statement has no braces (`quare:when-entry-single-statement-braces`); one too long for
the line wraps onto the next line instead:

```kotlin
when (event) {
    is WelcomeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
    WelcomeUiEvent.OnUseWithoutAccountClicked -> navigator.navigate(Route.ProfileSetup)
    WelcomeUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
}
```

### Two branches are an `if`

A `when` with one branch besides `else` is an `if` (`quare:two-branch-when`):

```kotlin
return if (hasProfile) SignInOutcome.PROFILE_RESTORED else SignInOutcome.NEEDS_PROFILE
```

### `fun interface`

An interface with exactly one abstract method is a `fun interface` (`quare:fun-interface-required`), so a test can
pass a lambda: `CurrentTimeMillis`, `Watch`, every use case. Pass the lambda directly, without the redundant SAM
constructor (`quare:redundant-sam-constructor-argument`): `completeSignIn = { outcome }`.

### Method references

A lambda that only forwards its parameter becomes a reference (`quare:prefer-method-reference`, which only sees
functions declared in the same file): `.map(::toSnapshot)`, `?.let(::toSessionUser)`, `onEvent = viewModel::onEvent`.
A `suspend` or `@Composable` callee keeps the lambda.

### Explicit backing fields

Mutable state exposed read-only uses an explicit backing field, not a `_name` property
(`quare:explicit-backing-field`):

```kotlin
val uiAction: SharedFlow<GroupsUiAction>
    field = MutableSharedFlow<GroupsUiAction>()
```

### Interface and implementation in separate files

`ProfileMapper.kt` and `ProfileMapperImpl.kt`, never one file (`quare:interface-implementation-separate-files`).
A sealed hierarchy stays in one file.

### Constructor parameters

A constructor `private val` read only while building the instance (a property initializer, `init`) is a plain
parameter (`quare:redundant-private-constructor-property`), and properties come before plain parameters
(`quare:constructor-property-order`):

```kotlin
class RecoveryCodeViewModel(
    private val nativeSignIn: NativeSignIn,
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val completeSignIn: CompleteSignIn,
    observeAccount: ObserveAccount,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel()
```

Every parameter is read (`quare:unused-function-parameter`); signatures dictated from outside (`override`,
`actual`, interface members) are skipped.

### DTOs

A `@Serializable` `*Dto` mirrors the wire format: every field names its key with `@SerialName`, even when it equals
the property, and has no default; what the payload may leave out is nullable (`quare:dto-serial-name`).

```kotlin
@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_color") val avatarColor: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("updated_at") val updatedAt: Long,
)
```

### Data classes

No default values on any data class property: `UiState`, domain model or Room entity
(`quare:data-class-default-value`). Every top-level data class and enum lives in its own file named after it
(`quare:data-class-own-file`); the cases of a sealed interface stay with it.

### Composable naming

A `@Composable` that emits UI ends in a word from a closed list that says what kind of UI it is
(`quare:composable-naming-suffix`); the file carries the same name:

| Suffix | Use for |
| --- | --- |
| `Screen` | the stateful entry of a feature: gets the ViewModel, collects state, handles actions (`WelcomeScreen`) |
| `Content` | the stateless body (`WelcomeContent`, `RecoveryCodeContent`) |
| `Scaffold`, `Theme`, `Root`, `Display`, `Layout` | structural wrappers and roots (`BlitzSplitTheme`, `AppNavDisplay`) |
| `Dialog`, `Sheet`, `Overlay`, `Pane`, `Panel`, `Drawer`, `Sidebar`, `Rail`, `Bar`, `Footer`, `Header` | surfaces and chrome (`RecoveryCodeSheet`) |
| `Card`, `Box`, `Surface`, `Section`, `Hero`, `Banner`, `Bubble`, `Tile`, `Cell`, `Item`, `Row`, `Column`, `List`, `Grid`, `Spacer` | containers (`ConnectionAlertCard`, `ShapeClusterHero`, `OfflineNoticeBanner`) |
| `Button`, `Fab`, `Action`, `Toggle`, `Switch`, `Slider`, `Picker`, `Option`, `Menu`, `Chip`, `Pill`, `Field`, `Handle` | controls (`GoogleSignInButton`) |
| `Text`, `Title`, `Heading`, `Label`, `Message`, `Icon`, `Image`, `Badge`, `Line`, `Indicator`, `Spinner`, `Snackbar` | content (`AvatarBadge`, `EmptyGroupsImage`) |
| `Skeleton` | a loading placeholder |
| `Effect`, `Collector` | no UI, only a side effect (`RecoveryCodeActionsEffect`) |
| `Component` | anything none of the above describes |

Value-returning composables (`rememberDeviceLayout`) and `@Preview` functions are exempt. A suffix only this app
needs goes in `.editorconfig` as `ktlint_quare_composable_extra_suffixes`.

### Comments say why, never what

Production Kotlin explains *what* through names. A comment only keeps what a name can't carry, the *why*: a
workaround with its issue, an invariant the code relies on, a deliberate choice that looks wrong, a constraint
outside the repository (`quare:comments-say-why`). No KDoc, no section labels, no narration.

```kotlin
// Why: stdlib runCatching swallows CancellationException, which breaks structured concurrency.
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = ...

/*
 * Why: a browser sign in finishes when the OAuth callback comes back, after this ViewModel was
 * recreated on the web (the page reloads), so a Google or Apple session seen while this screen
 * is on top completes it. An ID token sign in completes on its own.
 */
private fun observeBrowserSignIn() { ... }
```

A one-line reason is a `// Why:`; a longer one is a block with `/*` and `*/` on their own lines and ` * ` before
each line. Comments are free in tests, the `:testing` modules, Gradle scripts and `build-logic` (sections of
`.editorconfig`).

### Catching exceptions in coroutines

Use `suspendRunCatching` instead of a `try` that rethrows `CancellationException` (`quare:suspend-run-catching`).
See [coroutine-error-handling.md](coroutine-error-handling.md).

### File length

A production Kotlin file has at most 600 lines (`quare:max-file-length`). Past that, move a responsibility out
(private composables to their own files, a mapper, a collaborator class) rather than compress the code.

### Tests

The test conventions of [docs/testing](../testing/README.md) are rules that only run on the test source sets:

| Rule | Enforces |
| --- | --- |
| `quare:test-name-given-when-then` | [`GIVEN … WHEN … THEN …` names](../testing/given-when-then.md#naming) |
| `quare:test-body-sections` | [`// Given`, `// When`, `// Then`](../testing/given-when-then.md), once each and in order |
| `quare:prepare-scenario-returns-unit` | [`prepareScenario`](../testing/prepare-scenario.md) returns nothing |
| `quare:prepare-scenario-last-member` | `prepareScenario` is the last member, before only the companion object |
| `quare:prepare-scenario-in-given` | tests call `prepareScenario` under `// Given` |

## Swift

No linter runs on Swift yet; follow what the app already does.

- Swift 6 with `SWIFT_DEFAULT_ACTOR_ISOLATION: MainActor`: types are main-actor by default. Delegate callbacks from
  system frameworks are `nonisolated` and hop back with `MainActor.assumeIsolated` (`AppleSignIn`).
- One file per feature in `Sources/Shared/Features/<Name>/`, holding the `@Observable final class XModel` and the
  `XView`. A screen's private pieces are `private struct`s in the same file.
- The model: `private(set) var state`, `@ObservationIgnored private let holder / watches`, `[weak self]` in every
  watcher closure, `send(_:)` as the only way to the ViewModel, `isolated deinit { holder.clear() }`. Views keep
  it in `@State`.
- Platform differences go behind `#if os(macOS)` / `#if os(iOS)`, or into `Sources/iOS` and `Sources/macOS` when a
  whole type differs (`QRScannerView`).
- Colors come from `Palette`; layout decisions from `ScreenLayout` (`.measuringScreenLayout($layout)`,
  `usesTwoColumns`, `isLarge`); shared controls from `DesignSystem/` (`PrimaryButton`, `AppleIDButton`,
  `GlassNotice`).
- Liquid Glass is native (iOS and macOS 26): `.glassEffect`, the system tab bar and sheets, no fallbacks.
- `///` doc comments explain a type's role and the why, as the Kotlin `// Why:` does; inline `//` comments only
  for a reason.
- User-facing text is an English literal in `Text`, `Label`, `Tab` or `.navigationTitle`, with its pt-BR
  translation in `Localizable.xcstrings` (the development language is English).

## TypeScript and React

No linter runs on the web app yet; `tsc -b` (part of `npm run build`) is the check.

- Function components and hooks, named exports, one screen per file in `src/features/<name>/` (kebab-case
  folders, `XScreen.tsx`).
- Imports through the `@/` alias; types from `blitzsplit-shared` with `type` imports
  (`import { RestorePresenter, type JsRestoreState } from 'blitzsplit-shared'`).
- Single quotes, no semicolons, two-space indentation.
- shadcn/ui components live in `src/components/ui` (generated by its CLI, `components.json`); pieces two screens
  share in `src/components/common.tsx`; Tailwind classes merged with `cn()` from `src/lib/utils`.
- A screen talks to Kotlin only through its presenter and the hooks in `src/bridge/usePresenter.ts`. It returns
  `null` until both the presenter and the first state exist.
- JSDoc `/** */` on exported hooks and components explains the role and the why; inline `//` only for a reason.
- User-facing text comes from `t('key')` (`src/i18n`), with the key in both the `en` and `pt` dictionaries (`tsc`
  fails when one lacks a key). Enum names the presenter sends (`state.error`, `state.syncBadge`) map to keys in a
  `Record` at the top of the file.
