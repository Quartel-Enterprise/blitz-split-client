#!/bin/bash

# Looks for copied code in the Kotlin, Swift and TypeScript of the apps with jscpd (github.com/kucherenko/jscpd):
#   ./scripts/cpd.sh                                check: fails on copied code .jscpd-baseline.json doesn't list,
#                                                   and on copied code it still lists after it was removed
#   ./scripts/cpd.sh --update-baseline              take the removed copied code out of .jscpd-baseline.json
#   ./scripts/cpd.sh --update-baseline --allow-new  also take new copied code in, only when the copy is on purpose
#
# What it scans is in .jscpd.json. jscpd only compares files of the same language, so a screen drawn by Compose,
# SwiftUI and React is never a clone: what it finds is logic or UI copied within one of them.

set -e

# An exact release, like every tool this repository runs
JSCPD_VERSION="5.4.2"

ROOT="$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." > /dev/null && pwd)"
cd "$ROOT"

if ! command -v node > /dev/null; then
    echo "❌ jscpd needs Node.js 18 or later (see docs/getting-started.md)"
    exit 1
fi

rm -rf build/jscpd
npx --yes "jscpd@$JSCPD_VERSION" --config .jscpd.json

exec node scripts/cpd-check.mjs build/jscpd/jscpd-report.json .jscpd-baseline.json "$@"
