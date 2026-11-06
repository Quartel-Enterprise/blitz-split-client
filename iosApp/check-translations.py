#!/usr/bin/env python3
"""Fails the build on a string the Apple app has not translated into every language.

Localizable.xcstrings holds the English literal of each SwiftUI text as its key, and Xcode only warns about a key
that lacks a language in the editor, so an untranslated key ships as English in pt-BR, Spanish or French. This checks,
for every language in REQUIRED, that each key has a translated, non-empty value (each plural and device variation
too) and that the value keeps the key's placeholders (%lld, %@, %1$@, the private-use stand-ins of the activity
sentences): a dropped or renamed one prints "%lld" on screen or crashes the formatting.

es-ES is not required: it holds only the words Spain says differently, and the build fills the rest from es
(merge-regional-strings.py). A key with "shouldTranslate": false or a stale one is skipped.

Xcode runs it after Compile Sources, as a postCompileScript of project.yml:
  check-translations.py <Resources directory>
"""

import json
import re
import sys
from collections import Counter
from pathlib import Path

# Every language the app is translated into, but English (the source) and es-ES (partial)
REQUIRED = ("pt-BR", "es", "fr")
CATALOGS = ("Localizable.xcstrings", "InfoPlist.xcstrings")

# %lld, %@, %1$@, %.2f, %% (a literal percent) and the private-use characters the activity sentences use as stand-ins
# for the actor and the object
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-+0#]*\d*(?:\.\d+)?(?:hh|h|ll|l|q|L|z|t|j)?[diouxXeEfFgGaAcCsSp@]|[\ue000\ue001]")
POSITION = re.compile(r"^%\d+\$")


def placeholders(text: str) -> Counter:
    """The placeholders of `text` as a multiset, "%1$@" and "%@" being the same one: a translation numbers them to
    reorder them, which is what positions are for."""
    return Counter(POSITION.sub("%", found) for found in PLACEHOLDER.findall(text.replace("%%", "")))


def leaves(localization: dict, path: str = ""):
    """Every (variation path, stringUnit) of a localization, a plain string being the one leaf with an empty path."""
    if "stringUnit" in localization:
        yield path, localization["stringUnit"]
    for kind, variants in localization.get("variations", {}).items():
        for name, variant in variants.items():
            yield from leaves(variant, f"{path}/{kind}.{name}")
    # The substitutions of a plural sentence ("%#@count@") each hold their own variations
    for name, substitution in localization.get("substitutions", {}).items():
        yield from leaves(substitution, f"{path}/{name}")


def problems(key: str, entry: dict):
    if entry.get("shouldTranslate") is False or entry.get("extractionState") == "stale":
        return
    localizations = entry.get("localizations", {})
    # The key is the English text, unless it is an identifier ("push.bill_created") or a description, whose English
    # text is then the `en` localization
    english = localizations.get("en", {}).get("stringUnit", {}).get("value", key)
    wanted = placeholders(english)
    # A device variation ("/device.mac") is compared with the English one of the same device, else with the whole text
    english_variations = {path: placeholders(unit.get("value", "")) for path, unit in leaves(localizations.get("en", {}))}
    for language in REQUIRED:
        localization = localizations.get(language)
        if localization is None:
            yield f"has no {language} translation"
            continue
        found = False
        for path, unit in leaves(localization):
            found = True
            value = unit.get("value", "")
            if unit.get("state") != "translated" or not value.strip():
                yield f"{language}{path} is not translated"
            # A plural form may drop its number ("une seule dépense") and a plural sentence ("%1$#@people@") moves its
            # numbers into substitutions that Xcode checks itself: only the plain text and the device variations count
            elif "plural." not in path and "#@" not in value and placeholders(value) != english_variations.get(path, wanted):
                yield f'{language}{path} "{value}" does not keep the placeholders of the key'
        if not found:
            yield f"{language} has no value"


def main(resources: str) -> int:
    failures = 0
    for name in CATALOGS:
        catalog = Path(resources) / name
        if not catalog.exists():
            print(f"{catalog}: error: the catalog is missing")
            failures += 1
            continue
        strings = json.loads(catalog.read_text(encoding="utf-8")).get("strings", {})
        for key, entry in strings.items():
            for problem in problems(key, entry):
                print(f'{catalog}: error: "{key}" {problem}')
                failures += 1
    return 1 if failures else 0


if __name__ == "__main__":
    if len(sys.argv) != 2 or not sys.argv[1]:
        print("error: usage: check-translations.py <Resources directory>")
        sys.exit(1)
    sys.exit(main(sys.argv[1]))
