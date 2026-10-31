#!/usr/bin/env python3
"""Fails the build on a localized key with a %d, %i or %u specifier, which only a 32-bit integer interpolation makes.

Swift sees a Kotlin Int as Int32, and "\\(count) members" then gets the key "%d members". Localizable.xcstrings holds
the %lld key a Swift Int makes, so the lookup misses: no pt-BR and no plural variants, only the English literal.
Interpolate Int(count) instead.

Xcode runs it after Compile Sources, on the .stringsdata the compiler extracts the keys into (SWIFT_EMIT_LOC_STRINGS):
  check-localized-keys.py <OBJECT_FILE_DIR_normal>
"""

import json
import os
import re
import sys
from pathlib import Path

# %d, %5$d, %hd, %ld... but not %lld. A literal %% is dropped before the search.
INT32_SPECIFIER = re.compile(r"%(\d+\$)?(hh|h|l)?[diu]")


def main(objects_dir: str) -> int:
    failures = 0
    for strings_data in sorted(Path(objects_dir).rglob("*.stringsdata")):
        data = json.loads(strings_data.read_text())
        source = data.get("source", str(strings_data))
        # A file deleted or renamed since leaves its .stringsdata behind
        if not os.path.exists(source):
            continue
        for entries in data.get("tables", {}).values():
            for entry in entries:
                key = entry["key"]
                if INT32_SPECIFIER.search(key.replace("%%", "")):
                    line = entry.get("location", {}).get("startingLine", 1)
                    print(
                        f'{source}:{line}: error: the localized key "{key}" has a 32-bit specifier, so it misses '
                        "its %lld entry in Localizable.xcstrings: interpolate Int(...) for a Kotlin Int"
                    )
                    failures += 1
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
