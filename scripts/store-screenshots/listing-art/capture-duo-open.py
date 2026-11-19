"""Captures the open iPhone Duo's screens the listing's art shows (docs/store-listing-screenshots.md), on a Duo already
opened (Device Hub's Open, or the iOS Simulator tool's posture): each language's group, Balances and Statistics, from
its inner display, into build/outputs/apple-screenshot-captures/iphoneduoopen/<locale>/. capture-ios.sh can't, since it
boots the Duo closed and only opens it through Device Hub.

    python3 scripts/store-screenshots/listing-art/capture-duo-open.py <udid> [locale,locale]
"""
import base64
import os
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TABLES = ROOT / "scripts/store-screenshots"
OUTPUT = ROOT / "build/outputs/apple-screenshot-captures/iphoneduoopen"
APP = ROOT / "build/apple-screenshots/DerivedData/Build/Products/Debug-iphonesimulator/BoraRachar.app"
SCREENS = {"group": "01_group", "balances": "04_balances", "statistics": "06_statistics"}
INNER_DISPLAY = "3"
SETTLE = 6


def rows(name):
    lines = (TABLES / f"{name}.tsv").read_text().splitlines()
    return [line.split("\t") for line in lines if line.strip() and not line.startswith("#")]


def main():
    udid = sys.argv[1]
    only = set(sys.argv[2].split(",")) if len(sys.argv) > 2 else None
    environment = dict(os.environ)
    for name, path in rows("assets"):
        encoded = base64.b64encode((TABLES / path).read_bytes()).decode()
        environment[f"SIMCTL_CHILD_STORE_SCREENSHOTS_ASSET_{name}"] = encoded
    subprocess.run(["xcrun", "simctl", "install", udid, str(APP)], check=True)
    for tag, language, apple_language, apple_locale in rows("locales"):
        if only and tag not in only:
            continue
        folder = OUTPUT / tag
        folder.mkdir(parents=True, exist_ok=True)
        for code, file_name in SCREENS.items():
            subprocess.run(
                [
                    "xcrun", "simctl", "launch", "--terminate-running-process", udid, "com.quare.blitzsplit",
                    "-StoreScreenshotsLanguage", language, "-StoreScreenshotsTheme", "dark",
                    "-StoreScreenshotsScreen", code, "-AppleLanguages", f"({apple_language})",
                    "-AppleLocale", apple_locale,
                ],
                env=environment,
                check=True,
                capture_output=True,
            )
            time.sleep(SETTLE)
            subprocess.run(
                ["xcrun", "simctl", "io", udid, "screenshot", f"--display={INNER_DISPLAY}", "--type=png",
                 str(folder / f"{file_name}.png")],
                check=True,
                capture_output=True,
            )
        print(f"Captured iphoneduoopen: {tag}", flush=True)
    subprocess.run(["xcrun", "simctl", "terminate", udid, "com.quare.blitzsplit"], capture_output=True)


main()
