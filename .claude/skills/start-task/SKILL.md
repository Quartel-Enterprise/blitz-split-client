---
name: start-task
description: "Start a new task branched fresh from origin/main, either in an isolated git worktree or in-place in the current checkout, with its own Android emulator or iOS simulator only when the task needs a device."
---

# Start Task

This skill starts a new task from a fresh `origin/main`, either in an isolated git worktree under
`.claude/worktrees/` or in-place in the current checkout. A worktree keeps the task fully separate
from the main checkout and any other in-progress task, at the cost of an extra setup step (copying
git-ignored local config into the new worktree, and generating the Xcode project or installing the
web dependencies when the task needs them). In-place is faster to start but ties up the current
checkout for this one task.

When the task will need a device, it also gets one of its own, `BoraRachar_<short_description>`:
an Android emulator, an iOS simulator, or both. Its device work then never touches an emulator or
simulator the user or another session is using. Tasks that never need a device don't get one.

## Step-by-step workflow

### 1. Determine how to start

Check whether the user's request already specifies a mode (e.g. "cria numa worktree", "começa
in-place", "direto no checkout atual"). If it does, use that mode and skip straight to step 2.

Otherwise, ask the user directly: "Quer que eu crie isso numa worktree isolada, ou direto in-place
no checkout atual?" Do not assume a default: wait for their answer.

### 2. Determine the branch type and short description

Pick the `<type>` prefix using the table in [`branch-types.md`](../branch-types.md) (shared with
`create-pr`).

If the user's request makes the type and a short kebab-case description obvious, use them directly.
Otherwise ask the user.

### 3. Sync with origin

```bash
git remote update origin
```

### 4. Create the branch

#### Worktree mode

Check for collisions first:

```bash
git worktree list
git branch --list <type>/<short-description>
ls .claude/worktrees/<short-description>
```

If the branch already exists, the worktree path is already registered, or the directory already
exists on disk (even unregistered, e.g. a leftover from a worktree removed without
`git worktree remove`), stop and tell the user instead of overwriting anything.

```bash
git worktree add -b <type>/<short-description> .claude/worktrees/<short-description> origin/main
```

This creates the branch and checks it out in the new worktree in one step, based on the latest
`origin/main` fetched in step 3, not on whatever the main checkout currently has checked out.

Then copy the git-ignored local config into the new worktree, since a fresh worktree only contains
tracked files. Copy each one that exists in the main checkout:

```bash
WT=.claude/worktrees/<short-description>
cp local.properties "$WT/local.properties"
mkdir -p "$WT/.claude" && cp .claude/settings.local.json "$WT/.claude/settings.local.json"
```

- `local.properties` has `sdk.dir`; without it Gradle can't find the Android SDK and every build
  fails.
- `.claude/settings.local.json` gives the worktree's Claude Code session the same permission
  settings instead of re-prompting for everything.

If one of them doesn't exist in the main checkout, skip it and tell the user which one is missing.

Two more pieces are git-ignored and generated, so they are created fresh in the worktree rather
than copied. Do each one now if the task touches that area, or the first time the task needs it:

- **Apple** (`iosApp/`, or anything SwiftUI sees through the `Shared` framework): generate
  `iosApp/BoraRachar.xcodeproj` and `iosApp/Generated/` from `iosApp/project.yml`. It needs
  XcodeGen (`brew install xcodegen`; ask the user before installing it).

  ```bash
  "$WT/iosApp/generate-project.sh"
  ```

- **Web** (`webApp/`, or anything React sees through the JS library of `:shared`): build the JS
  library `webApp/package.json` points at (`blitzsplit-shared`, a `file:` dependency on
  `shared/build/dist/js/productionLibrary`), then install `webApp/node_modules`.

  ```bash
  npm --prefix "$WT/webApp" run shared
  npm --prefix "$WT/webApp" install
  ```

Do **not** copy git-ignored build outputs, caches or generated files from the main checkout
(`.gradle/`, `**/build/`, `.kotlin/`, `.idea/`, `kotlin-js-store/`, `iosApp/BoraRachar.xcodeproj`,
`iosApp/Generated/`, `webApp/node_modules`, `webApp/dist`, `.claude/memory/`, `.claude/worktrees/`).
They are regenerated fresh per worktree; copying them carries over stale state, such as an Xcode
project generated from an older `project.yml`.

#### In-place mode

Check the current checkout is clean and not already mid-task:

```bash
git status -s
git branch --show-current
```

If there are uncommitted changes, stop and ask the user how to proceed (commit, stash, or use
worktree mode instead). Don't discard or stash anything without asking. If the current branch
isn't `main` (or doesn't track `origin/main`), tell the user and confirm before switching away from
it, since that would leave whatever they're on now behind.

