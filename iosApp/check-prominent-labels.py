#!/usr/bin/env python3
"""Fails the build on a `.borderedProminent` or `.glassProminent` button style that isn't followed by
`.prominentLabel()`.

The system draws a prominent button's label white, which reads on the light accent (#266489, 6.4:1) but not on the
dark one (#95CDF7, 1.7:1). `.prominentLabel()` (Palette.swift) gives it the color made for its tint, so every
prominent button needs it, on the same line or on the next one that isn't an `#if`, `#else` or `#endif`.

Xcode runs it after Compile Sources:
  check-prominent-labels.py <Sources directory>
"""

import re
import sys
from pathlib import Path

PROMINENT = re.compile(r"\.buttonStyle\(\.(?:borderedProminent|glassProminent)\)")
LABEL = ".prominentLabel("
CONDITIONAL = re.compile(r"^\s*#(?:if|elseif|else|endif)\b")


def missing_labels(path: Path):
    lines = path.read_text(encoding="utf-8").splitlines()
    for index, line in enumerate(lines):
        if not PROMINENT.search(line) or LABEL in line:
            continue
        following = next((other for other in lines[index + 1:] if not CONDITIONAL.match(other)), "")
        if LABEL not in following:
            yield index + 1


def main() -> int:
    sources = Path(sys.argv[1])
    failures = [
        (path, line)
        for path in sorted(sources.rglob("*.swift"))
        for line in missing_labels(path)
    ]
    for path, line in failures:
        print(f"{path}:{line}: error: a prominent button needs .prominentLabel() right after its style, or its label "
              "is white on the dark accent (1.7:1)")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
