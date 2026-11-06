#!/usr/bin/env python3
"""Fills each regional table of the built app (es-ES.lproj) with its language's table (es.lproj), the region's own
keys winning.

Localizable.xcstrings holds Spain's es-ES only for the words Spain says differently (docs/product/general.md), and
Xcode compiles es-ES.lproj/Localizable.strings with only those. A bundle whose language is es-ES then reads every
table from es-ES.lproj alone: the string lookup never goes on to es.lproj for a key the table lacks, so the rest shows
the English source ("New group" next to "Añadir por nombre"). Once merged, each es-ES table holds every es key, so the
catalog keeps no copy of them.

Xcode runs it after Copy Bundle Resources, before the bundle is signed, on the app's resources folder:
  merge-regional-strings.py <TARGET_BUILD_DIR>/<UNLOCALIZED_RESOURCES_FOLDER_PATH>

It merges every table of the language's lproj: the .strings (Localizable, InfoPlist) and the .stringsdict of the
plurals. A key the region gives as a plain string drops the language's plural of that key, since the lookup reads the
.stringsdict first. Running it again on merged tables changes nothing.
"""

import plistlib
import re
import subprocess
import sys
from pathlib import Path

# es-ES, es_ES, pt-PT, es-419, zh-Hant-TW: a language (and script) then a region of two letters or three digits
REGIONAL = re.compile(r"^(?P<language>.+)[-_](?:[A-Z]{2}|\d{3})$")
TABLE_SUFFIXES = (".strings", ".stringsdict")


def read_table(path: Path) -> dict:
    # plutil reads every format a .strings may come in: binary, XML or the old text one
    xml = subprocess.run(["/usr/bin/plutil", "-convert", "xml1", "-o", "-", str(path)], check=True, capture_output=True)
    return plistlib.loads(xml.stdout)


def read_tables(lproj: Path) -> dict:
    return {
        table.name: read_table(table)
        for table in sorted(lproj.iterdir())
        if table.is_file() and table.suffix in TABLE_SUFFIXES
    }


def merge(language_lproj: Path, regional_lproj: Path) -> None:
    language_tables = read_tables(language_lproj)
    regional_tables = read_tables(regional_lproj)
    for name, language_table in language_tables.items():
        regional_table = regional_tables.get(name, {})
        inherited = dict(language_table)
        if name.endswith(".stringsdict"):
            plain_twin = regional_tables.get(name.removesuffix(".stringsdict") + ".strings", {})
            for key in plain_twin:
                inherited.pop(key, None)
        merged = inherited | regional_table
        if merged == regional_table:
            continue
        (regional_lproj / name).write_bytes(plistlib.dumps(merged, fmt=plistlib.FMT_BINARY))
        print(
            f"{regional_lproj.name}/{name}: {len(regional_table)} keys of its own, "
            f"{len(merged) - len(regional_table)} from {language_lproj.name}"
        )


def main(resources_dir: str) -> int:
    resources = Path(resources_dir)
    if not resources.is_dir():
        print(f"error: {resources} is not a folder")
        return 1
    for regional_lproj in sorted(resources.glob("*.lproj")):
        match = REGIONAL.match(regional_lproj.stem)
        if not match:
            continue
        language_lproj = resources / f"{match['language']}.lproj"
        if language_lproj.is_dir():
            merge(language_lproj, regional_lproj)
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("error: usage: merge-regional-strings.py <the app's resources folder>")
        sys.exit(1)
    sys.exit(main(sys.argv[1]))
