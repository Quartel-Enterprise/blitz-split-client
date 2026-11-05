#!/usr/bin/env python3
"""Fails the build on a `.borderedProminent` or `.glassProminent` button style that isn't followed by
`.prominentLabel()`.

The system draws a prominent button's label white, which reads on the light accent (#266489, 6.4:1) but not on the
dark one (#95CDF7, 1.7:1). `.prominentLabel()` (Palette.swift) gives it the color made for its tint, so every
prominent button needs it, on the same line or on the next one that isn't an `#if`, `#else` or `#endif`. A button
tinted `Palette.negative` in the same modifier chain needs `.prominentLabel(Palette.onNegative)`: the accent's label
(#00344E) doesn't read on the dark negative (#FFB599).

Xcode runs it after Compile Sources:
  check-prominent-labels.py <Sources directory>
"""

import re
import sys
from pathlib import Path

PROMINENT = re.compile(r"\.buttonStyle\(\.(?:borderedProminent|glassProminent)\)")
LABEL = ".prominentLabel("
CONDITIONAL = re.compile(r"^\s*#(?:if|elseif|else|endif)\b")
CHAINED = re.compile(r"^\s*(?:\.|#(?:if|elseif|else|endif)\b)")
NEGATIVE_TINT = ".tint(Palette.negative)"
NEGATIVE_LABEL = ".prominentLabel(Palette.onNegative)"


def modifier_chain(lines, index):
    """The lines of the modifier chain around `index`: the ones that start with `.` or a conditional."""
    start = index
    while start > 0 and CHAINED.match(lines[start]):
        start -= 1
    end = index
    while end + 1 < len(lines) and CHAINED.match(lines[end + 1]):
        end += 1
    return "\n".join(lines[start:end + 1])


def missing_labels(path: Path):
    lines = path.read_text(encoding="utf-8").splitlines()
    for index, line in enumerate(lines):
        if not PROMINENT.search(line):
            continue
        if LABEL in line:
            following = LABEL
        else:
            following = next((other for other in lines[index + 1:] if not CONDITIONAL.match(other)), "")
        if LABEL not in following:
            yield index + 1, "needs .prominentLabel() right after its style, or its label is white on the dark accent (1.7:1)"
            continue
        chain = modifier_chain(lines, index)
        if NEGATIVE_TINT in chain and NEGATIVE_LABEL not in chain:
            yield index + 1, "is tinted Palette.negative, so it needs .prominentLabel(Palette.onNegative)"


def main() -> int:
    sources = Path(sys.argv[1])
    failures = [
        (path, line, reason)
        for path in sorted(sources.rglob("*.swift"))
        for line, reason in missing_labels(path)
    ]
    for path, line, reason in failures:
        print(f"{path}:{line}: error: a prominent button {reason}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