```bash
git checkout -b <type>/<short-description> origin/main
```

No file copying is needed here: the checkout already has its local git-ignored config in place.
If the task touches Apple and `iosApp/BoraRachar.xcodeproj` is missing, run
`./iosApp/generate-project.sh`; if it touches the web and `webApp/node_modules` is missing, run
`npm --prefix webApp run shared` and `npm --prefix webApp install`.

### 5. Create the task's devices, if the task needs them

Decide from the user's request (and anything else known about the task) whether it will need a
device, and which one. Don't ask the user: infer it.

Create an **Android emulator** when the task will need a look at the running Android app:

- a visual or layout change in `android/` or `androidApp/` that a `@DevicePreviews` preview can't
  settle (insets, the keyboard, rotation, the tablet layouts, edge-to-edge);
- something only a real Android runtime does: Google sign in through Credential Manager, saving the
  recovery code to the password manager, the QR scanner, the `borarachar://auth-callback` deep link;
- a repro of an Android-only bug or crash.

Create an **iOS simulator** when the task will need a look at the running iPhone or iPad app:

- a visual or layout change in `iosApp/Sources/Shared` or `iosApp/Sources/iOS` (the Liquid Glass
  tab bar, the iPad sidebar, the two-column layouts, sheets);
- `onOpenURL` with `borarachar://auth-callback`, the QR scanner, saving to the Passwords app;
- a repro of an iOS-only bug or crash.

Don't create either when the task is docs, skills, Gradle or the version catalog, ktlint, the
backend (`blitz-split-api`), the web app (it runs in the browser pane), the macOS app (it runs on
this Mac), or shared Kotlin that `jvmTest` verifies: use cases, repositories, data sources,
mappers, `SyncCoordinator`, ViewModels. Compiling doesn't need a device either: the `xcodebuild`
checks build for `generic/platform=iOS Simulator`, and `:androidApp:assembleDebug` needs no
emulator.

When in doubt, don't create it: it can be created later at no extra cost, while an unneeded one
lingers on disk until `finish-task`.

If a task started without a device turns out to need one later, create it then, the same way,
before booting anything. Never fall back to the user's emulators and simulators or another task's.

See [Task devices](#task-devices) below for how to create, boot, use and shut down each one.

### 6. Report the result

Worktree mode: tell the user the absolute path to the new worktree and the branch name, and that
it's ready to open (new Claude Code session, Android Studio via "Open" on that path, or Xcode on
`iosApp/BoraRachar.xcodeproj` inside it once generated).

In-place mode: confirm the branch name and that the current checkout is now on it, ready to work.

In both modes, also name the task's emulator or simulator, or say that none was created and why, in
one line.

### 7. Start implementing immediately

If the user's request already described the task to do (bug to fix, feature to add, etc.), not
just which branch/mode to use, don't stop after step 6 and wait for a "go ahead"/"pode
implementar". Continue straight into implementing that task in this same turn, right after the
short status report from step 6.

- Worktree mode: keep working in this same session; do not wait for a new session to be opened
  in the worktree. Point every file tool (Read/Edit/Write/Bash/etc.) at paths inside the new
  worktree directory (`.claude/worktrees/<short-description>/...`) instead of the main checkout,
  since tools work with any path regardless of the session's current working directory. A `cd`
  doesn't persist between Bash calls, so a bare `./gradlew`, `git`, `npm --prefix webApp` or
  `xcodebuild -project iosApp/...` builds and inspects the main checkout: always use
  `./gradlew -p .claude/worktrees/<short-description>`, `git -C .claude/worktrees/<short-description>`,
  `npm --prefix .claude/worktrees/<short-description>/webApp` and
  `xcodebuild -project .claude/worktrees/<short-description>/iosApp/BoraRachar.xcodeproj`.
- In-place mode: the current checkout is already on the new branch, so just continue normally.

A change that adds or changes a `UiState`, `UiEvent` or `UiAction` in `feature/*` usually has to
reach every UI that draws it: the Compose screen in `android/feature/*`, the SwiftUI view in
`iosApp/Sources/Shared/Features/*`, and the React screen in `webApp/src/features/*` through its
`*Presenter` in `shared/src/jsMain`. Follow
[docs/architecture/new-feature-checklist.md](../../../docs/architecture/new-feature-checklist.md)
for which pieces a change touches.

Only stop and wait for the user if their request genuinely gave no task description (e.g. they
only said "start a task in a worktree" with no further detail); in that case, ask what to
implement.

