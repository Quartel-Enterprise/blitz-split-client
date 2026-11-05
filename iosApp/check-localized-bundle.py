#!/usr/bin/env python3
"""Fails the build on a `String(localized:)` call without `bundle: .appLanguage`.

Profile › Language switches the app's language while it runs, but Foundation reads a `String(localized:)` in the
language the app launched in unless the call names the bundle to read it from. `Bundle.appLanguage` (AppLocale.swift)
is the `.lproj` of the language picked, so every call passes it, or its text stays in the old language until the app
is opened again.

Xcode runs it after Compile Sources:
  check-localized-bundle.py <Sources directory>
"""

import re
import sys
from pathlib import Path

# Also a call wrapped one argument per line: `String(\n    localized: …`
CALL = re.compile(r"\bString\(\s*localized:")
BUNDLE = "bundle: .appLanguage"


def string_end(text: str, index: int) -> int:
    """The index right after the string literal opening at `index`, its interpolations' code skipped."""
    if text.startswith('"""', index):
        return text.index('"""', index + 3) + 3
    index += 1
    while text[index] != '"':
        if text[index] == "\\":
            index = parentheses_end(text, index + 1) if text[index + 1] == "(" else index + 2
        else:
            index += 1
    return index + 1


def parentheses_end(text: str, index: int) -> int:
    """The index right after the parenthesis matching the one at `index`."""
    depth = 0
    while True:
        char = text[index]
        if char == '"':
            index = string_end(text, index)
            continue
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return index + 1
        index += 1


def missing_bundles(path: Path):
    text = path.read_text(encoding="utf-8")
    for match in CALL.finditer(text):
        # A call a comment names, as this check's own rule does in AppLocale.swift
        if "//" in text[text.rfind("\n", 0, match.start()) + 1:match.start()]:
            continue
        call = text[match.start():parentheses_end(text, text.index("(", match.start()))]
        # A call holding another one in an interpolation names the bundle once for each
        if call.count(BUNDLE) < len(CALL.findall(call)):
            yield text.count("\n", 0, match.start()) + 1


def main() -> int:
    sources = Path(sys.argv[1])
    failures = [(path, line) for path in sorted(sources.rglob("*.swift")) for line in missing_bundles(path)]
    for path, line in failures:
        print(f"{path}:{line}: error: String(localized:) needs bundle: .appLanguage to follow Profile › Language")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
