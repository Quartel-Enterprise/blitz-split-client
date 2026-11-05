#!/usr/bin/env python3
"""Report the newer releases of every version in gradle/libs.versions.toml.

For each entry of [versions] that a library or plugin points at, it reads the maven-metadata.xml of
one of its modules from Google's Maven, Maven Central and the Gradle Plugin Portal, and prints the
newest release allowed by the stability rule of the bump-versions skill:

- a stable version only moves to a newer stable one;
- a pre-release (alpha, beta, rc, dev, eap, milestone) moves to the newest release of any kind,
  so it can reach the stable one when it ships.

It also checks the Gradle wrapper and the foojay toolchain resolver of settings.gradle.kts.

Usage: python3 .claude/skills/bump-versions/check_gradle_versions.py [repo root]
"""

import http.client
import json
import re
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Dict, List, Optional, Tuple

REPOS = [
    "https://dl.google.com/android/maven2",
    "https://repo1.maven.org/maven2",
    "https://plugins.gradle.org/m2",
]

# A pre-release qualifier, as a whole token of the version: 1.5.0-rc01, 3.9.0-beta-1, 2.5.0-Beta1,
# 1.0.0-M1, 2.0.0-dev-123. A word that only contains one of them (-source, -android) is not one.
PRE_RELEASE = re.compile(
    r"(?i)(?:^|[.-])(dev|snapshot|eap|preview|pre|alpha|beta|milestone|m(?=[.-]?\d)|rc)[.-]?(\d*)(?=$|[.-])"
)
PRE_RELEASE_RANK = {
    "dev": 0, "snapshot": 0, "eap": 1, "preview": 1, "pre": 1,
    "alpha": 2, "beta": 3, "m": 4, "milestone": 4, "rc": 5,
}
STABLE_RANK = 9


def is_stable(version: str) -> bool:
    return not PRE_RELEASE.search(version)


def flavor(version: str) -> str:
    """The variant word of a stable version (33.3.1-android, 33.3.1-jre), so a bump keeps it."""
    match = re.search(r"-([A-Za-z]+)$", version)
    return match.group(1).lower() if match and is_stable(version) else ""


def sort_key(version: str):
    """Orders 1.5.0-alpha01 < 1.5.0-beta02 < 1.5.0-rc01 < 1.5.0 < 1.5.1."""
    match = re.match(r"^([0-9.]+)(.*)$", version)
    if not match:
        return ((0,), 0, ())
    numbers = tuple(int(part) for part in match.group(1).strip(".").split(".") if part)
    suffix = match.group(2)
    pre_release = PRE_RELEASE.search(suffix)
    if pre_release:
        label = pre_release.group(1).lower()
        return (numbers, PRE_RELEASE_RANK[label], (int(pre_release.group(2) or 0),))
    return (numbers, STABLE_RANK, tuple(int(n) for n in re.findall(r"\d+", suffix)))


def fetch(url: str) -> Optional[str]:
    try:
        with urllib.request.urlopen(url, timeout=20) as response:
            return response.read().decode()
    # Why: OSError covers URLError, a read timeout (socket.timeout on 3.9) and a reset connection,
    # so one failed request reports "?" for its entry instead of stopping the whole run.
    except (OSError, http.client.HTTPException):
        return None


def versions_of(group: str, artifact: str) -> List[str]:
    path = f"{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    for repo in REPOS:
        metadata = fetch(f"{repo}/{path}")
        if metadata:
            return re.findall(r"<version>([^<]+)</version>", metadata)
    return []


def newest(current: str, available: List[str]) -> str:
    candidates = [
        v for v in available
        if (is_stable(v) or not is_stable(current)) and flavor(v) == flavor(current)
    ]
    candidates.append(current)
    return max(candidates, key=sort_key)


def parse_catalog(text: str) -> Tuple[Dict[str, str], Dict[str, Tuple[str, str]]]:
    """Reads the [versions] and, per version key, one module or plugin that points at it.

    A small line parser instead of tomllib, so it runs on the Python 3.9 that ships with macOS.
    """
    versions: Dict[str, str] = {}
    modules: Dict[str, Tuple[str, str]] = {}
    section = ""
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("["):
            section = line.strip("[]")
            continue
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = (part.strip() for part in line.split("=", 1))
        if section == "versions":
            quoted = re.match(r'"([^"]*)"', value)
            if quoted:
                versions[key] = quoted.group(1)
            continue
        ref = re.search(r'version\.ref\s*=\s*"([^"]+)"', value)
        if not ref or ref.group(1) in modules:
            continue
        module = re.search(r'module\s*=\s*"([^":]+):([^"]+)"', value)
        plugin = re.search(r'id\s*=\s*"([^"]+)"', value)
        if section == "libraries" and module:
            modules[ref.group(1)] = (module.group(1), module.group(2))
        elif section == "plugins" and plugin:
            modules[ref.group(1)] = (plugin.group(1), f"{plugin.group(1)}.gradle.plugin")
    return versions, modules


def check(name: str, current: str, group: str, artifact: str) -> Tuple[str, str, str, str]:
    available = versions_of(group, artifact)
    if not available:
        return name, current, "?", f"{group}:{artifact} not found"
    latest = newest(current, available)
    return name, current, latest, f"{group}:{artifact}"


def main() -> None:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    versions, modules = parse_catalog((root / "gradle/libs.versions.toml").read_text())

    jobs = []
    for name, current in versions.items():
        if name in modules:
            jobs.append((name, current, *modules[name]))
        else:
            print(f"skipped  {name} = {current} (no library or plugin points at it)")

    settings = (root / "settings.gradle.kts").read_text()
    foojay = re.search(r'foojay-resolver-convention"\)\s*version\s*"([^"]+)"', settings)
    if foojay:
        plugin_id = "org.gradle.toolchains.foojay-resolver-convention"
        jobs.append(("settings: foojay", foojay.group(1), plugin_id, f"{plugin_id}.gradle.plugin"))

    with ThreadPoolExecutor(max_workers=12) as pool:
        results = list(pool.map(lambda job: check(*job), jobs))

    wrapper = (root / "gradle/wrapper/gradle-wrapper.properties").read_text()
    gradle_current = re.search(r"gradle-(.+?)-(?:bin|all)\.zip", wrapper)
    if gradle_current:
        # services.gradle.org/versions/current is the newest stable release.
        gradle_latest = json.loads(fetch("https://services.gradle.org/versions/current") or "{}")
        results.append(
            ("gradle wrapper", gradle_current.group(1), gradle_latest.get("version", "?"), "services.gradle.org")
        )

    print()
    for name, current, latest, source in sorted(results):
        marker = "BUMP " if latest not in ("?", current) else "     "
        print(f"{marker}{name:42} {current:20} -> {latest:20} {source}")


if __name__ == "__main__":
    main()
