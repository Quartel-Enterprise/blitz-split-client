#!/usr/bin/env python3
"""Fills each regional table of the built app (es-ES.lproj, pt-PT.lproj, en-GB.lproj, en-AU.lproj, en-CA.lproj) with
its language's table (es.lproj, pt-BR.lproj, en.lproj), the region's own keys winning.

Localizable.xcstrings holds Spain's es-ES and Portugal's pt-PT only for the words they say differently
(docs/product/general.md), and Xcode compiles es-ES.lproj/Localizable.strings with only those. A bundle whose language
is es-ES then reads every table from es-ES.lproj alone: the string lookup never goes on to es.lproj for a key the table
lacks, so the rest shows the English source ("New group" next to "Añadir por nombre"). Once merged, each es-ES table
holds every es key, so the catalog keeps no copy of them.

The region's own keys are the ones the catalog gives that region, not every key of its built table: Xcode also
writes there, untranslated, each key it is told to keep (extractionState "manual") whose text takes arguments, with
the key as its value. Left to win, those showed "You owe %@ (a member)" and "push.bill_deleted" in Spain where es
has the sentence. So the catalogs are read too, and a key of the built table the catalog has no es-ES for takes es's.

Xcode runs it after Copy Bundle Resources, before the bundle is signed, on the app's resources folder and the folder
of its catalogs:
  merge-regional-strings.py <TARGET_BUILD_DIR>/<UNLOCALIZED_RESOURCES_FOLDER_PATH> <SRCROOT>/Resources

It merges every table of the language's lproj: the .strings (Localizable, InfoPlist) and the .stringsdict of the
plurals. A key the region gives as a plain string drops the language's plural of that key, since the lookup reads the
.stringsdict first. Running it again on merged tables changes nothing.
"""

import functools
import json
import plistlib
import re
import subprocess
import sys
from pathlib import Path
from typing import Optional

# es-ES, es_ES, pt-PT, es-419, zh-Hant-TW: a language (and script) then a region of two letters or three digits
REGIONAL = re.compile(r"^(?P<language>.+)[-_](?:[A-Z]{2}|\d{3})$")
# The table a regional one is filled from, when the language's own is not named by the language alone: the app's
# Portuguese is pt-BR (Brazil's), the one Portugal's pt-PT overrides
BASE_OF = {"pt-PT": "pt-BR"}
TABLE_SUFFIXES = (".strings", ".stringsdict")


def read_table(path: Path) -> dict:
    # plutil reads every format a .strings may come in: binary, XML or the old text one. Its stderr stays in the build
    # log, so a table it can't read says why
    xml = subprocess.run(
        ["/usr/bin/plutil", "-convert", "xml1", "-o", "-", str(path)], check=True, stdout=subprocess.PIPE
    )
    return plistlib.loads(xml.stdout)


def read_tables(lproj: Path) -> dict:
    return {
        table.name: read_table(table)
        for table in sorted(lproj.iterdir())
        if table.is_file() and table.suffix in TABLE_SUFFIXES
    }


@functools.cache
def read_catalog(catalog: Path) -> Optional[dict]:
    """The catalog's strings, read once however many tables and regions ask; None when there is no such catalog."""
    if not catalog.is_file():
        return None
    return json.loads(catalog.read_text(encoding="utf-8")).get("strings", {})


@functools.cache
def read_translated_keys(catalog: Path, region: str) -> Optional[frozenset]:
    """The keys the catalog translates into `region`, once for its .strings and .stringsdict; None for no catalog."""
    strings = read_catalog(catalog)
    if strings is None:
        return None
    return frozenset(key for key, entry in strings.items() if region in entry.get("localizations", {}))


def own_entries(table: dict, translated_keys: Optional[frozenset]) -> dict:
    if translated_keys is None:
        return table
    return {key: value for key, value in table.items() if key in translated_keys}


def merge(language_lproj: Path, regional_lproj: Path, catalogs: Path) -> None:
    language_tables = read_tables(language_lproj)
    built_tables = read_tables(regional_lproj)
    # The catalogs name a region with a hyphen (es-ES), whichever separator its lproj took
    region = regional_lproj.stem.replace("_", "-")
    regional_tables = {
        name: own_entries(table, read_translated_keys(catalogs / f"{Path(name).stem}.xcstrings", region))
        for name, table in built_tables.items()
    }
    for name, language_table in language_tables.items():
        regional_table = regional_tables.get(name, {})
        inherited = dict(language_table)
        if name.endswith(".stringsdict"):
            plain_twin = regional_tables.get(name.removesuffix(".stringsdict") + ".strings", {})
            for key in plain_twin:
                inherited.pop(key, None)
        merged = inherited | regional_table
        if merged == built_tables.get(name, {}):
            continue
        (regional_lproj / name).write_bytes(plistlib.dumps(merged, fmt=plistlib.FMT_BINARY))
        print(
            f"{regional_lproj.name}/{name}: {len(regional_table)} keys of its own, "
            f"{len(merged) - len(regional_table)} from {language_lproj.name}"
        )


def main(resources_dir: str, catalogs_dir: str) -> int:
    resources = Path(resources_dir)
    catalogs = Path(catalogs_dir)
    for folder in (resources, catalogs):
        if not folder.is_dir():
            print(f"error: {folder} is not a folder")
            return 1
    for regional_lproj in sorted(resources.glob("*.lproj")):
        match = REGIONAL.match(regional_lproj.stem)
        if not match:
            continue
        language_lproj = resources / f"{BASE_OF.get(regional_lproj.stem, match['language'])}.lproj"
        if language_lproj.is_dir():
            merge(language_lproj, regional_lproj, catalogs)
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("error: usage: merge-regional-strings.py <the app's resources folder> <the catalogs' folder>")
        sys.exit(1)
    sys.exit(main(sys.argv[1], sys.argv[2]))
