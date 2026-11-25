#!/usr/bin/env python3
"""Fails on an Android string a language lacks, has extra, or formats differently from English.

`lintDebug` already fails on most of these (MissingTranslation, ExtraTranslation, StringFormatMatches), but only after
compiling every Android module, minutes into build-and-test. This one is stricter on the format arguments: a dropped
argument (only lint's StringFormatCount warning) and a different conversion (%d for %s) fail here. This reads the XML alone, in a second, so the translations job of CI
says it first, and a pull request that only touches strings learns it without waiting for a Gradle build.

For every Android module with a `src/main/res/values/strings.xml` (English, the source):
- every full language (values-pt, values-b+zh+Hans, ...) the project has must exist in the module and hold every
  translatable <string> and <plurals> of English, and no name English doesn't have;
- a regional one that holds only the words its region says differently (values-pt-rPT, values-es-rES, values-en-rCA,
  values-b+en+001) may lack names, but never has an extra one;
- every value English formats takes the same arguments (%s, %1$s, %d, ...) by position, as Java's Formatter reads
  them: "%1$s" and the first "%s" are the same one, and a translation may repeat or reorder them. A plural's items
  may take any argument one of English's items takes.

  python3 -I scripts/check-android-strings.py [repository root]
"""

import os
import re
import sys
from pathlib import Path
from xml.etree import ElementTree

# The regional variants that hold only the words their region says differently (see docs/ai_agents.md). Any other
# language directory, a regional one included (values-zh-rTW), is a full language.
PARTIAL = {"values-pt-rPT", "values-es-rES", "values-en-rCA", "values-en-rGB", "values-en-rAU", "values-b+en+001"}
# A language directory; values-night, values-v31, values-land and the like are not languages.
LANGUAGE = re.compile(r"^values-(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\+[a-z]{2,3}(?:\+[A-Za-z0-9]+)*)$")
NOT_LANGUAGE = {"values-car", "values-tv", "values-vr", "values-ldrtl", "values-ldltr", "values-port", "values-land"}
# %s, %1$s, %d, %.2f, % d (the space flag); %% is a literal percent and no argument.
ARGUMENT = re.compile(r"%(?:(\d+)\$)?[-+0#,( ]*\d*(?:\.\d+)?([sSdfxXoeEgGcbBhH])")
TRANSLATABLE_FALSE = "false"


def arguments(text: str) -> dict:
    """The format arguments of `text` by position, as Java's Formatter reads them: "%1$s" is the first argument
    wherever it appears, and "%s" the next one in order. A translation may repeat or reorder them."""
    taken = {}
    following = 0
    for match in ARGUMENT.finditer(text.replace("%%", "")):
        if match.group(1):
            position = int(match.group(1))
        else:
            following += 1
            position = following
        taken[position] = match.group(2).lower()
    return taken


def read(path: Path) -> dict:
    """Every string and plurals of `path` by name, as the format arguments of each value, skipping the ones
    marked translatable="false"."""
    entries = {}
    for element in ElementTree.parse(path).getroot():
        name = element.get("name")
        if name is None or element.get("translatable") == TRANSLATABLE_FALSE:
            continue
        if element.tag == "string":
            entries[("string", name)] = [arguments("".join(element.itertext()))]
        elif element.tag == "plurals":
            entries[("plurals", name)] = [arguments("".join(item.itertext())) for item in element.iter("item")]
    return entries


def check_module(res: Path, languages: set, root: Path) -> list:
    errors = []
    english = read(res / "values" / "strings.xml")
    present = {directory.name for directory in res.iterdir() if (directory / "strings.xml").is_file()}
    for language in sorted(languages - present):
        errors.append(f"{res.relative_to(root)}: no {language}/strings.xml, which another module has")
    for language in sorted(present & (languages | PARTIAL)):
        path = res / language / "strings.xml"
        shown = path.relative_to(root)
        translated = read(path)
        if language not in PARTIAL:
            for kind, name in sorted(english.keys() - translated.keys()):
                errors.append(f'{shown}: missing <{kind} name="{name}">')
        for kind, name in sorted(translated.keys() - english.keys()):
            errors.append(f"{shown}: <{kind} name=\"{name}\"> isn't in English's strings.xml")
        for key in sorted(english.keys() & translated.keys()):
            expected = english[key]
            if key[0] == "string":
                # A string English formats with no argument is never formatted, so its "%" is text, as lint reads it.
                if expected[0] and translated[key][0] != expected[0]:
                    errors.append(
                        f'{shown}: <string name="{key[1]}"> takes {format_of(translated[key][0])}'
                        f" where English takes {format_of(expected[0])}"
                    )
            else:
                allowed = {position: conversion for item in expected for position, conversion in item.items()}
                for item in translated[key]:
                    extra = {
                        position: conversion
                        for position, conversion in item.items()
                        if allowed.get(position) != conversion
                    }
                    if allowed and extra:
                        errors.append(
                            f'{shown}: <plurals name="{key[1]}"> takes {format_of(extra)}, which no English item takes'
                        )
                        break
    return errors


def format_of(taken: dict) -> str:
    if not taken:
        return "no argument"
    return " ".join(f"%{position}${conversion}" for position, conversion in sorted(taken.items()))


def main(root: Path) -> int:
    modules = []
    # Pruned while walking: build outputs, node_modules, .git, .gradle and the worktrees under .claude hold
    # hundreds of thousands of files no module's strings are in.
    for directory, children, _ in os.walk(root):
        children[:] = sorted(child for child in children if child not in ("build", "node_modules") and child[0] != ".")
        if directory.endswith(os.path.join("src", "main", "res")) and os.path.isfile(
            os.path.join(directory, "values", "strings.xml")
        ):
            modules.append(Path(directory))
            children[:] = []
    if not modules:
        print(f"No Android strings.xml under {root}", file=sys.stderr)
        return 2
    languages = {
        directory.name
        for res in modules
        for directory in res.iterdir()
        if LANGUAGE.match(directory.name)
        and directory.name not in PARTIAL | NOT_LANGUAGE
        and (directory / "strings.xml").is_file()
    }
    errors = [error for res in modules for error in check_module(res, languages, root)]
    for error in errors:
        print(f"::error::{error}" if "GITHUB_ACTIONS" in os.environ else error)
    if errors:
        print(f"{len(errors)} problem(s) in the Android strings of {len(modules)} modules.", file=sys.stderr)
        return 1
    print(f"The Android strings of {len(modules)} modules hold every one of English's, in {len(languages)} languages.")
    return 0


if __name__ == "__main__":
    sys.exit(main(Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()))
