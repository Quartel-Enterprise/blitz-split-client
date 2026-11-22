# CI

The workflows under `.github/workflows`: the checks that build and test the app, the Claude review that runs before
them, and `merge-when-green`, which merges a pull request once they pass.

| Workflow | Runs on | What it does |
| --- | --- | --- |
| `build-and-test` | Pull requests and `main`, except docs, `.claude` and `iosApp` only changes | `jvmTest` and `:koverVerifyCi` (with `compileCommonMainKotlinMetadata` and the store screenshots' `compileDebugUnitTestKotlin` in the same job), `jsTest` split across shards, `:androidApp:assembleRelease` and `:wearApp:assembleRelease`, `lintDebug` (which fails on a missing or an extra Android translation), `npm --prefix webApp run check:messages` (every web language against English's keys and `{placeholders}`) and `npm --prefix webApp run build`, each in a job of its own |
| `apple` | The shared Kotlin, `iosApp`, the build (on a macOS runner) | Both schemes of the Apple app, each in a job of its own, which link the `Shared` framework and run `iosApp/check-translations.py`, failing on a string without its pt-BR, es, fr, de, it or pl translation (or a Polish plural without its few and many forms); `compileKotlinIosArm64`, `compileKotlinWatchosArm64` and `compileAppleMainKotlinMetadata`, in the macOS scheme's job on a pull request (skipped on one that changes the Swift app alone) and in `apple-tests` on `main`, which also links `Shared` for the iOS simulator and the watchOS XCFramework there; `macosArm64Test` on `main` and on a pull request that changes Apple code (an Apple source set of a module, the version catalog, the build logic of the Apple targets), in two jobs when the change reaches nearly every module, and `iosSimulatorArm64Test` too on one that changes an `iosMain` or the version catalog |
| `static-analysis` | Kotlin, Gradle scripts, `.editorconfig`, the version catalog | `./scripts/ktlint.sh` |
| `duplication` | Kotlin, Swift, `webApp/src`, the jscpd config and baseline, `scripts/cpd.sh` | `./scripts/cpd.sh`, one warning when the baseline takes in copied code against the base branch, with each file in the job summary |
| `module-graph` | Gradle scripts, `build-logic`, the version catalog, the wrapper | `./gradlew assertModuleGraph` |
| `release-notes` | `release-notes/`, `store-listing/`, `scripts/release/`, the screenshots' store locales and screens (`StoreScreenshots.kt` of build-logic, `scripts/store-screenshots/screens.tsv`, and the generators' `StoreScreen.kt`, `StoreLocale.kt` and `StoreScreenshots.kt`), the check of a set of screenshots made on a Mac with the two workflows it is held to (`scripts/store-screenshots/local-set.mjs` and its tests, `app-store-screenshots.yml` and `app-store-screenshots-upload.yml`), what the tidy-up of the App Store's shelves removes and sorts with the lane it is held to (`scripts/store-screenshots/shelves.mjs`, its tests and `fastlane/Fastfile`), and the workflows that send Google Play's screenshots, which tests hold together and to the lane they run (`release-production.yml`, `play-store-screenshots.yml`, `play-store-screenshots-upload.yml`, the `play-screenshots-draw` and `play-store-lane` actions and `fastlane/Fastfile`) | `node scripts/release/release-notes.mjs check`, which fails on a fragment a release couldn't publish, `node scripts/release/store-listing.mjs check`, which fails on a store listing over a store's limit ([store-listing.md](store-listing.md)), and the tests of the release scripts: `release-notes.test.mjs`, `release-scripts.test.mjs`, `play-screenshot-groups.test.mjs`, `play-screenshots-lane.test.mjs` (the lane itself, run against a stand-in for Google Play; it needs the pinned fastlane and is skipped on the runner, so run it on a Mac when the lane changes), `store-listing.test.mjs`, `scripts/store-screenshots/local-set.test.mjs` and `scripts/store-screenshots/shelves.test.mjs` ([release-notes.md](release-notes.md)) |
| `codeql` | Pull requests and `main`, except docs, `.claude` and `iosApp` only changes that touch no Python script, and once a week | GitHub's code scanning (CodeQL), one job per language: the workflows, JavaScript and TypeScript, Python, and Kotlin, which it reads from a build of the `jvm` target and of the Android debug variant. Its alerts are under Security → Code scanning, and a pull request gets the ones it adds as review comments ([Code scanning](#code-scanning)) |
| `claude-review` | A pull request opened, or a draft marked ready; or the push that solves the conflict it opened with | Reviews it, fixes what it found, posts the rest |
| `claude-review-on-request` | The `ai-review` label | Reviews it again, only commenting |
| `run-checks` | The `run-checks` label | Re-runs the checks the gate held, once the review's threads are resolved |
| `claude-review-refs` | Pull requests that change a workflow | Fails when the workflows pin different releases of the shared review |
| `merge-when-green` | The `merge-when-green` label, each pull request workflow that completes, every push to `main`, every 15 minutes (when GitHub fires it), and by hand | Squash merges the pull request once every check passed and every conversation is resolved |
| `release-beta` | By hand, with `./scripts/release/release-beta.sh` | Ships a beta to the closed group: Android on Firebase App Distribution, the web on Cloudflare Pages, iOS and macOS on TestFlight, and iOS again on Firebase App Distribution as an ad hoc build. Merges the versionCode back into `main`, publishes the `X.Y.Z-beta_N` GitHub pre-release and sends the beta's release notes to blitz-split-site; see [release-process.md](release-process.md) |
| `store-listing` | By hand, by an administrator, on `main` | Sends the stores' listing text to Google Play and the App Store with `fastlane upload_listing`: text only, no build and no submission ([store-listing.md](store-listing.md)) |
| `app-store-screenshots` | By hand, by an administrator, on `main`, with the name of a set that `node scripts/store-screenshots/local-set.mjs send` uploaded as a draft GitHub release, or with `tidy_only` and no set | Sends App Store screenshots captured and framed on a Mac to App Store Connect, where only GitHub has the key: fetches the set, checks it again (a set that would empty a shelf is refused), uploads it one locale per job on the editable version of the iPhone's and the Mac's apps, says in the run's summary which locales landed and whether the shelves were left as staged, and removes the draft release once both are so. `locales` sends some of the set's locales alone and `tidy_only` sends nothing and only tidies the shelves up and checks them, to repair a shelf without sending the whole set again ([Repairing shelves](store-listing-screenshots.md#repairing-shelves)). Screenshots only: no build, no text, no submission ([store-listing-screenshots.md](store-listing-screenshots.md#sending-a-set-made-on-a-mac)) |
| `app-store-screenshots-upload` | Called by `app-store-screenshots` and `release-production`, once per App Store version | Uploads that version's staged screenshots with `fastlane upload_screenshots`, one job per locale, one after the other, then `fastlane finish_app_store_screenshots`, which reads every shelf in full, sorts it by file name, removes nothing, and fails, naming the locale, the shelf and the file, when one doesn't hold every screen once. With `tidy_only`, that last job alone |
| `play-store-screenshots` | By hand, by an administrator, on `main` | Draws the Google Play listing's screenshots on the runner and sends them to Google Play, a few locales per job (`group_size`, 6 unless the run says), with no release and without touching the App Store's; `validate_only` only draws them and asks Google Play whether it would take them. Refuses to start while a stable release, a listing or another run of itself is going ([store-listing-screenshots.md](store-listing-screenshots.md#sending-google-plays-alone)) |
| `play-store-screenshots-upload` | Called by `play-store-screenshots` and `release-production` | Groups the locales (`group the locales`, `group_size` to a group) and uploads the staged Google Play screenshots with `fastlane upload_screenshots platform:android locales:…`, one job and one Play edit per group, one after the other: only the images that changed, and no edit for a group Google Play already holds. A save Google Play refuses for its daily quota ends its job well with a warning and stops the groups after it, which are sent later ([store-listing-screenshots.md](store-listing-screenshots.md#google-plays-daily-save-quota)) |
| `site-release-notes` | Called by `release-beta` and `release-production`, as their last job | Sends a released version's notes to blitz-split-site through a pull request it merges there ([release-notes.md](release-notes.md#where-the-notes-go)) |
| `release-production` | By hand, with `./scripts/release/release-production.sh`, by an administrator of the repository, on `main`: nothing waits for an approval | Ships a stable version: a signed AAB to a Google Play track and the iOS and macOS builds to App Store review, each with the version's "What's new", the listings' screenshots made again when the run asks for them (`screenshots`; the App Store's on the iPhone and the iPad, and on the iPhone Duo and the Mac only with `screenshots_iphone_duo` and `screenshots_mac`; Google Play's alone go through `play-store-screenshots`), and the web app to https://app.blitzsplit.com on Cloudflare Pages. Tags `X.Y.Z`, publishes the GitHub release, merges the version and the removal of the release notes' fragments back into `main` and sends the notes to blitz-split-site; see [release-process.md](release-process.md#releasing-a-stable-version) |

Before a pull request opens, `create-pr` runs only the fast part of these on the author's Mac: the JVM tests and
coverage, a compile of the shared code for the iOS simulator and JS, the debug APK, and the UI the branch touches (the
web build, or the Xcode scheme a change to `iosApp` reaches). The tests on the JS target, Android lint, the release APK
and the other Apple scheme are left to these workflows, and `merge-when-green` waits for them: on one Mac, queued behind
the other worktrees' builds, they were what made a branch slow to open. The tests on macOS run on `main`, after the
merge, and on a pull request that changes Apple code; the ones on the iOS simulator only on a pull request that changes
an `iosMain` or the version catalog (see [Speed and timeouts](#speed-and-timeouts)).

## Speed and timeouts

`build-and-test` runs its jobs side by side, so its wall time is its slowest job's. Two of them are split to keep that
short:

- `js-tests` is the slowest: each module with tests links a test executable of its own and runs it in its own Karma
  and headless Chrome, and a change to a core module recompiles every module after it. Its matrix splits the modules
  across its shards, dealt round-robin from the sorted list of the modules that have `commonTest` or `jsTest` sources,
  found when the job runs, so a new module joins a shard on its own. Each shard compiles the modules its own depend on again, side by
  side with the others. Adding a shard is adding a number to the matrix.
- `android` builds the release APKs of the phone and the watch (R8 runs only there) and `android-lint` runs `lintDebug`. They compile different
  variants and share no work, so in one job they ran one after the other.

Every job has a `timeout-minutes`; the ones that build get about three times what they take on a pull request that
recompiles everything. A runner that hangs then fails its check instead of holding it, and `merge-when-green` with it, for GitHub's
default of six hours.

Only `main` saves the Gradle caches, one set per job and shard, and pull requests read them. The repository's Actions
cache has 10 GB and drops the least recently used entries first, so it keeps the newest `main` commits' entries, the
ones pull requests read.

A task comes from that cache while its inputs and the classpath of the build scripts are the ones `main` built with.
A new module leaves both alone for every other module, since modules name each other by path
(`project(":core:model")`): with Gradle's typesafe project accessors, generated from the list of modules onto that
classpath, each `include()` changed the key of every task, and the pull request that added a module, `main` after it
and the pull requests that ran before `main` saved its caches recompiled everything. Nothing else shows that, so the
root `build.gradle.kts` fails the build when the feature preview is on. A change to
`gradle/libs.versions.toml` still does that, an entry no module uses included, since the `libs` accessors are generated
the same way. A change to a convention plugin of `build-logic/` doesn't: only the tasks it configures differently get a
new key. To see why a task missed, run it with `-Dorg.gradle.caching.debug=true` before and after the change and
compare the lines of its key.

The Gradle configuration cache doesn't survive between runs: every job configures the build again on its first Gradle
invocation, about 40 seconds on a runner. Every job passes `secrets.GRADLE_ENCRYPTION_KEY` to the Gradle action, and
setup-gradle documents it as the key that encrypts the configuration cache it keeps between runs, but the secret is not
set: tried on 2026-11-17, with setup-gradle v6 and the key set (`openssl rand -base64 16`), it kept none. The first run
of `main` saved no configuration cache entry (the action's cache summary lists none), and a pull request job that had
the key and restored that run's Gradle home still printed "Calculating task graph as no cached configuration is
available". Trying again with a later version of the action only takes creating the secret: after a run of `main` with
the key, the next run of `main` on a commit that changes no Gradle script, `build-logic/`, `gradle.properties` nor the
version catalog (any of them invalidates the entry anyway) prints "Reusing configuration cache" on the first Gradle
invocation of `build-and-test`'s jobs, or it doesn't.

The `apple` jobs run on `xcode-27`, GitHub's macOS image with Xcode 27 (a public preview, since the app uses the iOS 27
SDK's APIs and `macos-26` only has Xcode 26), free for a public repository. Only five macOS jobs run at once, for the
whole organization, so what a pull request waits for there is a runner more than a build: on a day of three pull
requests at a time, each holding five runners, a job waited 11 minutes for one at the median and 34 at the 90th
percentile, and a pull request's `apple` run took 44 minutes of which its longest job ran 17. So a pull request takes
two runners, one per scheme of the app, and a third and a fourth only when it changes Apple code (below). Each job
compiles the Kotlin/Native code on its own, since two runners finish sooner than one: with both schemes in one job,
`apple-app` took 14 to 16 minutes whatever the change. The Gradle action fits them to the runner: it selects the image's
newest Xcode, as the Mac the app is developed on runs, and it shrinks the Gradle and Kotlin daemons to its 7 GB of
memory. Only `main` saves their caches, and only `apple-tests` saves the Kotlin/Native toolchain and the Gradle home, so
the jobs don't store the same entries twice.

`apple-tests` runs every module's `commonTest` on macOS, and it is the slowest Apple check: each module with tests links
a test binary of its own, and a change to a core module links all of them, 18 to 21 minutes in one job. `jvm-tests` and
`js-tests` already run that suite on two other targets, on Linux runners nobody waits for. What the macOS run adds is
the code only an Apple target compiles (12 files in 8 modules with tests around them) and how a library behaves there.
So on a pull request the `changes` job, on a Linux runner, lists what it changes, and `apple-tests` runs only when that
includes:

- an `appleMain`, `iosMain` or `macosMain` of a module under `core/` or `feature/`. A `watchosMain` starts nothing: no
  suite compiles it, and the schemes and "Compile the device targets" (below) do. Neither do the Apple source sets of
  `:shared`, which has no tests and no tested module depending on it: the schemes compile and link it;
- `gradle/libs.versions.toml`: a library publishes its code for each target apart, so a bump is the change a target can
  take differently;
- `gradle.properties`, where the `kotlin.native` settings are;
- the build logic that sets up the test tasks: the files of `build-logic/` named after the multiplatform convention
  plugin or the simulators (`IosSimulatorTests.kt`, `SimulatorFamily.kt`). Not the rest of `build-logic/`: the store
  screenshots' file there changes with every language, and the watchOS convention plugin only adds targets no suite runs
  on, which the schemes and "Compile the device targets" build;
- what runs the job: `apple.yml`, the Gradle action, `scripts/modules-with-tests.sh`.

A module's own `build.gradle.kts` is not on the list, though it can give an Apple source set a dependency: the schemes
resolve and compile it on the pull request, and its tests run on `main`.

Any other pull request leaves the suite to `main`'s run, after the merge. In the 300 `apple` runs and 70 pull requests
before this rule, `apple-tests` failed on two commits of pull requests, both commits whose `build-and-test` failed too:
it found nothing the other targets hadn't. A moved file counts under both of its paths, and a diff that fails counts as
a change to all of it. A skipped check counts as passed for `merge-when-green`.

The iOS simulator runs the suite only on a pull request that changes an `iosMain`, the version catalog, the build logic
of the test tasks (above) or `apple.yml` itself, whose simulator steps no other run would try, and never on `main`. A
pull request whose `changes` job failed runs it too, as it runs `apple-tests`. Its run repeats the macOS one: the suite
is the same `commonTest`, the actuals it reaches are `appleMain`'s (only `core/profile` and `core/in_app_update` have an
`iosMain` and a `macosMain`, one line each that no test reaches), `create-pr` compiles the main and the test code for
the simulator before the pull request opens, and `apple-app` links `Shared` for it. It links one more test binary per
module, which on `main`, on every push, made a run that recompiles everything take up to 48 minutes on a runner the pull
requests waited for, and a week of its failures there were all the simulator's own (CoreSimulator crashing, no device
listed), none a test. `main` compiles nothing for the simulator's tests any more, so a pull request that runs its suite
links every module's test binary for it.

A pull request whose `apple-tests` runs and whose change reaches nearly every module splits it in two jobs, `apple-tests
(1)` and `apple-tests (2)`. The `changes` job decides it from the changed paths: anything under `core/` or `gradle/`
(the version catalog, the wrapper), or `gradle.properties`, the source sets no Apple target compiles and Markdown left
out. It is a guess, on the cheap side: a core module nothing depends on splits for nothing, which costs one runner five
minutes, and a change that reaches every module from elsewhere runs in one job. `build-logic/` and the root's Gradle
scripts are left out, since a change there gives a new key only to the tasks it configures differently (see above). The
modules that have a `commonTest` are dealt in turn from their sorted list, the one `js-tests` deals its own from
(`scripts/modules-with-tests.sh`, which leaves out a folder `settings.gradle.kts` doesn't include), so a new module
joins a shard by itself. Each shard compiles for macOS the modules its own depend on, which leaves `:shared` to
`apple-app`'s macOS build: no test depends on it, and it depends on every module. `main` keeps one job, since no pull
request waits for its run. A change to `apple.yml` itself or to that script also runs both shards, so a change to the
split is seen working before it merges.

`apple-app` always runs, and its Gradle time has a floor the cache can't lower: Kotlin/Native caches the link of a test
binary but not the link of a framework, so each scheme links `Shared` again, close to four minutes of Gradle each, with
every module's compilation restored. A change to a core module restores none: Kotlin/Native compiles a module again when
a module it depends on changed at all, so each job then compiles every module for its target. `apple-app (iOS)` builds
the Apple Watch app with the iPhone app that embeds it, linking the watchOS `Shared` for the simulator.

`apple-app (macOS)` then compiles the device targets, which no scheme builds on a pull request: `compileKotlinIosArm64`,
`compileKotlinWatchosArm64` (the watch's arm64_32) and `compileAppleMainKotlinMetadata`, the `appleMain` shared by the
Apple targets, which only a macOS host compiles. They are compiled and not linked: a job of its own that linked the
watchOS `Shared` for both slices held a fifth runner on every pull request for 7 to 16 minutes, and what a device slice
can take differently from the simulator's shows when it compiles. The step is skipped on a pull request that changes the
Swift app alone, when every file is under `iosApp/` or `release-notes/`, or is Markdown: no input of a Gradle task of
the Apple targets changed. It runs after a scheme that failed too, so a Swift error doesn't hide a device target's until
the next push. On `main` the compilations are `apple-tests`' instead, with
`:shared:linkDebugFrameworkIosSimulatorArm64`, so the Gradle home that job saves, the one every macOS job restores,
holds what the pull requests' jobs read. That link is also what keeps the simulator's libraries in the Kotlin/Native
entry (below), which only that job saves: a library is compiled to native code on its first link for a target, and the
simulator's test binaries were the links that did it there. There `apple-tests` also links the watchOS `Shared` for the
device and the simulator into an XCFramework (`:shared:assembleSharedDebugXCFramework`), after the tests, the only link
of the device slice outside a release. A job of its own saving a home on `main` would be one more entry that
`drop-failed-cache` doesn't clean, and one a job without an entry of its own (`apple-app`) could restore in place of
`apple-tests`' (setup-gradle falls back to the newest entry of the runner's OS). Kotlin/Native keeps no library cache
for the watchOS targets, so `~/.konan` gains nothing from it. No test runs on the watchOS simulator in CI: the suite is
the same `commonTest` over the same `appleMain` as macOS's, and `./scripts/heavy-check.sh ./gradlew
:<module>:watchosSimulatorArm64Test` runs it on the Mac once the watchOS runtime is installed (`xcodebuild
-downloadPlatform watchOS`); without it the suites are skipped.

A pull request's jobs wait for a runner while all five are taken. So the `apple` runs of `main` share one concurrency
group that never cancels a run once it started: a push that arrives meanwhile waits, and a later one takes its place in
the queue. A burst of merges then runs the first commit and the newest, one after the other, each starting from the
Gradle cache the one before saved, and that run holds two of the five runners at most: no pull request waits for it, so
there the two schemes of `apple-app` take turns on one runner (`max-parallel`). With a group per commit, as
`build-and-test` has on its Linux runners, every merge's run started at once from the same cache and recompiled what the
others were compiling: over a day of twenty merges they took half of the macOS runners' time, and the pull requests'
jobs waited up to 36 minutes for a runner. A run of `main` therefore stands for the pushes it replaced in the queue too,
whose changes its own push doesn't list, so on `main` the `changes` job is skipped and `apple-tests` always runs, a push
to the Swift app alone included. A commit of `main` whose `apple` run shows as cancelled was replaced in the queue: the
next run that completed is the one that checked it. A run of `main` run again by hand stays out of that queue, in a
group of its own, or it would take the place of the newest push waiting there.

On `main`, a failed `apple-tests` leaves no Gradle home in the cache: the `drop-failed-cache` job deletes the entry
the run saved. setup-gradle saves the home when the job ends, whatever its result, and every later run restores the
newest entry. A run that lost the network restored nothing, found no plugin to configure the build with, and saved an
empty home; the next run of `main` restored it and compiled everything (40 minutes, with no task from the cache), and
so did the pull requests opened meanwhile. Without that entry they restore the one of the last run that passed. When
the job warns that the cache lists no home of the run, look for one yourself
(`gh cache list --ref refs/heads/main --key gradle-home-`, the key that has `apple-tests` in it and ends with the
commit) and delete it with `gh cache delete <id>`. The jobs of `build-and-test` save their homes the same way, and
nothing deletes those yet.

`apple-tests` bounds its test step rather than the job, since only the step knows which targets run: 60 minutes on macOS
alone, where recompiling everything took 22, and 80 with the simulator too, where it took up to 48 on `main`. `main`
keeps the 80: its step also links `Shared` for the simulator and compiles the device targets, a cold run of which is not
measured yet, and a run the bound kills saves no cache for the next to start from. `apple-app (macOS)` has 60 minutes,
for its scheme and the device targets after it, which have 30 of their own; `apple-app (iOS)` keeps 40. The run with the
simulator is the exception to the three times above: three times 48 would hold a runner for over two hours. A run the
step's timeout kills saves no Kotlin/Native entry, since it may have left a library half compiled. The job's own
`timeout-minutes` of 100 only stops what hangs around the tests, with room for the caches restored before the longest
run and saved after it.

A test that fails on an Apple target alone therefore shows up after the merge, as a failed `apple` run on `main`. The
`report-failure` job then opens the issue "The apple checks failed on main", assigned to whoever merged the commit that
ran, or comments on it while it is open; it does the same when `apple-app` fails or a job times out there. The commit
the issue names is the one the run checked out, and what broke may be any push since the last `apple` run of `main` that
completed, since a run also stands for the pushes it replaced in the queue: the issue links those commits. Fix it in a
pull request of its own, checked on the Mac, since that pull request runs `apple-tests` only if it changes Apple code:
`./scripts/heavy-check.sh ./gradlew :<module>:macosArm64Test`. Close the issue once `main` is green.

The simulator's suites run one module at a time, while Gradle still links their test binaries side by side: the
`blitzsplit.kotlin.multiplatform` plugin gives every `iosSimulatorArm64Test` and `watchosSimulatorArm64Test` a shared
build service with a single slot.
Each suite starts its own `simctl`, and two at once could crash CoreSimulator while it reported the device set
(`Invalid parameter not satisfying: devicePair`), failing one module's suite with exit code 134 before any of its tests
ran. A suite, the boot of its own simulator included, took 3 seconds on the Mac (`:core:model`), so running the
modules with tests in turn adds a few minutes to the step, well within its 80.

The same service hands each suite its device, the one the Kotlin plugin picks by default: the first of the last iOS
runtime that `xcrun simctl list devices available` lists. The plugin asks for it once per module and fails the
module's suite when the answer has none (`Xcode does not support simulator tests for ios_simulator_arm64`). That
happened on `main` twelve minutes after a run's first suite: the suites of 27 modules had passed, and the 22 after
them failed without running a test, with nothing in the log to say why the devices were gone. So when the simulator
lists no device, the service logs its state (every device with the reason it is unavailable, the runtimes, and the
disk images they are mounted from), and on CI, where `CI` is `true`, the suite waits for a device to be listed again:
up to about ten minutes for the whole build, so a simulator that never comes back holds the job once, not once per
module: each suite left asks `simctl` once more and fails without waiting. A `simctl` that hangs counts as no device:
each call gets a minute, and when it still hangs once the wait is over, the suites left fail without asking it again.
An `xcrun` that cannot run at all is not waited for. Off CI the suite fails at once, with the same state in the log,
since a Mac with no iOS runtime installed has nothing to wait for. When `apple-tests` fails with "The iOS simulator
has no available device", look for "The iOS simulator lists no available device" in its log: what follows says what
was missing.

The Kotlin/Native entry (`~/.konan`) holds the toolchain and the native code each library is compiled to on its first
link. Every Kotlin/Native job restores the newest entry of its Kotlin version, and `apple-tests` saves it on `main`
when it ends, under a key with a fingerprint of the libraries compiled in it and the run that saved it: a link that
compiled a library's new version changes the fingerprint and saves a new entry, which is then the newest, a run whose
fingerprint is the newest entry's saves none, and a failed test doesn't stop the save. With the Kotlin version alone
as the key, the entry was saved once and every run compiled the libraries bumped since. An entry keeps the versions
bumped away until a new Kotlin version starts an empty one.

## Downloads

Gradle downloads what the cache it restored doesn't hold. Maven Central rate-limits by IP address, and GitHub-hosted
runners leave through IPs every project on GitHub Actions shares, so it may answer `429 Too Many Requests` to traffic
that isn't ours. Three things keep that from failing a job:

- **The mirror.** The plugin and dependency repositories of `settings.gradle.kts` and `build-logic/settings.gradle.kts`
  list Google's mirror of Maven Central (`maven-central.storage-download.googleapis.com/maven2`) before
  `mavenCentral()` (`build-logic` has no `mavenCentral()` among its plugin repositories: the Plugin Portal comes
  after the mirror there). Maven Central stays as the fallback for an artifact the mirror doesn't have: a release
  published minutes ago, and a few it lacks for good (BuildKonfig 0.24.1 and `software.quare:loadable` 1.2.0 were
  404s on 2026-11-20). A repository's content filters stay where they are: the mirror only takes what reaches Maven
  Central today. The fallback is per module, not per file: Gradle downloads a module's jar from the repository that
  had its POM, so a module the mirror holds only in part (its POM but not its jar yet) fails with `Could not find
  <module>.jar (MavenCentralMirror)` instead of asking Maven Central. Should that happen, exclude the module from the
  mirror (`content { excludeModule("<group>", "<name>") }` in its `maven(...)` blocks) until the mirror catches up.
- **The retries.** `gradle.properties` raises Gradle's tries of a download that fails with a 429, a 5xx or a dropped
  connection from 3 to 6, and the first wait from 1 to 2 seconds, doubled each time: about a minute of waits in all
  before Gradle gives up, plus each try's own time, up to the 30 second socket timeout when a connection hangs. A
  repository that still fails is disabled for the rest of the build ("Repository MavenRepo is disabled due to earlier
  error"). Gradle then stops looking in the repositories after it too, unless the repository allows it: the mirror
  sets `allowInsecureContinueWhenDisabled`, so a disabled mirror leaves the rest to Maven Central. Maven Central
  doesn't set it, so once it is disabled the build fails, and where the Gradle Plugin Portal comes after it (the
  root's `pluginManagement`, `build-logic`'s dependencies) the portal is no longer asked either.
- **A warm cache.** Pull requests restore `main`'s newest Gradle home (see "Speed and timeouts"), and setup-gradle saves
  it on `main` whatever the job's result, so a red `main` still saves what it downloaded. The one exception is
  `apple-tests`, whose home a failed run deletes (`drop-failed-cache`, see "Speed and timeouts"), so the macOS jobs
  restore the last green one. A red `main` downloads nothing when the build fails while configuring, as `main` did
  after the library bump of #522 (`compileSdk = "37.1"`): every pull request then downloads the new versions, every
  job of each one at once. No warm-up job would help there, since resolving the dependencies needs a configured build,
  so the mirror and the retries carry those runs until `main` is green again.

A 429 shows in the failed task as `Received status code 429 from server: Too Many Requests`. The log of the step
"Setup Gradle" says which Gradle home the job restored ("Restored cache entry with key gradle-home-...-<commit>"): the
commit of `main` it came from.

## AI review

Claude reviews a pull request at three points:

- **Before it opens**, `create-pr` runs `/code-review high --max-findings all` and `review-conventions`, on the
  author's machine, and fixes their findings before pushing.
- **On GitHub, when it opens**, `claude-review` runs the same `/code-review high --max-findings all`, on Opus, with
  `--fix`: it applies the findings that are clear and contained, pushes them to the branch as a commit of
  `claude[bot]`, and posts one review. The fixed findings are listed in its body, under the fix commit; each one left
  for the author is an inline comment, labelled 🔴 **Must fix**, 🟡 **Should fix** or ⚪ **Nitpick**, with a
  suggestion to commit when the fix fits the commented lines. A draft is reviewed when it is marked ready. Later
  pushes, the fix commit included, aren't reviewed again.
- **On request**, the `ai-review` label runs `claude-review-on-request`: the same review, only commenting, told what
  the earlier reviews found so it doesn't repeat them. The label is removed when it ends. Use it after new pushes.

Both fail only when the run itself does (an expired token, the 30-minute timeout), never because of what they found.
They are skipped on pull requests from forks, which get no secrets. The action refuses to run when the pull request
changes the workflow file that started it, so a change to either review workflow can only be tried after it merges.

The review, the gate and the threads lookups are the `review`, `gate` and `threads` actions of
[Quartel-Enterprise/claude-review](https://github.com/Quartel-Enterprise/claude-review), shared with
bible-planner-mobile-client. Every `uses:` of them (`claude-review.yml`, `claude-review-on-request.yml`,
`review-gate.yml`, `run-checks.yml`, `merge-when-green.yml`) pins the same exact release: a fix there ships as a release, then a bump of all
of them here, which `claude-review-refs` checks. Renovate opens that bump as one pull request as soon as the release
ships (see [Dependency updates](#dependency-updates)), and the review skips it, since it changes the review's own
workflow. What runs comes from that release, never from the pull request under
review, so a pull request can't change the review that judges it. How the review works, which Bash commands it may
run and how it keeps the tokens out of its comments are in that repository's README.

The action hides Claude's output, since this repository's logs are public. A tool the review was denied, or a Bash
command its guard refused, shows as a warning on the run; allowing one is a change to the shared repository (see its
README, "Security"). Only someone who can push a branch to this repository can try a prompt injection, since forks
are never reviewed. If a token ever shows up in a comment, a commit or a log, revoke it: create a new OAuth token
with `claude setup-token` and update `CLAUDE_CODE_OAUTH_TOKEN`; the GitHub tokens expire with the job.

### Setup

- The [Claude GitHub App](https://github.com/apps/claude) installed on the repository.
- The `CLAUDE_CODE_OAUTH_TOKEN` secret, created with `claude setup-token` on the maintainer's account (what it
  bills is in [Which key each job uses](#which-key-each-job-uses)). When it expires the review fails on
  authentication: create a new one and update the secret.
- The `ai-review`, `run-checks` and `merge-when-green` labels.
- The `MERGE_WHEN_GREEN_TOKEN` secret, for `merge-when-green` (see [Merge when green](#merge-when-green)).
- A ruleset on `main` with **Require conversation resolution before merging**. This is what keeps a pull request
  with open review threads from merging: the checks the gate holds are skipped, not failed, and GitHub counts a
  skipped required check as passing.

No check is required in that ruleset. Every check workflow is path filtered, so a required one would leave a pull
request it doesn't run on (docs only, `iosApp` only) waiting forever, and the gate's own `wait for review` job is
skipped on every push but the opening one, which also counts as passing. Without required checks, `create-pr`
adds the `merge-when-green` label, which merges only once every check of the head commit is green, and waits with
`gh pr checks` itself only when the label can't merge (no `MERGE_WHEN_GREEN_TOKEN`) or when asked to merge right
away.

### Which key each job uses

| Job                                  | Key                                   | Billed to                    |
|--------------------------------------|---------------------------------------|------------------------------|
| `claude-review`                      | `CLAUDE_CODE_OAUTH_TOKEN` (the plan)  | The plan's usage limits      |
| `claude-review-on-request`           | `CLAUDE_CODE_OAUTH_TOKEN` (the plan)  | The plan's usage limits      |
| Every other workflow                 | None: no other job calls Claude       | -                            |

The plan's monthly API credit (Max US$100, Ultra US$200, see the
[help center](https://support.claude.com/en/articles/17154008-monthly-api-credits-for-max-ultra-and-team-plans))
can't pay for the review, even with an API key of the linked Console organization. The help center counts "runs started
by the Claude Code GitHub Action" as Claude Code usage, which the credit doesn't cover, even when the action runs
Claude with `-p`; and the review action runs Claude through `anthropics/claude-code-action`, which tags every run it
starts with `CLAUDE_CODE_ENTRYPOINT=claude-code-github-action` (`base-action/src/parse-sdk-options.ts`). Passing the
action's `anthropic-api-key` instead of `oauth-token` would only bill the reviews per token, outside the credit. Only
`claude -p` or the Agent SDK run in a plain step, with that organization's key, draws from the credit, which would mean
replacing `claude-code-action` in Quartel-Enterprise/claude-review, and with it the Claude App's token the review
pushes and posts with.

The credit wouldn't cover the reviews anyway: from 18 October to 17 November 2026 the reviews reported US$352 of API
usage here (183 reviews, US$1.92 each on average), and US$421 together with blitz-split-site and blitz-split-api, more
than either plan's credit. So the reviews stay on the plan. If one ever moves to a key, it gets a workspace of its own
in the linked organization with a spend limit, auto-reload stays off, and a run that finds the credit spent fails
with an error that names it, never falling back to a card.

## The review gate

The checks wait for the opening review instead of running next to it, so they don't run twice (once on the opening
commit, again on the fix commit):

- `build-and-test`, `apple`, `static-analysis`, `duplication`, `module-graph`, `release-notes` and `codeql` start with a `review-gate` job, the reusable
  `review-gate.yml`. On the commit a pull request opened with or a draft was marked ready on, and on the review's
  fix commit (a push by `claude[bot]`), it waits for the `claude-review` run of the reviewed commit to end (on a draft
  marked ready, only a run that event created, never the one skipped while it was a draft). When the
  head moved meanwhile (the fix commit), it skips the checks here: they run on the new commit. Every other event (an
  author's push, a reopened pull request, `main`) skips the gate, and the checks run right away. A draft, a fork, a
  failed or cancelled review, one still running after 40 minutes or GitHub's API failing three times in a row let
  the checks run.
- GitHub starts no pull request workflow while the pull request conflicts with its base, so one opened (or marked
  ready) with a conflict gets neither the review nor the checks on that event. `claude-review` also runs on pushes
  for that case: on a push, the shared `review` action looks for a `claude-review` run between the pull request's
  opening (or a draft's last marking as ready) and this one, and with none (GitHub started none while it conflicted)
  the push that solved the conflict is reviewed. The gate
  doesn't wait for that review, since the push is an author's: the checks run on it right away, and again on the
  review's fix commit, if any. Every other push ends the run in seconds without reviewing.
- Once the review ended, the gate counts its unresolved threads. With one open, the checks are skipped and the gate
  fails in its `Hold for the open threads` step, so the pull request shows why nothing ran. The author's next push
  runs them. Resolving the threads without a push (a finding answered instead of fixed) starts no workflow: add the
  `run-checks` label, which re-runs every held run of the head commit, or comments when a thread is still open.

`review-gate.yml` is a thin wrapper of the shared `gate` action, which waits and counts the threads; the wrapper
only fails the hold, in its `Hold for the open threads` step. The shared `threads` action holds what `run-checks`
agrees on with the gate: which threads are the review's, and which runs were held (any failed run of the commit whose
`Hold for the open threads` step failed, so no list of the gated workflows has to be kept).

The fix commit isn't built before the push, since the review has no Gradle: a fix that breaks the build or ktlint
shows up in the checks of the fix commit. The local branch is one commit behind afterwards, so pull before pushing to
it again.

## Merge when green

Adding the `merge-when-green` label to a pull request makes the `merge-when-green` workflow squash merge it once
every check on its head commit has passed, then remove the label. GitHub's own auto-merge can't do this: it waits
only for the required checks, and none is required here (see [Setup](#setup)).

The workflow runs when the label is added and each time a pull request workflow completes, so the last check to
finish is the one that triggers the merge. GitHub loses some of those events: a run of this workflow that fails to start
(`startup_failure`), or a `workflow_run` never sent. So it also runs a `sweep`: a job that lists the open pull requests
with the label (drafts and forks left out) and checks each one the same way. GitHub's scheduler can't be what drives it:
it fired about one run in 26 of the `schedule` here (GitHub drops scheduled runs under load, and the workflow has no
workflow-level `concurrency` that could cancel them; only the `merge` job has one, per branch). The sweep therefore runs on every push to `main` (each
merge), every 15 minutes when GitHub does fire the schedule, and by hand.
The push trigger only fires when something lands on `main`: with `main` idle, a pull request that missed its event waits for the
schedule or for someone to dispatch the workflow. To check them right away, run
the workflow by hand (**Actions**, `merge-when-green`, **Run workflow**), with a pull request number to check only that
one. The sweep only sees pull requests that still have the label: one it removed for an unresolved conversation needs
the label added again, as below. A push to the pull request moves its head commit, and the new checks have
to pass again. A failed check leaves the pull request open with the label on; re-running the check and getting it
green merges it. The pull request's current head is the one judged, whichever commit's run started the workflow, so
a pull request whose changes run no check workflow (docs only, `iosApp` only) merges once the review ends, even after
the review pushed its fix commit. Every push runs `claude-review`, which ends in seconds without reviewing (see the
review gate below) and starts `merge-when-green` when it completes. A label added, or a sweep run, right after a push
still waits 30 seconds first, for the push's workflows to register their runs. Drafts wait until they are marked
ready, and pull requests from forks are never merged: their author could push after a maintainer added the label.

A job waiting on another has no check until it starts, so while `review-gate` waits for the review, or right after it
passes, every check of a pull request can be green while its workflows still have jobs to start. Every run of the
head commit therefore counts as unfinished until it completes, and its completion starts `merge-when-green` again. So
does a `claude-review` run of an earlier commit, which ends on the opening commit after pushing its fix commit. Only the
runs a label starts (`claude-review-on-request`, `run-checks`, `merge-when-green`) don't count.

GitHub sometimes leaves a job's check in progress after its workflow run completed: the job has no steps and never
ends. A check still pending in a completed run takes the run's conclusion instead (a run succeeds only when every job of
it did), with a warning on the `merge-when-green` run that names the run.

Two kinds of check turn green with nothing to start `merge-when-green` then:

- The `CodeQL` check code scanning adds to a pull request, which is no workflow run: it turns green a little after the
  `codeql` run completes.
- The `Analyze` checks of GitHub's Code Quality. They are jobs of a run GitHub starts on its own on every push of a
  pull request, docs only included: its event is `dynamic`, its workflow (`dynamic/github-code-quality/codeql`) is no
  file of the repository and it is named after the pull request, so a `workflow_run` trigger can't name it. On a pull
  request whose changes run no check workflow they are the last checks to finish, after the `claude-review` run that
  started `merge-when-green`: such a pull request used to sit green with its label on until the next push to `main`
  swept it.

So while the only checks left are pending ones of those two kinds, `merge-when-green` reads the checks again before
giving up until the next event or sweep, every 20 seconds for the first two minutes and every minute after that:

- **For 15 minutes** while a `dynamic` run that is still going owns one of them. A Code Quality run takes a minute as
  a rule, and took 13 at its longest.
- **For 2 minutes** for a check no run of Actions owns, as before.

A pending check of one of the repository's own workflows, or a failed check, still ends the run at once: that
workflow's completion starts another. Around that wait:

- A `dynamic` run of the head commit that is still going while every check there is has passed gets 2 minutes for
  its jobs to have their checks. Past that it is a run with no check on the commit (stuck in progress, or one that
  checks nothing): the pull request merges, with a warning that names the run.
- On every round after the first the pull request is read again: one that was merged or closed meanwhile, made a
  draft, lost its label or got a push ends the wait, unmerged by that run (a push's own runs decide).
- A read GitHub's API fails in the middle of a wait is taken again, for 2 minutes. A pull request is never merged on
  a round whose reads didn't all answer: the run ends with a warning instead, or fails when it is the checks that
  can't be read.
- A Code Quality check that failed and was re-run green starts nothing either: add the label again, or run the
  workflow by hand.

The `Main rules` ruleset requires every review conversation to be resolved before a pull request merges into `main`,
with no bypass, admins included, and `merge-when-green` never merges with `--admin`. When everything else is green
and a conversation is still open, it removes its label and comments on the pull request instead. Fix each finding,
or reply with why the code stays as it is, resolve the conversation, then add the label again: resolving one starts
no workflow. When the opening review's threads held the checks (see [The review gate](#the-review-gate)), the failed
gates don't count as failed checks: once every conversation is resolved, `merge-when-green` re-runs the held runs,
keeps its label and merges when they pass. A merge GitHub refuses with everything green (a conflict with `main`, or a
token without the Workflows permission, see below) also removes the label, with a comment saying why.

The merge uses the `MERGE_WHEN_GREEN_TOKEN` secret, not `GITHUB_TOKEN`: a merge made with `GITHUB_TOKEN` triggers no
other workflow, so the push to `main` would run none of its checks. The merge shows up as made by the token's owner.
The held runs are re-run with it too, since a re-run made with `GITHUB_TOKEN` may not start `merge-when-green` when it
completes. Before the secret is set, or once the token expires, the workflow fails with an error that names the
secret. The checks are read with `GITHUB_TOKEN`, which the job grants `checks: read` and `statuses: read`.

**Manual setup** (Pierre): create a fine-grained personal access token restricted to this repository, with Actions,
Contents, Pull requests and Workflows set to Read and write, and save it as the `MERGE_WHEN_GREEN_TOKEN` repository
secret. Until then, the label does nothing but fail the workflow.

Workflows is what lets it merge a pull request that changes a file under `.github/workflows/`. Without it GitHub
refuses that merge, with every check green and every conversation resolved. `gh pr merge` then only says "the base
branch policy prohibits the merge", while the REST endpoint's answer names the reason: "refusing to allow … to create
or update workflow … without `workflow` scope". So `merge-when-green` merges through the REST endpoint, and quotes its
answer in the comment it leaves when it removes its label. A merge that only failed for now (no answer, an error of
GitHub's own, `main` moved under it) keeps the label, and the next run tries again. Such a pull request still merges from the pull request's page, with "Squash and merge". A token's permissions
are edited in place (Settings › Developer settings › Personal access tokens › Fine-grained tokens), which keeps its
value, so the secret stays as it is.

When a workflow is added to or renamed in the pull request checks, update the `workflow_run` list in
`merge-when-green.yml` too. A workflow missing from it still blocks the merge while it runs, but its completion does
not trigger one. A new workflow started by a label goes in the list of label workflows of its `Check whether every
check passed` step instead: a run of it still queued would hold the merge, and its skipped completion would start
nothing.

## Dependency updates

[Renovate](https://docs.renovatebot.com/) proposes the new releases of every platform: the Gradle catalog, the
settings plugins and the Gradle wrapper, the npm packages of `webApp/` and `scripts/store-video/remotion/`, the Swift
packages of `iosApp/project.yml` (its `xcodegen` manager) and the `uses:` of the workflows and actions. It runs as the
Mend Renovate GitHub App, free, installed on this repository; `renovate.json` is its whole configuration. It replaced
Dependabot, which only bumped the two Quartel-Enterprise actions.

- Every minor and patch goes in one pull request a week, `chore: bump the libraries of every platform`, opened on
  Monday before 9:00 (São Paulo). A major gets a pull request of its own, so its breaking changes are fixed apart from
  the rest; the packages that must share a version (React, Tailwind, Remotion) move their major together.
- The claude-review refs move together in one pull request, and copied-code-check in one of its own, both as soon as
  a release ships.
- The stability rule and the pins are those of the `bump-versions` skill: a stable release only moves to a newer
  stable one; `@js-joda/core` follows kotlinx-datetime by hand; `androidx-lifecycle` moves only once JetBrains'
  `lifecycle-viewmodel` has the release; the CI runtimes (`java-version`, `node-version`,
  `ruby-version`) and the Android SDK levels change only when the build needs them.
- Renovate never merges. Its pull requests run the checks like any other: read the release notes it links, fix what
  breaks on the branch (or run the `bump-versions` skill for a major that needs code changes), and add
  `merge-when-green`. A Kotlin bump that brings new npm packages fails the JS build with "Lock file was changed":
  run `./gradlew kotlinUpgradeYarnLock` on the branch and commit `kotlin-js-store/yarn.lock`. Once anyone (or
  claude-review's fix commit) pushes to a Renovate branch, Renovate stops rebasing it, and ticking rebase on the
  Dependency Dashboard would throw those commits away: merge `main` into it by hand instead.
- The Dependency Dashboard issue lists every update pending, held for the schedule or failing. Tick one there to
  have Renovate open its pull request now.

To check a change to `renovate.json` before it merges:
`npx --package renovate renovate-config-validator --strict renovate.json`.

## Code scanning

`codeql` is CodeQL's advanced setup, a workflow of this repository, instead of the default setup of the repository's
settings (Settings → Advanced Security → Code scanning). A repository runs one or the other: with the default setup
on, GitHub refuses what the workflow uploads. The default setup can't build the Kotlin code, so it stays off, and the
workflow also analyzes the languages that need no build.

- Kotlin is read from inside the compiler, so CodeQL only sees the files a build compiles while it watches:
  `compileKotlinJvm` and `:androidApp:compileDebugKotlin`, without the build cache, since a compile task restored
  from it runs no compiler. The actuals of the Apple and JS targets are not analyzed, nor are the tests (the
  `:android:store_screenshots` module among them), which neither task compiles.
- CodeQL supports a range of Kotlin versions, and a new CodeQL release follows a new Kotlin one by some weeks. When a
  bump of Kotlin fails `analyze (java-kotlin)` on an unsupported version, wait for the CodeQL release that supports
  it, or merge without that check.
- Swift is not analyzed: the Apple app needs Xcode 27 and Swift 6.4, and CodeQL supports neither yet
  ([the announcement](https://github.blog/changelog/2026-10-19-codeql-changes-for-macos-27-xcode-27-and-swift-6-4/)).
  Once it does, add a `swift` entry to the matrix, on the macOS runner, with the build of `apple-app` as its build,
  and take `iosApp/**` out of the workflow's path filter.
- Ruby is not analyzed either: the only Ruby is `fastlane/Fastfile`, and CodeQL's analysis of it failed.

Besides the `analyze` jobs, code scanning adds a check of its own to a pull request, `CodeQL`, which fails when the
pull request adds an alert of high severity. It has no run and no log: read the alert on the pull request, or with
`gh api 'repos/{owner}/{repo}/code-scanning/alerts?ref=refs/pull/<number>/merge&state=open'`, and fix the code. An
alert that is wrong is dismissed on GitHub with its reason, never by taking the query or the file out of the analysis.
A dismissal starts no workflow, so, as after resolving a conversation, add the `merge-when-green` label again for the
pull request to merge.

Secret scanning with push protection and code quality (JavaScript, TypeScript and Python) are also on, in the
repository's settings alone.

## Adding a workflow

- Scope it with `paths` or `paths-ignore`, so a docs-only change doesn't run a build.
- Give every job a `timeout-minutes` (see [Speed and timeouts](#speed-and-timeouts)).
- Use per-commit `concurrency` groups on `main` if it writes a Gradle cache, per-ref otherwise: a cancelled run saves
  no cache. `codeql` is per commit too: a commit of `main` without its analysis would hand its alerts to the pull
  requests based on it. On the macOS runners, use `apple`'s group instead: one for every push to `main`, which doesn't
  cancel in progress (see [Speed and timeouts](#speed-and-timeouts)).
- If it checks the app on pull requests, start it with the `review-gate` job, with the same `pull_request` types
  and `if` as the other callers, and make its other jobs need it.
- Set up Gradle through `./.github/actions/gradle` and pass `secrets.GRADLE_ENCRYPTION_KEY`, as the other jobs do,
  though the secret is not set today (see [Speed and timeouts](#speed-and-timeouts)).
- If it runs on pull requests, add it to the `workflow_run` list of `merge-when-green.yml` (see [Merge when green](#merge-when-green)).
