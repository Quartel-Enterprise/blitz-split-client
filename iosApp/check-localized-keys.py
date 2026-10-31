#!/usr/bin/env python3
"""Fails the build on a localized key with a %d or %f specifier, which an Int32 or a Float interpolation makes (Swift's
Int and Double make %lld and %lf).

Swift sees a Kotlin Int as Int32, and "\\(count) members" then gets the key "%d members". Localizable.xcstrings holds
the %lld key a Swift Int makes, so the lookup misses: no pt-BR and no plural variants, only the English literal.
Interpolate Int(count) instead. A Kotlin Float is the same: a Swift Float makes %f where a Double makes %lf.

Xcode runs it after Compile Sources, on the .stringsdata the compiler extracts the keys into (SWIFT_EMIT_LOC_STRINGS):
  check-localized-keys.py <OBJECT_FILE_DIR_normal>
"""

import json
import os
import re
import sys
from pathlib import Path

# %d, %5$d, %hd, %ld... but not %lld; a bare %f, not %lf nor the %.2f of an explicit specifier. A literal %% is
# dropped before the search.
NARROW_SPECIFIER = re.compile(r"%(\d+\$)?((hh|h|l)?[diu]|f)")


def main(objects_dir: str) -> int:
    failures = 0
    all_strings_data = sorted(Path(objects_dir).rglob("*.stringsdata"))
    if not all_strings_data:
        print(f"warning: no .stringsdata under {objects_dir}, so no localized key was checked")
    for strings_data in all_strings_data:
        data = json.loads(strings_data.read_text(encoding="utf-8"))
        source = data.get("source", str(strings_data))
        # A file deleted or renamed since leaves its .stringsdata behind, and so does an architecture this build
        # doesn't compile: older than its source, it holds keys the source no longer has
        if not os.path.exists(source) or strings_data.stat().st_mtime < os.path.getmtime(source):
            continue
        for entries in data.get("tables", {}).values():
            for entry in entries:
                key = entry["key"]
                if NARROW_SPECIFIER.search(key.replace("%%", "")):
                    line = entry.get("location", {}).get("startingLine", 1)
                    print(
                        f'{source}:{line}: error: the localized key "{key}" has a %d or %f specifier, so it '
                        "misses its %lld or %lf entry in Localizable.xcstrings: interpolate Int(...) for a Kotlin "
                        "Int, Double(...) for a Kotlin Float"
                    )
                    failures += 1
    return 1 if failures else 0


if __name__ == "__main__":
    if len(sys.argv) != 2 or not sys.argv[1]:
        print("error: usage: check-localized-keys.py <OBJECT_FILE_DIR_normal>")
        sys.exit(1)
    sys.exit(main(sys.argv[1]))
