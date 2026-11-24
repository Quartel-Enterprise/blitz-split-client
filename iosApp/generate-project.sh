#!/bin/bash

# Generates BoraRachar.xcodeproj from project.yml. The app version comes from ../version.properties,
# the same file Gradle reads, so every platform ships the same versionName and versionCode.
#   ./iosApp/generate-project.sh             # on this Mac, the iPhone app without the Apple Watch app
#   ./iosApp/generate-project.sh --watch     # the iPhone app with the Apple Watch app inside, as CI and a release build it
#   ./iosApp/generate-project.sh --no-watch  # without it, on CI too
#
# Why the watch app is left out here: embedded, every build of the iPhone app builds it too, and links a second Shared
# framework for it. Kotlin/Native keeps no compiler cache for the watchOS simulator, so that link takes about 50
# seconds after any Kotlin change, where the iPhone's takes 30 (an M4 Max, November 2026), for an app a simulator
# run of the iPhone app never opens. CI still builds it with every pull request, a release can't be built without it
# (the first lines of its "Build the Shared framework" phase), and the BoraRachar-watchOS scheme builds and runs it alone
# either way.

set -e

# CI sets CI=true: its builds are the ones that check the watch app still builds and the ones that ship it.
EMBED_WATCH=""
case "${CI:-}" in true | 1) EMBED_WATCH=true ;; esac
for argument in "$@"; do
    case "$argument" in
        --watch) EMBED_WATCH=true ;;
        --no-watch) EMBED_WATCH="" ;;
        *)
            echo "usage: $0 [--watch | --no-watch]" >&2
            exit 2
            ;;
    esac
done

DIR="$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" > /dev/null && pwd)"
PROPERTIES="$DIR/../version.properties"

read_property() {
    sed -nE "s/^$1[[:space:]]*=[[:space:]]*(.*)$/\1/p" "$PROPERTIES" | head -1
}

if ! command -v xcodegen > /dev/null; then
    echo "❌ XcodeGen is missing: brew install xcodegen"
    exit 1
fi

# The whole versionName, a beta's suffix too ("3.0.0-beta_7"), is what Profile shows (BlitzSplitVersionName); Apple
# refuses a suffix in the version string, so MARKETING_VERSION takes the plain X.Y.Z.
BLITZSPLIT_DISPLAY_VERSION="$(read_property versionName)"
BLITZSPLIT_VERSION_NAME="${BLITZSPLIT_DISPLAY_VERSION%%-*}"
BLITZSPLIT_VERSION_CODE="$(read_property versionCode)"
# What project.yml's include of watch-embed.yml reads.
BLITZSPLIT_EMBED_WATCH="${EMBED_WATCH:-false}"
export BLITZSPLIT_DISPLAY_VERSION BLITZSPLIT_VERSION_NAME BLITZSPLIT_VERSION_CODE BLITZSPLIT_EMBED_WATCH

if [ "$BLITZSPLIT_EMBED_WATCH" = true ]; then
    echo "The iPhone app embeds the Apple Watch app"
else
    echo "The iPhone app leaves the Apple Watch app out: --watch embeds it"
fi

xcodegen generate --spec "$DIR/project.yml" --project "$DIR"
