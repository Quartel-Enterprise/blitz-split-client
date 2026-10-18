#!/bin/bash

# Generates BoraRachar.xcodeproj from project.yml. The app version comes from ../version.properties,
# the same file Gradle reads, so every platform ships the same versionName and versionCode.
#   ./iosApp/generate-project.sh

set -e

DIR="$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" > /dev/null && pwd)"
PROPERTIES="$DIR/../version.properties"

read_property() {
    sed -nE "s/^$1[[:space:]]*=[[:space:]]*(.*)$/\1/p" "$PROPERTIES" | head -1
}

if ! command -v xcodegen > /dev/null; then
    echo "❌ XcodeGen is missing: brew install xcodegen"
    exit 1
fi

BLITZSPLIT_VERSION_NAME="$(read_property versionName)"
BLITZSPLIT_VERSION_CODE="$(read_property versionCode)"
export BLITZSPLIT_VERSION_NAME BLITZSPLIT_VERSION_CODE

xcodegen generate --spec "$DIR/project.yml" --project "$DIR"