## Task devices

Several sessions run at once, and every emulator and simulator on this Mac is visible to all of
them. An `adb install` or a `simctl` command aimed at "the" device lands on whichever one is running,
possibly one another session is using, and the user's own devices (`Medium_Phone`, `Pixel_9`,
`Pixel_Tablet`, the stock iPhone and iPad simulators) are theirs to look at. So a task that needs a
device gets its own.

### Name

`BoraRachar_<short_description>`: the task's short description from step 2, with `-` turned into
`_`. For `feature/split-by-shares`, that is `BoraRachar_split_by_shares`. The Android emulator and
the iOS simulator of a task share the name. The name follows from the branch, so `finish-task`
finds them without anything written down.

### Android emulator

**Create.** Copy `Pixel_9`'s config, an API 36 phone with Google Play (Credential Manager needs
it). For a task about the tablet layouts, copy `Pixel_Tablet` instead. If
`~/Library/Android/sdk/emulator/emulator -list-avds` doesn't list the source, ask the user which
AVD to copy.

```bash
AVD=BoraRachar_<short_description>
SOURCE=Pixel_9
AVD_HOME=~/.android/avd
ls -d "$AVD_HOME/$AVD.avd" "$AVD_HOME/$AVD.ini" 2>/dev/null
```

If either path already exists, stop and ask, the way step 4 does for a branch that already exists.
It may belong to a task that is still running, so don't reuse it or overwrite it. Otherwise:

```bash
mkdir "$AVD_HOME/$AVD.avd"
cp "$AVD_HOME/$SOURCE.avd/config.ini" "$AVD_HOME/$AVD.avd/config.ini"
sed -i '' "s/^AvdId=.*/AvdId=$AVD/; s/^avd.ini.displayname=.*/avd.ini.displayname=$AVD/" \
  "$AVD_HOME/$AVD.avd/config.ini"
printf 'avd.ini.encoding=UTF-8\npath=%s\npath.rel=avd/%s.avd\ntarget=%s\n' \
  "$AVD_HOME/$AVD.avd" "$AVD" "$(sed -n 's/^target=//p' "$AVD_HOME/$SOURCE.ini")" \
  > "$AVD_HOME/$AVD.ini"
```

Only `config.ini` is copied, so no disk image is duplicated and the emulator only takes space once
it boots. Copying the source's `userdata` or `snapshots` would carry over its apps, its data and its
rotation.

**Boot** it only when the task needs the device. Give it a console port outside the range adb scans
on its own (5554–5585), and an adb server of its own:

```bash
CONSOLE_PORT=<an even port from 5700 to 5798 that `lsof -iTCP:<port>` shows free>
export ANDROID_ADB_SERVER_PORT=$((CONSOLE_PORT + 1000))
nohup ~/Library/Android/sdk/emulator/emulator -avd "$AVD" -port "$CONSOLE_PORT" \
  -no-window -no-snapshot -no-audio > /dev/null 2>&1 &
until [ "$(adb -s "emulator-$CONSOLE_PORT" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 5; done
```

The loop is what waits for the boot: `adb -s <serial> wait-for-device` fails at once with "device
not found" while the emulator hasn't registered with the adb server yet. The default adb server,
the one Gradle and Android Studio use, never sees this emulator.

Always pass `-s emulator-<port>`: even a private adb server finds the user's emulators on the usual
ports. A shell variable doesn't survive from one Bash call to the next, so repeat the `export` (and
the variables) in every call that runs `adb`. Drop `-no-window` when the user wants to watch.

**Run the app on it.** Run configurations and Gradle's `installDebug` pick a device on their own,
so install the APK directly (in worktree mode, `./gradlew -p <worktree>` and the worktree's APK
path, or you install the main checkout's build):

```bash
./gradlew :androidApp:assembleDebug
adb -s "emulator-$CONSOLE_PORT" install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s "emulator-$CONSOLE_PORT" shell am start -n com.quare.blitzsplit/.MainActivity
adb -s "emulator-$CONSOLE_PORT" exec-out screencap -p > <scratchpad>/screen.png
```

- The deep link of the browser sign in:
  `adb -s "emulator-$CONSOLE_PORT" shell am start -a android.intent.action.VIEW -d 'borarachar://auth-callback'`.
- Start over from the welcome screen (Room is the source of truth, so the old profile stays until
  the data is cleared): `adb -s "emulator-$CONSOLE_PORT" shell pm clear com.quare.blitzsplit`.
- Offline: `adb -s "emulator-$CONSOLE_PORT" shell svc wifi disable` and `svc data disable`
  (`enable` to bring them back).
- A landscape check only stays in landscape with the rotation locked:

  ```bash
  adb -s "emulator-$CONSOLE_PORT" shell cmd window user-rotation lock 1   # 0 for portrait
  adb -s "emulator-$CONSOLE_PORT" shell cmd window fixed-to-user-rotation enabled
  ```

**Shut it down** as soon as the device work is done (the look at the app is over), before reporting
back, not at the end of the task. A running emulator holds its 2 GB of guest RAM plus the
emulator's own, and with several sessions at once that adds up fast. Stop its private adb server
too:

```bash
export ANDROID_ADB_SERVER_PORT=$((CONSOLE_PORT + 1000))
adb -s "emulator-$CONSOLE_PORT" emu kill
while pgrep -f -- "-avd $AVD( |$)" > /dev/null; do sleep 1; done
adb kill-server
```

The AVD stays, so the task can boot it again later (cold, since `-no-snapshot`). `finish-task`
deletes it.

### iOS simulator

**Create.** An iPhone 17 Pro on the newest installed iOS runtime, or an iPad Pro 11-inch (M5) for a
task about the iPad layouts. If `xcrun simctl list devicetypes` doesn't list the device type, ask
the user which one to use.

```bash
SIM=BoraRachar_<short_description>
xcrun simctl list devices | grep -F "$SIM ("
```

If it prints anything, a simulator of that name already exists: stop and ask, as for the emulator.
Otherwise:

```bash
xcrun simctl create "$SIM" "iPhone 17 Pro"
```

It prints the new simulator's UDID. A simulator takes little space until it boots, and none of the
user's simulators is touched.

**Boot** it only when the task needs the device, and address it by UDID in every command. Never use
the `booted` alias: it picks any booted simulator, possibly another session's.

```bash
UDID=$(xcrun simctl list devices | sed -nE "s/^ +$SIM \(([0-9A-F-]{36})\).*/\1/p")
xcrun simctl boot "$UDID"
xcrun simctl bootstatus "$UDID" -b
```

It boots headless. When the user wants to watch, open the iOS Simulator panel on this device
(`device: BoraRachar_<short_description>`) or `open -a Simulator`.

**Run the app on it.** Generate the Xcode project first if this checkout doesn't have it yet
(`./iosApp/generate-project.sh`). The build phase of the project builds the `Shared` framework with
Gradle, so the first build takes a few minutes. In worktree mode, use the worktree's project and
derived data path.

```bash
xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme BoraRachar-iOS \
  -destination "id=$UDID" -derivedDataPath iosApp/build/DerivedData build -quiet
xcrun simctl install "$UDID" iosApp/build/DerivedData/Build/Products/Debug-iphonesimulator/BoraRachar.app
xcrun simctl launch "$UDID" com.quare.blitzsplit
xcrun simctl io "$UDID" screenshot <scratchpad>/screen.png
```

- The deep link of the browser sign in: `xcrun simctl openurl "$UDID" 'borarachar://auth-callback'`.
- Start over from the welcome screen: `xcrun simctl uninstall "$UDID" com.quare.blitzsplit`, then
  install again.

**Shut it down** as soon as the device work is done, before reporting back:

```bash
xcrun simctl shutdown "$UDID"
```

The simulator stays, so the task can boot it again later. `finish-task` deletes it.

### macOS and the web

Neither needs a device of its own. The macOS app runs on this Mac: build the `BoraRachar-macOS`
scheme as `create-pr` does and `open iosApp/build/DerivedData/Build/Products/Debug/BoraRachar.app`
only when the user wants to see it. The web app runs in the browser pane: the `web` configuration of
`.claude/launch.json` serves the main checkout on port 5173. In worktree mode, start the worktree's
own dev server on a free port instead (`npm --prefix <worktree>/webApp run dev -- --port <port>
--strictPort`, in the background) and stop it once the look is over.

## Edge cases

- If `git worktree add` fails because the branch or path already exists, stop; don't force or
  pick a different name silently. Ask the user how they want to resolve it.
- Never run worktree mode from inside another worktree; it should always target
  `.claude/worktrees/<short-description>` relative to the main repo root.
- In in-place mode, if `git checkout -b` fails because the branch name already exists, stop and ask
  the user how they want to resolve it.
- If the task's emulator or simulator already exists (an AVD of that name or its `.avd` directory,
  or a simulator of that name), stop and ask. It may belong to a task that is still running, so
  don't reuse it or overwrite it.
- If `generate-project.sh` fails because XcodeGen is missing, or `npm install` fails, tell the user
  and go on with the parts of the task that don't need them; don't install tools without asking.
